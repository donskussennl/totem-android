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
    val weekdays: Set<Int> = setOf(2, 3, 4, 5, 6)
) {
    /** "09:00 – 17:00" of "22:00 – als je tikt". */
    val timeText: String
        get() {
            val start = "%02d:%02d".format(startHour, startMinute)
            val stop = when (end) {
                is ScheduleEnd.Time -> "%02d:%02d".format(end.hour, end.minute)
                is ScheduleEnd.Tap -> "als je tikt"
            }
            return "$start – $stop"
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

    /** Korte samenvatting onder de naam in de lijst. */
    val summary: String
        get() = buildString {
            append("$blockedCount geblokkeerd")
            if (schedule.isOn) append(" · ${schedule.timeText}")
        }

    companion object {
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
    val startedBySchedule: Boolean = false
)

/** Een afgeronde sessie, voor de statistieken. */
@Serializable
data class SessionLog(
    val id: String = UUID.randomUUID().toString(),
    val modeName: String,
    val startedAt: Long,
    /** Duur in seconden. */
    val duration: Long
)

/**
 * Fouten die de gebruiker te zien kan krijgen.
 *
 * [Cancelled] is bewust geen echte fout: die betekent alleen dat het
 * scanvenster is weggeklikt.
 */
sealed class TotemError(val text: String?) : Exception(text) {
    data object WrongTag :
        TotemError("Dit is niet jouw Totem. Houd je eigen Totem tegen de telefoon.")

    data object NotATotem :
        TotemError("Dit is geen echte Totem.")

    data object NfcUnavailable :
        TotemError("NFC is niet beschikbaar op dit toestel.")

    data object NfcDisabled :
        TotemError("NFC staat uit. Zet het aan in de instellingen van je telefoon.")

    data object NotAuthorized :
        TotemError("Geef Totem toegang tot toegankelijkheid om apps te kunnen blokkeren.")

    data object Cancelled : TotemError(null)
}
