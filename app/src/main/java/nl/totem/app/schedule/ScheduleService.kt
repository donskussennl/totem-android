package nl.totem.app.schedule

import nl.totem.app.model.FocusMode
import nl.totem.app.model.Schedule
import nl.totem.app.model.ScheduleEnd
import java.util.Calendar

/**
 * Beslist of een modus op dit moment volgens zijn schema actief hoort te zijn.
 *
 * Dit is de rekenkern van de schema's, en hij is met opzet één op één vertaald
 * uit de iOS-versie: dezelfde weekdagnummering (1 = zondag), dezelfde
 * behandeling van vensters over middernacht. Zo blijven beide apps hetzelfde
 * antwoord geven op dezelfde vraag.
 */
object ScheduleService {

    /** Zit [nu] binnen het venster van dit schema? */
    fun isWithinWindow(schedule: Schedule, nu: Long = System.currentTimeMillis()): Boolean {
        if (!schedule.isOn) return false

        val cal = Calendar.getInstance().apply { timeInMillis = nu }
        val minutesNow = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val start = schedule.startHour * 60 + schedule.startMinute
        val today = cal.get(Calendar.DAY_OF_WEEK)

        val end = schedule.end
        if (end !is ScheduleEnd.Time) {
            // Eindigt pas als je tikt: alleen de starttijd telt, op de gekozen dag.
            return schedule.weekdays.contains(today) && minutesNow >= start
        }

        val endMinutes = end.hour * 60 + end.minute

        return if (endMinutes > start) {
            // Gewoon venster binnen één dag, bijvoorbeeld 09:00 – 17:00.
            schedule.weekdays.contains(today) && minutesNow >= start && minutesNow < endMinutes
        } else {
            // Loopt over middernacht heen, bijvoorbeeld 22:00 – 07:00.
            val yesterday = if (today == 1) 7 else today - 1
            when {
                minutesNow >= start -> schedule.weekdays.contains(today)
                minutesNow < endMinutes -> schedule.weekdays.contains(yesterday)
                else -> false
            }
        }
    }

    /**
     * Het moment waarop het venster dat nu loopt is begonnen.
     *
     * Nodig om te weten of je een blokkade al hebt weggetikt: alleen een tik
     * ná dit moment telt voor het venster van vandaag.
     */
    fun windowStart(schedule: Schedule, nu: Long = System.currentTimeMillis()): Long? {
        if (!isWithinWindow(schedule, nu)) return null

        val cal = Calendar.getInstance().apply { timeInMillis = nu }
        val minutesNow = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val start = schedule.startHour * 60 + schedule.startMinute

        // Zijn we al voorbij de starttijd, dan begon het venster vandaag.
        // Zo niet, dan loopt het over middernacht en begon het gisteren.
        if (minutesNow < start) cal.add(Calendar.DAY_OF_YEAR, -1)

        cal.set(Calendar.HOUR_OF_DAY, schedule.startHour)
        cal.set(Calendar.MINUTE, schedule.startMinute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /**
     * De eerste modus die volgens zijn schema nu actief hoort te zijn.
     *
     * [dismissed] houdt bij wanneer je een geplande blokkade zelf hebt
     * beëindigd. Zo'n modus komt binnen hetzelfde venster niet terug — anders
     * zou hij meteen na je tik weer aanslaan.
     */
    fun modeThatShouldRun(
        modes: List<FocusMode>,
        nu: Long = System.currentTimeMillis(),
        dismissed: Map<String, Long> = emptyMap()
    ): FocusMode? = modes.firstOrNull { mode ->
        if (!mode.schedule.isOn || !mode.isConfigured) return@firstOrNull false
        val start = windowStart(mode.schedule, nu) ?: return@firstOrNull false
        val tappedAway = dismissed[mode.id]
        !(tappedAway != null && tappedAway >= start)
    }

    /** Moet een lopende, door een schema gestarte sessie nu stoppen? */
    fun shouldStop(mode: FocusMode, nu: Long = System.currentTimeMillis()): Boolean {
        if (!mode.schedule.isOn) return true
        if (mode.schedule.end is ScheduleEnd.Tap) return false   // wacht op een tik
        return !isWithinWindow(mode.schedule, nu)
    }
}
