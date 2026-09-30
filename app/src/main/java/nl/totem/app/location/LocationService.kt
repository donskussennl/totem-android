package nl.totem.app.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import nl.totem.app.data.SharedStore
import nl.totem.app.model.Place
import nl.totem.app.schedule.SessionEngine

/**
 * Houdt bij of je bij de locatie van een schema bent.
 *
 * Per schema op locatie meldt dit een cirkel (geofence) aan bij Google Play
 * Services. Kom je binnen of ga je weg, dan wekt Android [GeofenceReceiver],
 * ook als de app dicht is. Daarna beslist [SessionEngine.syncWithSchedule],
 * net als bij schema's op tijd, of een modus moet starten of stoppen.
 *
 * Werkt alleen met locatietoegang "Altijd toestaan"; met "Alleen tijdens
 * gebruik" krijgt de app geen seintjes als hij dicht is.
 */
object LocationService {

    private const val TAG = "TotemLocatie"
    private const val REQUEST_CODE = 5000

    fun hasForegroundAccess(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /** Mag Android ons wekken als de app dicht is? */
    fun hasBackgroundAccess(context: Context): Boolean {
        if (!hasForegroundAccess(context)) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return true
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Meldt de locaties van alle modi opnieuw aan en ruimt de rest op.
     *
     * Veilig om vaak aan te roepen: bij het opnieuw aanmelden meldt Android
     * direct of je al binnen bent, en een bestaande aankomsttijd blijft staan.
     */
    @SuppressLint("MissingPermission")
    fun refresh(context: Context) {
        SharedStore.init(context)
        val gewenst = SharedStore.modes.filter { it.schedule.isLocationBased && it.isConfigured }

        // Aankomsten van modi die niet meer op locatie lopen weggooien.
        val ids = gewenst.map { it.id }.toSet()
        val aankomsten = SharedStore.arrivals
        if (aankomsten.keys.any { it !in ids }) {
            SharedStore.arrivals = aankomsten.filterKeys { it in ids }
        }

        val client = LocationServices.getGeofencingClient(context)
        client.removeGeofences(pendingIntent(context))
        if (gewenst.isEmpty() || !hasForegroundAccess(context)) return

        val cirkels = gewenst.map { mode ->
            val plek = mode.schedule.place!!
            Geofence.Builder()
                .setRequestId(mode.id)
                .setCircularRegion(
                    plek.latitude,
                    plek.longitude,
                    plek.radius.coerceIn(Place.MIN_RADIUS, Place.MAX_RADIUS).toFloat()
                )
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .build()
        }
        val verzoek = GeofencingRequest.Builder()
            // Sta je er al, dan meteen een "binnenkomst".
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(cirkels)
            .build()

        runCatching {
            client.addGeofences(verzoek, pendingIntent(context))
                .addOnFailureListener { Log.w(TAG, "Geofences aanmelden mislukt", it) }
        }.onFailure { Log.w(TAG, "Geen toestemming voor locatie", it) }
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, GeofenceReceiver::class.java)
        // Moet muteerbaar zijn: Play Services zet de gebeurtenis erin.
        val vlaggen = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, vlaggen)
    }

    /** Een aankomst of vertrek verwerken. */
    internal fun update(context: Context, modeID: String, inside: Boolean) {
        SharedStore.init(context)
        val aankomsten = SharedStore.arrivals
        if (inside) {
            // Al binnen? Dan het oorspronkelijke aankomstmoment houden, anders
            // telt een eerder weggetikte blokkade niet meer.
            if (modeID in aankomsten) return
            SharedStore.arrivals = aankomsten + (modeID to System.currentTimeMillis())
        } else {
            if (modeID !in aankomsten) return
            SharedStore.arrivals = aankomsten - modeID
        }
        SessionEngine.syncWithSchedule(context)
    }
}

/** Android meldt hier dat je bij een gekozen plek aankomt of weggaat. */
class GeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val gebeurtenis = GeofencingEvent.fromIntent(intent) ?: return
        if (gebeurtenis.hasError()) return
        val binnen = when (gebeurtenis.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> true
            Geofence.GEOFENCE_TRANSITION_EXIT -> false
            else -> return
        }
        gebeurtenis.triggeringGeofences?.forEach {
            LocationService.update(context, it.requestId, binnen)
        }
    }
}
