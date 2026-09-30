package nl.totem.app.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import nl.totem.app.R
import nl.totem.app.data.SharedStore
import nl.totem.app.model.Points
import java.util.Calendar

/**
 * De weekscore op zondagavond en de "we missen je"-herinneringen.
 *
 * Anders dan op iOS hoeft de tekst niet vooraf vast te liggen: een wekker
 * maakt ons op het moment zelf wakker, en dan rekenen we de punten van die
 * week uit. De herinneringen gaan na 7, 14 en 21 dagen zonder Totem, en
 * daarna niet meer. Wie Totem weer gebruikt, begint opnieuw te tellen.
 */
object Reminders {

    private const val REQUEST_WEEKLY = 6001
    private const val REQUEST_COMEBACK = 6002
    const val ACTION_WEEKLY = "nl.totem.app.WEEKLY_SCORE"
    const val ACTION_COMEBACK = "nl.totem.app.COMEBACK"

    private const val WEEK = 7L * 24 * 60 * 60 * 1000
    private const val MAX_COMEBACKS = 3

    /** Totem is net gebruikt: de herinneringen opnieuw laten tellen. */
    fun onUse(context: Context) {
        SharedStore.init(context)
        SharedStore.lastUse = System.currentTimeMillis()
        SharedStore.comebackCount = 0
        scheduleComeback(context)
    }

    /** Bij het opstarten van de app en na een herstart van de telefoon. */
    fun reschedule(context: Context) {
        SharedStore.init(context)
        // Nog nooit gebruikt? Dan telt vanaf nu, zodat ook wie Totem
        // installeert maar nooit gebruikt de herinneringen krijgt.
        if (SharedStore.lastUse == 0L) SharedStore.lastUse = System.currentTimeMillis()
        scheduleWeekly(context)
        scheduleComeback(context)
    }

    private fun scheduleWeekly(context: Context) {
        set(context, nextSundayEvening(), REQUEST_WEEKLY, ACTION_WEEKLY)
    }

    private fun scheduleComeback(context: Context) {
        if (SharedStore.comebackCount >= MAX_COMEBACKS) {
            cancel(context, REQUEST_COMEBACK, ACTION_COMEBACK)
            return
        }
        val at = SharedStore.lastUse + (SharedStore.comebackCount + 1) * WEEK
        set(context, maxOf(at, System.currentTimeMillis() + 60_000L), REQUEST_COMEBACK, ACTION_COMEBACK)
    }

    /** Een wekker is afgegaan. */
    internal fun handle(context: Context, action: String?) {
        SharedStore.init(context)
        when (action) {
            ACTION_WEEKLY -> {
                val punten = weekPoints(System.currentTimeMillis())
                val naam = context.getString(nickname(System.currentTimeMillis()))
                val tekst = if (punten > 0) context.getString(R.string.weekly_body, naam, punten)
                else context.getString(R.string.weekly_body_zero, naam)
                NotificationService.notifyReminder(context, context.getString(R.string.weekly_title), tekst, activate = false)
                scheduleWeekly(context)
            }

            ACTION_COMEBACK -> {
                val nu = System.currentTimeMillis()
                val index = SharedStore.comebackCount
                // Toch gebruikt in de tussentijd, of al drie keer gestuurd? Dan niets.
                if (index < MAX_COMEBACKS && nu - SharedStore.lastUse >= (index + 1) * WEEK - 60_000L) {
                    val (titel, tekst) = COMEBACKS[index]
                    NotificationService.notifyReminder(
                        context, context.getString(titel), context.getString(tekst), activate = true
                    )
                    SharedStore.comebackCount = index + 1
                }
                scheduleComeback(context)
            }
        }
    }

    /** Punten in de week (maandag t/m zondag) waarin [at] valt. */
    fun weekPoints(at: Long): Int = Points.week(SharedStore.history, SharedStore.modes, at)

    /** Elke week een andere bijnaam. */
    fun nickname(at: Long): Int {
        val week = Points.calendar().apply { timeInMillis = at }.get(Calendar.WEEK_OF_YEAR)
        return NICKNAMES[week % NICKNAMES.size]
    }

    val NICKNAMES = listOf(
        R.string.nick_scrollstopper, R.string.nick_algorithm,
        R.string.nick_brainrot, R.string.nick_feed
    )

    private val COMEBACKS = listOf(
        R.string.comeback1_title to R.string.comeback1_body,
        R.string.comeback2_title to R.string.comeback2_body,
        R.string.comeback3_title to R.string.comeback3_body
    )

    private fun nextSundayEvening(): Long {
        val nu = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 19)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY || cal.timeInMillis <= nu) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    private fun intent(context: Context, request: Int, action: String): PendingIntent =
        PendingIntent.getBroadcast(
            context, request,
            Intent(context, ReminderReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    /** Niet op de minuut nodig: een gewone wekker die de slaapstand mag doorbreken. */
    private fun set(context: Context, at: Long, request: Int, action: String) {
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        runCatching {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent(context, request, action))
        }
    }

    private fun cancel(context: Context, request: Int, action: String) {
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarms.cancel(intent(context, request, action))
    }
}

/** Wordt gewekt voor de weekscore en de herinneringen. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Reminders.handle(context, intent.action)
    }
}
