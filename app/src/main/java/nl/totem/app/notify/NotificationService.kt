package nl.totem.app.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import nl.totem.app.R
import nl.totem.app.data.SharedStore

/**
 * Meldingen rond geplande blokkades.
 *
 * Op iOS deelde de extensie de toestemming van de app; op Android is dat
 * hetzelfde, alleen heet de toestemming sinds Android 13 POST_NOTIFICATIONS en
 * moet je hem net als de camera expliciet vragen.
 */
object NotificationService {

    /** De doorlopende melding tijdens een blokkade. */
    const val CHANNEL_SESSION = "totem.session"

    /** Losse berichten: een schema is gestart of afgelopen. */
    const val CHANNEL_SCHEDULE = "totem.schedule"

    const val SESSION_NOTIFICATION_ID = 1001
    private const val SCHEDULE_NOTIFICATION_ID = 1002

    /** Wordt één keer aangeroepen vanuit [nl.totem.app.TotemApplication]. */
    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SESSION,
                context.getString(R.string.channel_session),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.channel_session_description)
                setShowBadge(false)
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SCHEDULE,
                context.getString(R.string.channel_schedule),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.channel_schedule_description)
            }
        )
    }

    /** Of Totem bericht stuurt als een schema een blokkade start of stopt. */
    var isEnabled: Boolean
        get() = SharedStore.notificationsEnabled
        set(value) { SharedStore.notificationsEnabled = value }

    fun hasPermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else true

    // MARK: - Versturen

    fun notifyStarted(context: Context, modeName: String, until: String?) {
        if (!isEnabled) return
        val body = until?.let { context.getString(R.string.notify_started_until, it) }
            ?: context.getString(R.string.notify_started_tap)
        send(context, context.getString(R.string.notify_started_title, modeName), body)
    }

    fun notifyEnded(context: Context, modeName: String) {
        if (!isEnabled) return
        send(
            context,
            context.getString(R.string.notify_ended_title, modeName),
            context.getString(R.string.notify_ended_body)
        )
    }

    /** De pauze is voorbij; de apps zijn weer bevroren. */
    fun notifyRefrozen(context: Context, modeName: String) {
        if (!isEnabled) return
        send(
            context,
            context.getString(R.string.notify_refrozen_title, modeName),
            context.getString(R.string.notify_refrozen_body)
        )
    }

    /**
     * Een schema wilde starten maar mocht niet blokkeren.
     *
     * Deze melding staat los van de schakelaar hierboven: hij gaat niet over
     * een schema dat wél werkte, maar over een schema dat stilzwijgend niets
     * deed. Dat mag je nooit missen.
     */
    fun notifyCannotBlock(context: Context, modeName: String) {
        send(
            context,
            context.getString(R.string.notify_cannot_block_title, modeName),
            context.getString(R.string.notify_cannot_block_body)
        )
    }

    private fun send(context: Context, title: String, body: String) {
        if (!hasPermission(context)) return

        val notification = NotificationCompat.Builder(context, CHANNEL_SCHEDULE)
            .setSmallIcon(R.drawable.ic_totem_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        runCatching {
            NotificationManagerCompat.from(context)
                .notify(SCHEDULE_NOTIFICATION_ID, notification)
        }
    }
}
