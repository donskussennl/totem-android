package nl.totem.app.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import nl.totem.app.data.SharedStore
import nl.totem.app.shield.ShieldService

/**
 * Wat er gebeurt als een wekker afgaat, of als de telefoon net opnieuw is
 * opgestart.
 *
 * Er is bewust geen aparte afhandeling voor "start" en "stop": de wekker maakt
 * ons alleen wakker, en daarna kijkt [SessionEngine.syncWithSchedule] zelf wat
 * er hoort te gebeuren. Dat scheelt een hoop randgevallen — een wekker die is
 * overgeslagen wordt zo vanzelf ingehaald.
 */
class ScheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        SharedStore.init(context)

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                // Na een herstart of tijdwijziging staan alle wekkers uit.
                ShieldService.restore(context)
                ActivityScheduler.refresh(context)
                SessionEngine.syncWithSchedule(context)
                nl.totem.app.notify.Reminders.reschedule(context)
            }

            else -> {
                SessionEngine.syncWithSchedule(context)
                // Wekkers zijn eenmalig; meteen de volgende week weer zetten.
                ActivityScheduler.refresh(context)
            }
        }
    }

    companion object {
        const val ACTION_TICK = "nl.totem.app.SCHEDULE_TICK"
    }
}
