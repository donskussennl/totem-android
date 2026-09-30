package nl.totem.app.ui

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import nl.totem.app.R
import nl.totem.app.model.FocusMode
import nl.totem.app.model.Schedule
import nl.totem.app.model.ScheduleEnd
import nl.totem.app.model.ScheduleTrigger
import java.text.DateFormatSymbols
import java.util.Calendar

/**
 * Teksten die uit meerdere stukken bestaan, zoals "ma–vr  09:00 – 17:00".
 *
 * Dagnamen en tijden komen van Android zelf, dus altijd in de taal en de
 * tijdnotatie van de telefoon ("Mon–Fri  9:00 AM – 5:00 PM" in het Engels).
 */
object Texts {

    /** "13 geblokkeerd", of "Nog geen apps gekozen". */
    fun modeSummary(context: Context, mode: FocusMode): String =
        if (mode.isConfigured) context.getString(R.string.mode_blocked_count, mode.blockedCount)
        else context.getString(R.string.mode_no_apps)

    /** De samenvatting van een schema, of null als het uit staat. */
    fun scheduleSummary(context: Context, schedule: Schedule): String? {
        if (!schedule.isOn) return null
        val dagen = days(context, schedule.weekdays)
        if (schedule.trigger == ScheduleTrigger.LOCATION) {
            val plek = schedule.place?.name ?: context.getString(R.string.sched_no_place)
            val eind = context.getString(
                if (schedule.end is ScheduleEnd.Tap) R.string.sched_until_tap else R.string.sched_until_leave
            )
            return context.getString(R.string.sched_location_summary, dagen, plek, eind)
        }
        val start = clock(context, schedule.startHour, schedule.startMinute)
        val eind = when (val end = schedule.end) {
            is ScheduleEnd.Time -> clock(context, end.hour, end.minute)
            is ScheduleEnd.Tap, is ScheduleEnd.Leave -> context.getString(R.string.sched_until_tap)
        }
        return context.getString(R.string.sched_time_summary, dagen, start, eind)
    }

    /** Een tijd in de notatie van de telefoon: "17:00" of "5:00 PM". */
    fun clock(context: Context, hour: Int, minute: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        return DateFormat.getTimeFormat(context).format(cal.time)
    }

    /**
     * De dagen zo kort mogelijk: "ma–vr" in plaats van "ma di wo do vr", en
     * "elke dag" als ze allemaal aanstaan. Maandag eerst.
     */
    fun days(context: Context, weekdays: Set<Int>): String {
        val namen = DateFormatSymbols.getInstance(currentLocale(context)).shortWeekdays
            .map { it.trimEnd('.') }
        val volgorde = listOf(2, 3, 4, 5, 6, 7, 1)
        val gekozen = volgorde.filter { it in weekdays }
        if (gekozen.isEmpty()) return context.getString(R.string.sched_no_day)
        if (gekozen.size == 7) return context.getString(R.string.sched_every_day)
        if (gekozen == listOf(7, 1)) return context.getString(R.string.sched_weekend)

        // Aaneengesloten reeksen samentrekken tot "ma–vr".
        val delen = mutableListOf<String>()
        var i = 0
        while (i < gekozen.size) {
            var j = i
            while (j + 1 < gekozen.size &&
                volgorde.indexOf(gekozen[j + 1]) == volgorde.indexOf(gekozen[j]) + 1
            ) j++
            if (j - i >= 2) delen += "${namen[gekozen[i]]}–${namen[gekozen[j]]}"
            else for (k in i..j) delen += namen[gekozen[k]]
            i = j + 1
        }
        return delen.joinToString(" ")
    }

    /** De letters voor de weekdagkiezer, 1 = zondag … 7 = zaterdag. */
    fun dayLetters(context: Context): Map<Int, String> {
        val kort = DateFormatSymbols.getInstance(currentLocale(context)).shortWeekdays
        return (1..7).associateWith { kort[it].take(1).uppercase(currentLocale(context)) }
    }

    private fun currentLocale(context: Context) =
        context.resources.configuration.locales[0]
}

/** Korte weg in Compose. */
@Composable
fun scheduleSummary(schedule: Schedule): String? =
    Texts.scheduleSummary(LocalContext.current, schedule)
