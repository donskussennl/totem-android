package nl.totem.app.store

import android.app.Application
import nl.totem.app.R
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import nl.totem.app.data.PinService
import nl.totem.app.data.SharedStore
import nl.totem.app.model.ActiveSession
import nl.totem.app.model.FocusMode
import nl.totem.app.model.PairedTotem
import nl.totem.app.model.SessionLog
import nl.totem.app.model.TotemError
import nl.totem.app.nfc.TotemAuth
import nl.totem.app.schedule.ActivityScheduler
import nl.totem.app.schedule.SessionEngine
import nl.totem.app.shield.ShieldService
import java.util.Calendar

/**
 * Centrale staat van de app: gekoppelde Totem, modi en de lopende sessie.
 *
 * Op iOS was dit een `ObservableObject` met `@Published`-velden. Hier is het
 * een ViewModel met StateFlows; Compose leest die met `collectAsStateWithLifecycle`.
 *
 * Het lezen zelf gebeurt niet hier maar in de MainActivity: de NfcAdapter heeft
 * een activiteit nodig om aan te haken. Die stuurt élke gelezen tag door naar
 * [onTagRead]; wat ermee moet gebeuren — koppelen, starten, stoppen — beslist
 * dit model. Zo blijft de logica los van het scherm.
 */
class AppStore(app: Application) : AndroidViewModel(app) {

    /** Waarom er gescand wordt. Bepaalt wat er met de UID gebeurt. */
    enum class ScanPurpose { PAIR, START, STOP, PAUSE }

    data class ScanRequest(
        val purpose: ScanPurpose,
        val prompt: String,
        /** Bij START: de modus die daarna moet starten. */
        val modeID: String? = null,
        /** Bij PAIR: de naam die de gebruiker heeft ingevuld. */
        val name: String? = null
    )

    private val context get() = getApplication<Application>()

    private val _totem = MutableStateFlow<PairedTotem?>(null)
    val totem: StateFlow<PairedTotem?> = _totem.asStateFlow()

    private val _modes = MutableStateFlow<List<FocusMode>>(emptyList())
    val modes: StateFlow<List<FocusMode>> = _modes.asStateFlow()

    private val _session = MutableStateFlow<ActiveSession?>(null)
    val session: StateFlow<ActiveSession?> = _session.asStateFlow()

    private val _history = MutableStateFlow<List<SessionLog>>(emptyList())
    val history: StateFlow<List<SessionLog>> = _history.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _scanRequest = MutableStateFlow<ScanRequest?>(null)
    val scanRequest: StateFlow<ScanRequest?> = _scanRequest.asStateFlow()

    private val _emergencyUsed = MutableStateFlow(0)
    val emergencyUsed: StateFlow<Int> = _emergencyUsed.asStateFlow()

    private val _strictMode = MutableStateFlow(false)
    val strictMode: StateFlow<Boolean> = _strictMode.asStateFlow()

    /** Je hebt getikt terwijl er niets liep: welke modus wordt het? */
    private val _awaitingModeChoice = MutableStateFlow(false)
    val awaitingModeChoice: StateFlow<Boolean> = _awaitingModeChoice.asStateFlow()

    val isScanning: Boolean get() = _scanRequest.value != null

    /**
     * True zolang de wizard nog niet is doorlopen. Wie al een ingestelde modus
     * heeft (van vóór de wizard), slaat hem over.
     */
    private val _needsSetup = MutableStateFlow(false)
    val needsSetup: StateFlow<Boolean> = _needsSetup.asStateFlow()

    init {
        SharedStore.init(context)
        PinService.init(context)
        load()
        refreshEmergencyPeriod()

        // Meeluisteren met alles wat buiten de app om gebeurt: een wekker, het
        // vangnet, de blokkadedienst. Die zetten de opslag goed en tikken hier
        // aan dat er iets veranderd is.
        viewModelScope.launch {
            SessionEngine.revision.collect { load() }
        }
    }

    // MARK: - Afgeleide staat

    val isPaired: Boolean get() = _totem.value != null

    fun activeMode(): FocusMode? {
        val session = _session.value ?: return null
        return _modes.value.firstOrNull { it.id == session.modeID }
    }

    fun mode(id: String): FocusMode? = _modes.value.firstOrNull { it.id == id }

    // MARK: - Koppelen

    fun pairTotem(name: String = "Mijn Totem") {
        // Zonder Totem koppelen, voor App Review en Play Review.
        if (name.trim().uppercase() == PairedTotem.DEMO_CODE) {
            val demo = PairedTotem(
                tagUID = "DEMO",
                name = "Demo Totem",
                pairedAt = System.currentTimeMillis(),
                isDemo = true,
            )
            _totem.value = demo
            SharedStore.totem = demo
            return
        }

        _scanRequest.value = ScanRequest(
            purpose = ScanPurpose.PAIR,
            prompt = "Houd je telefoon tegen je Totem om te koppelen",
            name = name
        )
    }

    fun unpairTotem() {
        if (_session.value != null) SessionEngine.stop(context, bySchedule = false)
        _totem.value = null
        SharedStore.totem = null
    }

    // MARK: - Eerste keer instellen

    /**
     * Rondt de wizard af. Lege standaardmodi van vroeger ruimen we op, zodat
     * je niet naast je nieuwe "Werk" nog een lege "Werk" ziet staan.
     */
    fun finishSetup(mode: FocusMode?) {
        val plaatshouders = setOf("Werk", "Slaap", "Relaxen")
        _modes.value.filter { !it.isConfigured && !it.schedule.isOn && it.name in plaatshouders }
            .forEach { ActivityScheduler.cancel(context, it.id) }
        _modes.value = _modes.value.filterNot {
            !it.isConfigured && !it.schedule.isOn && it.name in plaatshouders
        } + listOfNotNull(mode)
        SharedStore.setupDone = true
        _needsSetup.value = false
        persistModes()
    }

    // MARK: - Modi

    fun addMode(mode: FocusMode): Boolean {
        if (_modes.value.size >= FocusMode.MAX_COUNT) {
            _errorMessage.value = context.getString(R.string.error_max_modes, FocusMode.MAX_COUNT)
            return false
        }
        _modes.value = _modes.value + mode
        persistModes()
        return true
    }

    fun update(mode: FocusMode) {
        _modes.value = _modes.value.map { if (it.id == mode.id) mode else it }
        persistModes()
    }

    fun deleteMode(id: String) {
        if (_session.value?.modeID == id) SessionEngine.stop(context, bySchedule = false)
        // Eerst de wekkers weghalen: daarna kent de planner deze modus niet
        // meer en zou hij ze nooit meer kunnen opzeggen.
        ActivityScheduler.cancel(context, id)
        _modes.value = _modes.value.filterNot { it.id == id }
        persistModes()
    }

    private fun persistModes() {
        SharedStore.modes = _modes.value
        ActivityScheduler.refresh(context)
        // Loopt er een sessie van deze modus, dan meteen de blokkade bijwerken.
        _session.value?.let { s ->
            mode(s.modeID)?.let { ShieldService.startBlocking(context, it, s.startedAt) }
        }
        load()
    }

    // MARK: - Sessie starten en stoppen

    /** Start een modus. Vereist een tik van de gekoppelde Totem. */
    fun startSession(mode: FocusMode) {
        if (!isPaired) return
        if (!mode.isConfigured) {
            _errorMessage.value =
                "Kies eerst welke apps geblokkeerd moeten worden in deze modus."
            return
        }
        if (!ShieldService.isReady(context)) {
            _errorMessage.value = TotemError.NotAuthorized.text
            return
        }
        if (_totem.value?.isDemo == true) {
            SessionEngine.start(context, mode, bySchedule = false)
            return
        }
        _scanRequest.value = ScanRequest(
            purpose = ScanPurpose.START,
            prompt = "Tik je Totem aan om ‘${mode.name}’ te starten",
            modeID = mode.id
        )
    }

    /**
     * Stopt de lopende sessie. Kan alléén door de Totem opnieuw te tikken.
     *
     * Tijdens een schema stopt een tik niet, maar ontdooit hij de apps even;
     * zie [SessionEngine.tapWhileRunning].
     */
    fun endSession() {
        val session = _session.value ?: return
        if (SessionEngine.tapPauses()) {
            if (session.isPaused()) return
            val mode = mode(session.modeID) ?: return
            if (_totem.value?.isDemo == true) {
                SessionEngine.pause(context)
                return
            }
            _scanRequest.value = ScanRequest(
                purpose = ScanPurpose.PAUSE,
                prompt = context.getString(R.string.scan_unfreeze, mode.schedule.pauseMinutes)
            )
            return
        }
        if (_totem.value?.isDemo == true) {
            SessionEngine.stop(context, bySchedule = false)
            return
        }
        _scanRequest.value = ScanRequest(
            purpose = ScanPurpose.STOP,
            prompt = "Tik je Totem aan om te ontgrendelen"
        )
    }

    /**
     * Je hebt de Totem al aangetikt terwijl de app dicht was; Android opende
     * ons met de link van de tag. Dan hoeft er niet nog eens gescand te worden.
     */
    fun handleTapLink(uid: String) {
        if (_totem.value == null) {
            // Nog niets gekoppeld. Bewust níét koppelen op grond van deze link:
            // iedereen kan een tag beschrijven met dit adres. Koppelen gaat
            // alleen via een echte scan, want alleen daar lezen we de
            // handtekening uit het taggeheugen.
            _errorMessage.value =
                "Koppel eerst je Totem in de app; houd hem dan tegen de telefoon."
            return
        }
        // Elke Totem mag bedienen, net als op iOS -- er wordt hier bewust niet
        // vergeleken met de gekoppelde Totem.
        //
        // Maar STOPPEN gaat niet op grond van deze link. Een adres is tekst en
        // bewijst niet dat de chip tegen de telefoon gehouden is; wie het
        // bewaart als bladwijzer zou zijn eigen blokkade kunnen opheffen.
        // Daarom vragen we alsnog om een echte scan, en die loopt wél langs de
        // handtekeningcontrole. Starten mag wel meteen: dat valt niet te
        // misbruiken.
        if (_session.value != null) {
            endSession()
        } else {
            _awaitingModeChoice.value = true
        }
    }

    /** Na een tik: de gekozen modus starten, zonder nieuwe scan. */
    fun startAfterTap(mode: FocusMode) {
        _awaitingModeChoice.value = false
        if (!mode.isConfigured) return
        if (!ShieldService.isReady(context)) {
            _errorMessage.value = TotemError.NotAuthorized.text
            return
        }
        SessionEngine.start(context, mode, bySchedule = false)
    }

    fun dismissModeChoice() {
        _awaitingModeChoice.value = false
    }

    // MARK: - Antwoord van de lezer

    /** De naam die op het koppelscherm is ingetypt. */
    var pairingName: String = "Mijn Totem"

    /**
     * Er is een tag gelezen.
     *
     * Anders dan op iOS leest Android gewoon door zolang Totem op de voorgrond
     * staat; er is geen systeemvenster dat het scannen afbakent. Daarom is dit
     * één ingang voor élke tik, of je nu net op "activeren" hebt gedrukt of de
     * app alleen maar open hebt staan.
     *
     * Dat is niet alleen prettiger, het is ook nodig: zet je de lezer alleen
     * aan tijdens een scanvenster, dan pakt Android zelf de tag op zodra je
     * daarbuiten tikt — en krijg je zijn eigen melding "Lege tag" te zien in
     * plaats van dat Totem er iets mee doet.
     */
    fun onTagRead(result: Result<Pair<String, ByteArray?>>) {
        result.onFailure { error ->
            if (error !is TotemError.Cancelled) {
                _errorMessage.value = (error as? TotemError)?.text ?: error.message
            }
            return
        }
        val (uid, payload) = result.getOrNull() ?: return

        val request = _scanRequest.value
        _scanRequest.value = null

        when (request?.purpose) {
            ScanPurpose.PAIR -> koppel(uid, payload, request.name ?: pairingName)

            ScanPurpose.START -> {
                if (!accepteerTotem(uid, payload)) return
                val mode = request.modeID?.let { mode(it) } ?: return
                SessionEngine.start(context, mode, bySchedule = false)
            }

            ScanPurpose.STOP -> {
                if (!accepteerTotem(uid, payload)) return
                SessionEngine.stop(context, bySchedule = false)
            }

            ScanPurpose.PAUSE -> {
                if (!accepteerTotem(uid, payload)) return
                SessionEngine.pause(context)
            }

            // Een tik terwijl er niets gevraagd was.
            null -> losseTik(uid, payload)
        }
    }

    /** Een tik zonder dat de app erom vroeg. */
    private fun losseTik(uid: String, payload: ByteArray?) {
        if (_totem.value == null) {
            // Nog niets gekoppeld: dan is dit precies de bedoeling.
            koppel(uid, payload, pairingName)
            return
        }
        if (!accepteerTotem(uid, payload)) return
        if (_session.value != null) {
            // Tijdens een schema ontdooien, anders stoppen.
            SessionEngine.tapWhileRunning(context)
        } else {
            _awaitingModeChoice.value = true
        }
    }

    /**
     * Koppelt deze tag als jouw Totem.
     *
     * De echtheidscontrole hoort alléén hier. Bij het starten en stoppen is de
     * UID genoeg: die tag is bij het koppelen al echt bevonden. Zou je elke keer
     * de handtekening uit het taggeheugen lezen, dan kon één mislukte uitlezing
     * — een tik die net iets scheef zat — je opgesloten laten zitten in je eigen
     * blokkade.
     */
    private fun koppel(uid: String, payload: ByteArray?, name: String) {
        val uidBytes = runCatching {
            uid.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        }.getOrDefault(ByteArray(0))

        if (!TotemAuth.isGenuine(uidBytes, payload)) {
            _errorMessage.value = TotemError.WrongTag.text
            return
        }
        savePaired(uid, name.ifBlank { "Mijn Totem" })
    }

    fun cancelScan() {
        _scanRequest.value = null
    }

    private fun savePaired(uid: String, name: String) {
        val paired = PairedTotem(tagUID = uid, name = name, pairedAt = System.currentTimeMillis())
        _totem.value = paired
        SharedStore.totem = paired
    }

    /** Alleen de gekoppelde Totem mag bedienen? Zie accepteerTotem(). */
    private val ALLEEN_EIGEN_TOTEM = false

    private fun matchesPairedTotem(uid: String): Boolean =
        _totem.value?.tagUID?.equals(uid, ignoreCase = true) == true

    /**
     * Mag deze Totem de blokkade bedienen?
     *
     * Standaard elke echte Totem, gelijk aan iOS: daar staat in de code
     * "activeren en deactiveren kan met elke Totem". Handig als je er twee
     * hebt, of er een deelt binnen het gezin. De echtheidscontrole blijft
     * gewoon staan, dus een willekeurige NFC-tag werkt niet.
     *
     * Zet ALLEEN_EIGEN_TOTEM op true als uitsluitend de gekoppelde Totem mag
     * bedienen. Strenger, maar dan sta je met een lopende blokkade voor niets
     * als je je eigen Totem niet bij je hebt.
     */
    private fun accepteerTotem(uid: String, payload: ByteArray?): Boolean {
        if (ALLEEN_EIGEN_TOTEM) {
            if (matchesPairedTotem(uid)) return true
            _errorMessage.value = TotemError.WrongTag.text
            return false
        }

        val uidBytes = runCatching {
            uid.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        }.getOrDefault(ByteArray(0))

        if (TotemAuth.isGenuine(uidBytes, payload)) return true
        _errorMessage.value = TotemError.NotATotem.text
        return false
    }

    // MARK: - Ontdooien

    /** Of een tik nu zou ontdooien in plaats van stoppen. */
    fun tapPauses(): Boolean = SessionEngine.tapPauses()

    /** Weer bevriezen voordat de pauze om is. Daar hoeft geen Totem voor. */
    fun freezeNow() {
        SessionEngine.resume(context, notify = false)
    }

    // MARK: - Noodontgrendeling

    /**
     * Hoeveel noodontgrendelingen er deze maand nog over zijn.
     *
     * Bewust een StateFlow en geen gewone getter: het scherm laat dit getal
     * zien, en Compose hertekent alleen als het een stroom is om naar te
     * luisteren. Met een gewone getter bleef er "nog 0 over" staan nadat de
     * maand was omgeslagen.
     */
    val emergencyRemaining: StateFlow<Int> = _emergencyUsed
        .map { maxOf(0, EMERGENCY_LIMIT - it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, EMERGENCY_LIMIT)

    /**
     * Mag je nu zonder Totem ontgrendelen?
     *
     * In de strikte modus niet zomaar: dan hoort er eerst een pincode in.
     * Zonder deze koppeling zou de strikte modus alleen een schakelaar zijn
     * die nergens op uitkomt.
     */
    val emergencyNeedsPin: Boolean get() = _strictMode.value && PinService.isSet

    /**
     * Beëindigt de sessie zonder Totem. Bedoeld voor als je je Totem kwijt bent
     * of niet bij de hand hebt. Beperkt tot vijf keer per kalendermaand.
     */
    fun useEmergencyUnlock(): Boolean {
        refreshEmergencyPeriod()
        _emergencyUsed.value = SharedStore.emergencyUsed
        if (EMERGENCY_LIMIT - _emergencyUsed.value <= 0) {
            _errorMessage.value = "Je hebt deze maand al $EMERGENCY_LIMIT " +
                "noodontgrendelingen gebruikt. Volgende maand krijg je er weer $EMERGENCY_LIMIT."
            return false
        }
        if (_session.value == null) return false

        _emergencyUsed.value = _emergencyUsed.value + 1
        SharedStore.emergencyUsed = _emergencyUsed.value
        SessionEngine.stop(context, bySchedule = false)
        return true
    }

    private fun refreshEmergencyPeriod() {
        val cal = Calendar.getInstance()
        val period = "%04d-%02d".format(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        if (period != SharedStore.emergencyPeriod) {
            SharedStore.emergencyPeriod = period
            SharedStore.emergencyUsed = 0
            _emergencyUsed.value = 0
        }
    }

    // MARK: - Strikte modus

    fun setStrictMode(on: Boolean) {
        _strictMode.value = on
        SharedStore.strictMode = on
    }

    // MARK: - Toestemmingen

    fun hasShieldAccess(): Boolean = ShieldService.isReady(context)

    // MARK: - Foutmelding

    fun clearError() {
        _errorMessage.value = null
    }

    fun showError(text: String) {
        _errorMessage.value = text
    }

    // MARK: - Opslag

    /** Herleest alles uit de opslag. De opslag is de enige bron van waarheid. */
    fun load() {
        // Ook hier de maand controleren: het proces kan dagen blijven leven
        // doordat de blokkadedienst draait, en dan zou de teller pas bij een
        // herstart van de app terugspringen.
        refreshEmergencyPeriod()
        _totem.value = SharedStore.totem
        _modes.value = SharedStore.modes
        _session.value = SharedStore.session
        _history.value = SharedStore.history
        _emergencyUsed.value = SharedStore.emergencyUsed
        _strictMode.value = SharedStore.strictMode
        _needsSetup.value = !SharedStore.setupDone && _modes.value.none { it.isConfigured }
    }

    /** Bij het openen van de app: kijken of de schema's nog kloppen. */
    fun onAppear() {
        SessionEngine.syncWithSchedule(context)
        load()
    }

    companion object {
        /** Aantal noodontgrendelingen per maand. */
        const val EMERGENCY_LIMIT = 5
    }
}
