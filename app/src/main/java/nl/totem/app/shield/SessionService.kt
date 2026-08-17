package nl.totem.app.shield

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import nl.totem.app.MainActivity
import nl.totem.app.R
import nl.totem.app.data.SharedStore
import nl.totem.app.notify.NotificationService

/**
 * Houdt de lopende blokkade zichtbaar en levend.
 *
 * Dit is het Android-antwoord op de Live Activity van iOS: een melding die
 * niet weg te vegen is, met een teller die vanzelf doorloopt. Tegelijk zorgt
 * een voorgronddienst ervoor dat het systeem ons proces niet zomaar opruimt
 * terwijl er een blokkade loopt.
 */
class SessionService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        SharedStore.init(this)

        // Een lege intent betekent dat het systeem ons opnieuw heeft gestart na
        // een opruiming. Dan halen we de sessie uit de opslag, zodat de teller
        // niet stiekem vanaf nul begint.
        val session = SharedStore.session
        if (intent == null && session == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val modeName = intent?.getStringExtra(EXTRA_MODE_NAME)
            ?: session?.let { SharedStore.mode(it.modeID)?.name }
            ?: "Totem"
        val startedAt = intent?.getLongExtra(EXTRA_STARTED_AT, 0L)?.takeIf { it > 0 }
            ?: session?.startedAt
            ?: System.currentTimeMillis()

        val notification = buildNotification(modeName, startedAt)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NotificationService.SESSION_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NotificationService.SESSION_NOTIFICATION_ID, notification)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        // Ook als we via stopService worden afgesloten moet de melding weg.
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun buildNotification(modeName: String, startedAt: Long): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE
        )

        // setWhen levert de teller zijn nulpunt; setUsesChronometer laat hem
        // vanzelf doorlopen, ook als de app dicht is.
        return NotificationCompat.Builder(this, NotificationService.CHANNEL_SESSION)
            .setSmallIcon(R.drawable.ic_totem_notification)
            .setContentTitle("$modeName loopt")
            .setContentText("Tik je Totem aan als je weer verder wilt.")
            .setWhen(startedAt)
            .setUsesChronometer(true)
            .setShowWhen(true)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(open)
            .build()
    }

    companion object {
        private const val EXTRA_MODE_NAME = "modeName"
        private const val EXTRA_STARTED_AT = "startedAt"

        fun start(context: Context, modeName: String, startedAt: Long) {
            val intent = Intent(context, SessionService::class.java).apply {
                putExtra(EXTRA_MODE_NAME, modeName)
                putExtra(EXTRA_STARTED_AT, startedAt)
            }
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }

        /**
         * Bewust `stopService` en geen intent met een actie: een dienst starten
         * vanuit de achtergrond mag niet meer, maar stoppen altijd wel. Anders
         * bleef de melding met een tikkende teller staan bij een sessie die al
         * voorbij is.
         */
        fun stop(context: Context) {
            runCatching {
                context.stopService(Intent(context, SessionService::class.java))
            }
        }
    }
}
