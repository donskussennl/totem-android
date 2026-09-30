package nl.totem.app.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import nl.totem.app.data.SharedStore
import nl.totem.app.model.FocusMode
import nl.totem.app.model.ScheduleEnd
import java.util.Calendar

/**
 * Zet de wekkers die de schema's laten aanslaan, ook als de app dicht is.
 *
 * Op iOS deed het DeviceActivity-framework dit, met een extensie die het
 * systeem op begin- en eindtijd wakker maakte. Android heeft daar geen
 * evenknie van, maar wel twee dingen die samen hetzelfde doen:
 *
 * 1. **AlarmManager** met een exacte wekker per begin- en eindtijd. Dat is de
 *    precieze route: hij gaat op de minuut af.
 * 2. **WorkManager** als vangnet, elk kwartier. Fabrikanten die diensten
 *    doodmaken slaan soms een wekker over; de periodieke controle haalt dat
 *    weer recht.
 *
 * Anders dan iOS kent Android geen grens van twintig aanmeldingen: we mogen
 * zoveel wekkers zetten als we willen.
 */
object ActivityScheduler {

    private const val REQUEST_BASE = 7000
    /** Eigen nummer voor de wekker aan het einde van een pauze. */
    private const val REQUEST_PAUSE = 6999

    /** Deelt de huidige stand en zet de wekkers opnieuw. */
    fun refresh(context: Context) {
        SharedStore.init(context)
        val modes = SharedStore.modes
        cancelAll(context, modes)
        schedule(context, modes)
        ScheduleWorker.enqueue(context)
    }

    // MARK: - Wekkers

    private fun schedule(context: Context, modes: List<FocusMode>) {
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        for (mode in modes) {
            if (!mode.schedule.isTimed || !mode.isConfigured) continue

            for (weekday in mode.schedule.weekdays.sorted()) {
                // Begintijd
                val startAt = nextOccurrence(weekday, mode.schedule.startHour, mode.schedule.startMinute)
                set(context, alarms, startAt, requestCode(mode.id, weekday, true))

                // Eindtijd, alleen als die vastligt.
                val end = mode.schedule.end
                if (end is ScheduleEnd.Time) {
                    val startMin = mode.schedule.startHour * 60 + mode.schedule.startMinute
                    val endMin = end.hour * 60 + end.minute
                    // Loopt het venster over middernacht, dan valt het einde op
                    // de volgende dag.
                    val endWeekday = if (endMin > startMin) weekday else weekday % 7 + 1
                    val endAt = nextOccurrence(endWeekday, end.hour, end.minute)
                    set(context, alarms, endAt, requestCode(mode.id, weekday, false))
                }
            }
        }
    }

    private fun set(context: Context, alarms: AlarmManager, at: Long, requestCode: Int) {
        val pending = pendingIntent(context, requestCode)
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarms.canScheduleExactAlarms()
        } else true

        runCatching {
            if (canExact) {
                // setAlarmClock is de enige soort die de diepe slaapstand van
                // Android echt doorbreekt; setExactAndAllowWhileIdle wordt daar
                // nog altijd afgeknepen tot ongeveer één keer per negen minuten.
                // Voor een blokkade die op de minuut hoort te beginnen is dat
                // het verschil tussen 22:00 en 22:09.
                alarms.setAlarmClock(AlarmManager.AlarmClockInfo(at, null), pending)
            } else {
                // Zonder toestemming voor exacte wekkers: dan maar bij benadering.
                // De gebruiker ziet in de instellingen dat dit speelt.
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
            }
        }.onFailure { Log.w("Totem", "Wekker zetten mislukt", it) }
    }

    // MARK: - Ontdooien

    /**
     * Laat ons wakker maken als de pauze voorbij is, zodat de apps weer
     * bevriezen, ook als de app dicht is. [SessionEngine.syncWithSchedule]
     * ziet dan dat de pauze om is.
     */
    fun schedulePauseEnd(context: Context, at: Long) {
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        set(context, alarms, at, REQUEST_PAUSE)
    }

    fun cancelPauseEnd(context: Context) {
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarms.cancel(pendingIntent(context, REQUEST_PAUSE))
    }

    private fun cancelAll(context: Context, modes: List<FocusMode>) {
        for (mode in modes) cancel(context, mode.id)
    }

    /**
     * Zegt alle wekkers van één modus op.
     *
     * Moet aangeroepen worden vóórdat een modus uit de opslag verdwijnt: daarna
     * weten we de id niet meer, en blijven de wekkers tot in lengte van dagen
     * het toestel wakker maken voor iets wat niet meer bestaat.
     */
    fun cancel(context: Context, modeID: String) {
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (weekday in 1..7) {
            alarms.cancel(pendingIntent(context, requestCode(modeID, weekday, true)))
            alarms.cancel(pendingIntent(context, requestCode(modeID, weekday, false)))
        }
    }

    private fun pendingIntent(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, ScheduleReceiver::class.java)
            .setAction(ScheduleReceiver.ACTION_TICK)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Een uniek nummer per modus, dag en soort. De hash van de id is stabiel
     * zolang de modus bestaat, dus kunnen we de wekker later terugvinden.
     */
    private fun requestCode(modeID: String, weekday: Int, isStart: Boolean): Int {
        val base = modeID.hashCode() and 0x0FFFFFF
        return REQUEST_BASE + base * 16 + weekday * 2 + if (isStart) 0 else 1
    }

    /** Het eerstvolgende moment dat deze weekdag en tijd voorkomt. */
    private fun nextOccurrence(weekday: Int, hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val today = cal.get(Calendar.DAY_OF_WEEK)
        var days = (weekday - today + 7) % 7
        if (days == 0 && cal.timeInMillis <= System.currentTimeMillis()) days = 7
        cal.add(Calendar.DAY_OF_YEAR, days)
        return cal.timeInMillis
    }

}
