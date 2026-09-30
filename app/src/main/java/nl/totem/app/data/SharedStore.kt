package nl.totem.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import nl.totem.app.model.ActiveSession
import nl.totem.app.model.FocusMode
import nl.totem.app.model.PairedTotem
import nl.totem.app.model.SessionLog

/**
 * De opslag van Totem, op één plek.
 *
 * Op iOS waren dit twee dingen: `UserDefaults.standard` voor de app en een App
 * Group voor de extensies. Android kent dat onderscheid niet — de
 * AccessibilityService, de widget en de app draaien in hetzelfde proces of
 * kunnen bij dezelfde SharedPreferences. Eén bestand is dus genoeg.
 *
 * Bewust SharedPreferences en geen DataStore: de AccessibilityService moet
 * synchroon en zonder coroutine kunnen lezen bij elke schermwissel.
 */
object SharedStore {

    private const val FILE = "totem.store"

    private const val KEY_TOTEM = "totem.paired"
    private const val KEY_MODES = "totem.modes"
    private const val KEY_SESSION = "totem.session"
    private const val KEY_HISTORY = "totem.history"
    private const val KEY_EMERGENCY_USED = "totem.emergency.used"
    private const val KEY_EMERGENCY_PERIOD = "totem.emergency.period"
    private const val KEY_STRICT = "totem.strict"
    private const val KEY_NOTIFY = "totem.notify"
    private const val KEY_DISMISSED = "totem.dismissed"
    private const val KEY_ONBOARDED = "totem.onboarded"
    private const val KEY_A11Y_CONSENT = "totem.a11y.consent"
    private const val KEY_A11Y_CONSENT_AT = "totem.a11y.consent.at"
    private const val KEY_ARRIVALS = "totem.location.arrivals"
    private const val KEY_SETUP_DONE = "totem.setupDone"
    private const val KEY_PROMO_INDEX = "totem.promo.index"
    private const val KEY_TIP_INDEX = "totem.promo.tip"

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Vluchtig en gesynchroniseerd: de app, de wekkers, de WorkManager en de
     * widget komen alle vier vanaf een eigen draad hier binnen.
     */
    @Volatile
    private var prefsRef: SharedPreferences? = null

    private val prefs: SharedPreferences
        get() = prefsRef ?: error("SharedStore.init is nog niet aangeroepen")

    /** Wordt aangeroepen door alles wat het systeem los kan starten. */
    @Synchronized
    fun init(context: Context) {
        if (prefsRef != null) return
        prefsRef = context.applicationContext
            .getSharedPreferences(FILE, Context.MODE_PRIVATE)
    }

    // MARK: - Gekoppelde Totem

    var totem: PairedTotem?
        get() = prefs.getString(KEY_TOTEM, null)?.let {
            runCatching { json.decodeFromString<PairedTotem>(it) }.getOrNull()
        }
        set(value) {
            prefs.edit().apply {
                if (value == null) remove(KEY_TOTEM)
                else putString(KEY_TOTEM, json.encodeToString(value))
            }.apply()
        }

    // MARK: - Modi

    var modes: List<FocusMode>
        get() = prefs.getString(KEY_MODES, null)?.let {
            runCatching { json.decodeFromString<List<FocusMode>>(it) }.getOrNull()
        } ?: emptyList()
        set(value) {
            prefs.edit().putString(KEY_MODES, json.encodeToString(value)).apply()
        }

    fun mode(id: String): FocusMode? = modes.firstOrNull { it.id == id }

    // MARK: - Lopende sessie

    var session: ActiveSession?
        get() = prefs.getString(KEY_SESSION, null)?.let {
            runCatching { json.decodeFromString<ActiveSession>(it) }.getOrNull()
        }
        set(value) {
            prefs.edit().apply {
                if (value == null) remove(KEY_SESSION)
                else putString(KEY_SESSION, json.encodeToString(value))
            }.apply()
        }

    // MARK: - Geschiedenis

    var history: List<SessionLog>
        get() = prefs.getString(KEY_HISTORY, null)?.let {
            runCatching { json.decodeFromString<List<SessionLog>>(it) }.getOrNull()
        } ?: emptyList()
        set(value) {
            // Niet oneindig laten groeien; 500 sessies is ruim een jaar.
            val trimmed = if (value.size > 500) value.takeLast(500) else value
            prefs.edit().putString(KEY_HISTORY, json.encodeToString(trimmed)).apply()
        }

    // MARK: - Losse instellingen

    var strictMode: Boolean
        get() = prefs.getBoolean(KEY_STRICT, false)
        set(value) = prefs.edit().putBoolean(KEY_STRICT, value).apply()

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFY, value).apply()

    /**
     * Wanneer de gebruiker de prominente kennisgeving heeft aangetikt.
     *
     * Dit is een aantekening, geen sleutel: of de kennisgeving getoond wordt
     * hangt af van de dienst zelf. Staat die uit, dan wordt er opnieuw
     * toestemming gevraagd -- uitzetten is intrekken. Het tijdstip staat er
     * apart bij, zodat bij een beoordeling na te gaan is wanneer de gebruiker
     * voor het laatst akkoord ging.
     */
    var accessibilityConsent: Boolean
        get() = prefs.getBoolean(KEY_A11Y_CONSENT, false)
        set(value) = prefs.edit()
            .putBoolean(KEY_A11Y_CONSENT, value)
            .putLong(KEY_A11Y_CONSENT_AT, if (value) System.currentTimeMillis() else 0L)
            .apply()

    /** Wanneer die toestemming is gegeven, of 0 als dat niet is gebeurd. */
    val accessibilityConsentAt: Long
        get() = prefs.getLong(KEY_A11Y_CONSENT_AT, 0L)

    /** Of de wizard voor de eerste modus is doorlopen (of overgeslagen). */
    var setupDone: Boolean
        get() = prefs.getBoolean(KEY_SETUP_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_SETUP_DONE, value).apply()

    /** Welk promotiescherm er na een ontgrendeling aan de beurt is. */
    var promoIndex: Int
        get() = prefs.getInt(KEY_PROMO_INDEX, 0)
        set(value) = prefs.edit().putInt(KEY_PROMO_INDEX, value).apply()

    /** Welk Totem-weetje er de volgende keer aan de beurt is. */
    var tipIndex: Int
        get() = prefs.getInt(KEY_TIP_INDEX, 0)
        set(value) = prefs.edit().putInt(KEY_TIP_INDEX, value).apply()

    var hasOnboarded: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDED, value).apply()

    var emergencyUsed: Int
        get() = prefs.getInt(KEY_EMERGENCY_USED, 0)
        set(value) = prefs.edit().putInt(KEY_EMERGENCY_USED, value).apply()

    var emergencyPeriod: String
        get() = prefs.getString(KEY_EMERGENCY_PERIOD, "") ?: ""
        set(value) = prefs.edit().putString(KEY_EMERGENCY_PERIOD, value).apply()

    // MARK: - Weggetikte vensters

    /**
     * Wanneer een geplande blokkade zelf is beëindigd, per modus. De planner
     * leest dit zodat hij een weggetikt venster niet opnieuw aanzet.
     */
    var dismissed: Map<String, Long>
        get() = prefs.getString(KEY_DISMISSED, null)?.let {
            runCatching { json.decodeFromString<Map<String, Long>>(it) }.getOrNull()
        } ?: emptyMap()
        set(value) {
            prefs.edit().putString(KEY_DISMISSED, json.encodeToString(value)).apply()
        }

    fun markDismissed(modeID: String, at: Long) {
        dismissed = dismissed + (modeID to at)
    }

    // MARK: - Locatie

    /** Modus-id → moment van aankomst, voor elke locatie waar je nu bent. */
    var arrivals: Map<String, Long>
        get() = prefs.getString(KEY_ARRIVALS, null)?.let {
            runCatching { json.decodeFromString<Map<String, Long>>(it) }.getOrNull()
        } ?: emptyMap()
        set(value) {
            prefs.edit().putString(KEY_ARRIVALS, json.encodeToString(value)).apply()
        }
}
