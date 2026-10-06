package nl.totem.app.shield

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationManagerCompat

/**
 * Houdt meldingen van geblokkeerde apps tegen zolang Totem actief is.
 *
 * De blokkade zelf zit in [TotemAccessibilityService]: die houdt de app
 * dicht. Maar een app die je niet kunt openen, kan je nog steeds lokken met
 * een melding — en dat is precies wat je tijdens een focusmodus niet wilt.
 *
 * Android laat een app alleen aan meldingen van anderen komen via een
 * meldingenluisteraar, en die moet de gebruiker zelf aanzetten bij
 * 'Toegang tot meldingen'. Staat hij uit, dan werkt de rest van Totem gewoon;
 * alleen de meldingen komen dan nog binnen.
 *
 * We vegen een melding weg zodra hij binnenkomt, en ruimen bij de start van
 * een blokkade (of na ontdooien) alles op wat er al stond. Wat we wegvegen is
 * weg uit het meldingenpaneel; het bericht zelf staat gewoon nog in de app.
 */
class MeldingenFilter : NotificationListenerService() {

    override fun onListenerConnected() {
        actief = this
        // Wordt het proces voor ons opgestart, dan staat de blokkade nog niet
        // in het geheugen.
        ShieldService.restore(applicationContext)
        veegOp()
    }

    override fun onListenerDisconnected() {
        if (actief === this) actief = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (moetWeg(sbn)) weg(sbn)
    }

    /** Alles wat er al staat van een geblokkeerde app weghalen. */
    private fun veegOp() {
        if (!ShieldService.isBlocking) return
        val meldingen = runCatching { activeNotifications }.getOrNull() ?: return
        meldingen.filter(::moetWeg).forEach(::weg)
    }

    private fun moetWeg(sbn: StatusBarNotification): Boolean =
        // Doorlopende meldingen (muziekspeler, navigatie) kunnen we niet
        // wegvegen; die laten we staan in plaats van het steeds te proberen.
        !sbn.isOngoing && sbn.packageName in ShieldService.huidigeStand.packages

    private fun weg(sbn: StatusBarNotification) {
        runCatching { cancelNotification(sbn.key) }
            .onFailure { Log.w(TAG, "Kon melding van ${sbn.packageName} niet wegvegen", it) }
    }

    companion object {
        private const val TAG = "MeldingenFilter"

        @Volatile
        private var actief: MeldingenFilter? = null

        /** Blokkade net begonnen of weer bevroren: wat er al staat opruimen. */
        fun veegOpNu() {
            actief?.veegOp()
        }

        /** Heeft de gebruiker 'Toegang tot meldingen' voor Totem aangezet? */
        fun isEnabled(context: Context): Boolean =
            context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)

        /**
         * Opent de instelling, op Android 11 en nieuwer meteen bij Totem; op
         * oudere versies de lijst waarin de gebruiker Totem zelf aanzet.
         */
        fun openSettings(context: Context) {
            val detail = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
                    Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                    ComponentName(context, MeldingenFilter::class.java).flattenToString()
                )
            } else null
            val lijst = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            for (intent in listOfNotNull(detail, lijst)) {
                val gelukt = runCatching {
                    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }.isSuccess
                if (gelukt) return
            }
        }
    }
}
