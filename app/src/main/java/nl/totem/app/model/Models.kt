package nl.totem.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Een gekoppelde Totem. De NFC-tag-UID is de unieke sleutel van het apparaat.
 */
@Serializable
data class PairedTotem(
    val tagUID: String,
    val name: String,
    /** Epoch-milliseconden; op Android bewaren we tijden als Long. */
    val pairedAt: Long,
    /** Gekoppeld met het codewoord, zonder echte tag. Zie DEMO_CODE. */
    val isDemo: Boolean = false
) {
    val shortUID: String
        get() = if (tagUID.length > 8) tagUID.take(8) + "…" else tagUID

    companion object {
        /**
         * App Review en Play Review hebben geen Totem. Typen ze dit woord in
         * het naamveld bij het koppelen, dan koppelt de app zonder tag en
         * gaan starten en stoppen met een knop. Zelfde codewoord als op iOS.
         */
        const val DEMO_CODE = "APPLEREVIEW"
    }
}

/**
 * Wanneer een geplande blokkade ophoudt.
 *
 * Op iOS was dit een enum met een associated value; in Kotlin doen we dat met
 * een verzegelde klasse en een expliciete discriminator, zodat de JSON die de
 * app bewaart leesbaar en stabiel blijft.
 */
@Serializable
sealed class ScheduleEnd {
    /** Een vaste eindtijd, bijvoorbeeld 17:00. */
    @Serializable
    @SerialName("time")
    data class Time(val hour: Int, val minute: Int) : ScheduleEnd()

    /** Geen eindtijd: de blokkade blijft staan tot je je Totem aantikt. */
    @Serializable
    @SerialName("tap")
    data object Tap : ScheduleEnd()

    /** Zodra je de locatie verlaat. Alleen bij een schema op locatie. */
    @Serializable
    @SerialName("leave")
    data object Leave : ScheduleEnd()
}

/** Waardoor een schema begint. */
@Serializable
enum class ScheduleTrigger {
    /** Op een vaste tijd. De standaard. */
    @SerialName("time") TIME,

    /** Zodra je bij een gekozen plek aankomt, bijvoorbeeld de sportschool. */
    @SerialName("location") LOCATION
}

/** Een plek waar een schema op locatie begint. */
@Serializable
data class Place(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    /** In meters. Onder de ~100 m reageert Android onbetrouwbaar. */
    val radius: Double = 150.0
) {
    companion object {
        const val MIN_RADIUS = 100.0
        const val MAX_RADIUS = 1000.0
    }
}

/**
 * Het schema van één modus: begintijd, einde en de dagen waarop het geldt.
 *
 * De weekdagnummers volgen [java.util.Calendar]: 1 = zondag … 7 = zaterdag.
 * Dat is dezelfde nummering als op iOS, zodat bewaarde gegevens en de
 * WeekdayPicker één op één overkomen.
 */
@Serializable
data class Schedule(
    val isOn: Boolean = false,
    val startHour: Int = 9,
    val startMinute: Int = 0,
    val end: ScheduleEnd = ScheduleEnd.Time(17, 0),
    val weekdays: Set<Int> = setOf(2, 3, 4, 5, 6),
    /**
     * Hoe lang de apps ontdooien als je tijdens het schema je Totem aantikt.
     * Daarna bevriezen ze weer tot het schema voorbij is.
     */
    val pauseMinutes: Int = 5,
    /** Op tijd (standaard) of op locatie. */
    val trigger: ScheduleTrigger = ScheduleTrigger.TIME,
    /** De plek, als het schema op locatie begint. */
    val place: Place? = null
) {
    /** Of dit schema met wekkers op tijd loopt. */
    val isTimed: Boolean get() = isOn && trigger == ScheduleTrigger.TIME

    /** Of dit schema op locatie loopt én er een plek gekozen is. */
    val isLocationBased: Boolean
        get() = isOn && trigger == ScheduleTrigger.LOCATION && place != null

    /**
     * Of een tik tijdens dit schema de apps even ontdooit in plaats van het
     * schema te beëindigen. Bij "als je tikt" is de tik juist het einde.
     */
    val pausesOnTap: Boolean
        get() = isOn && end !is ScheduleEnd.Tap

    companion object {
        val PAUSE_OPTIONS = listOf(5, 10, 15)
    }

}

/**
 * Een modus: werk, slaap, relaxen of zelfbedacht.
 *
 * Op iOS bewaarde `FamilyActivitySelection` versleutelde verwijzingen naar
 * apps. Android heeft dat niet: hier bewaren we gewoon de pakketnamen van de
 * apps die geblokkeerd worden.
 */
@Serializable
data class FocusMode(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    /** Naam van het Material-icoon, zie [nl.totem.app.ui.TotemIcons]. */
    val symbol: String,
    val blockedPackages: Set<String> = emptySet(),
    val schedule: Schedule = Schedule()
) {
    val blockedCount: Int get() = blockedPackages.size

    val isConfigured: Boolean get() = blockedPackages.isNotEmpty()

    // (Meer dan tien modi maakt de lijst onoverzichtelijk; zie MAX_COUNT.)

    // Samenvattingen voor op het scherm staan in ui/Texts.kt, in de taal van de telefoon.

    companion object {
        /** Meer dan tien modi maakt de lijst onoverzichtelijk. */
        const val MAX_COUNT = 10

        fun defaults(): List<FocusMode> = listOf(
            FocusMode(name = "Werk", symbol = "laptop"),
            FocusMode(name = "Slaap", symbol = "moon"),
            FocusMode(name = "Relaxen", symbol = "leaf")
        )
    }
}

/** Een lopende sessie: welke modus is actief en sinds wanneer. */
@Serializable
data class ActiveSession(
    val modeID: String,
    val startedAt: Long,
    /** Of een schema deze sessie startte, of jij met je Totem. */
    val startedBySchedule: Boolean = false,
    /** Tot wanneer de apps even ontdooid zijn (epoch-ms). Null = bevroren. */
    val pausedUntil: Long? = null,
    /** Wanneer de lopende pauze begon. */
    val pauseStartedAt: Long? = null,
    /** Hoe lang er in totaal ontdooid is geweest, in ms. Telt niet mee voor punten. */
    val pausedTotal: Long = 0L
) {
    fun isPaused(nu: Long = System.currentTimeMillis()): Boolean =
        pausedUntil != null && pausedUntil > nu

    /** Sluit een lopende pauze af en telt hem op bij het totaal. */
    fun closePause(nu: Long = System.currentTimeMillis()): ActiveSession {
        val begin = pauseStartedAt ?: return copy(pausedUntil = null)
        val eind = minOf(nu, pausedUntil ?: nu)
        return copy(
            pausedUntil = null,
            pauseStartedAt = null,
            pausedTotal = pausedTotal + maxOf(0L, eind - begin)
        )
    }
}

/** Een afgeronde sessie, voor de statistieken. */
@Serializable
data class SessionLog(
    val id: String = UUID.randomUUID().toString(),
    val modeName: String,
    val startedAt: Long,
    /** Duur in seconden. */
    val duration: Long,
    /** Hoe lang er tussendoor ontdooid is geweest, in seconden. */
    val pausedSeconds: Long = 0L,
    /** Hoeveel apps er op slot zaten; bepaalt mee de punten. */
    val appCount: Int? = null,
    val modeID: String? = null
) {
    /** De tijd dat er echt iets geblokkeerd was, in seconden. */
    val blockedSeconds: Long get() = maxOf(0L, duration - pausedSeconds)
}

/**
 * Fouten die de gebruiker te zien kan krijgen.
 *
 * [Cancelled] is bewust geen echte fout: die betekent alleen dat het
 * scanvenster is weggeklikt.
 */
sealed class TotemError(@androidx.annotation.StringRes val textRes: Int?) : Exception() {
    data object WrongTag : TotemError(nl.totem.app.R.string.err_wrong_tag)

    data object NotATotem : TotemError(nl.totem.app.R.string.err_not_totem)

    data object NfcUnavailable : TotemError(nl.totem.app.R.string.err_nfc_unavailable)

    data object NfcDisabled : TotemError(nl.totem.app.R.string.err_nfc_off)

    data object NotAuthorized : TotemError(nl.totem.app.R.string.err_not_authorized)

    data object Cancelled : TotemError(null)
}

/**
 * Punten voor je weekscore: 1 punt per geblokkeerde app per 10 minuten.
 * Een uur met 6 apps op slot levert dus 36 punten op. Zelfde formule als op iOS.
 */
object Points {
    fun apps(mode: FocusMode): Int = maxOf(1, mode.blockedCount)

    fun points(blockedSeconds: Long, apps: Int): Int =
        Math.round(blockedSeconds / 60.0 * apps / 10.0).toInt()

    /**
     * Punten voor één afgeronde sessie. Oude sessies van vóór de punten kennen
     * hun aantal apps niet; dan rekenen we met de modus zoals hij nu is.
     */
    fun forLog(log: SessionLog, modes: List<FocusMode>): Int {
        val apps = log.appCount
            ?: modes.firstOrNull { it.id == log.modeID || it.name == log.modeName }?.let { apps(it) }
            ?: 1
        return points(log.blockedSeconds, apps)
    }

    /** Punten in de week (maandag t/m zondag) waarin [at] valt. */
    fun week(history: List<SessionLog>, modes: List<FocusMode>, at: Long): Int {
        val start = weekStart(at)
        val eind = start + 7L * 24 * 60 * 60 * 1000
        return history.filter { it.startedAt + it.duration * 1000 in start until eind }
            .sumOf { forLog(it, modes) }
    }

    /** Maandag als eerste dag van de week, zoals in Nederland. */
    fun calendar(): java.util.Calendar = java.util.Calendar.getInstance().apply {
        firstDayOfWeek = java.util.Calendar.MONDAY
        minimalDaysInFirstWeek = 4
    }

    /** Het begin (maandag 00:00) van de week waarin [at] valt. */
    fun weekStart(at: Long): Long = calendar().apply {
        timeInMillis = at
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
        while (get(java.util.Calendar.DAY_OF_WEEK) != java.util.Calendar.MONDAY) {
            add(java.util.Calendar.DAY_OF_YEAR, -1)
        }
    }.timeInMillis
}
