package nl.totem.app.nfc

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import nl.totem.app.model.TotemError

/**
 * Leest de UID van een NFC-tag uit. Wordt gebruikt om te koppelen én om een
 * sessie te starten of te beëindigen.
 *
 * Het grote verschil met iOS: daar opent het systeem een eigen scanvenster met
 * een tekst erin. Android heeft dat niet — de telefoon leest continu mee zodra
 * je de reader mode aanzet. De app moet dus zelf een scherm laten zien met
 * "houd je telefoon tegen je Totem", en dat weer weghalen als er gelezen is.
 * Dat scherm is [nl.totem.app.ui.ScanSheet].
 */
class NfcService(private val activity: Activity) {

    private val adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)

    private var onResult: ((Result<TagRead>) -> Unit)? = null

    /** Wat er van een tag is gelezen. */
    data class TagRead(val uid: String, val payload: ByteArray?) {
        // Nodig omdat een ByteArray in een data class op referentie vergelijkt.
        override fun equals(other: Any?): Boolean =
            other is TagRead && other.uid == uid
        override fun hashCode(): Int = uid.hashCode()
    }

    val isAvailable: Boolean get() = adapter != null
    val isEnabled: Boolean get() = adapter?.isEnabled == true

    /**
     * Zet de lezer aan. Blijft luisteren tot [stop] wordt aangeroepen of er een
     * tag is gelezen.
     */
    fun start(onResult: (Result<TagRead>) -> Unit) {
        val adapter = adapter ?: run {
            Log.w(TAG, "start: dit toestel heeft geen NFC-adapter")
            onResult(Result.failure(TotemError.NfcUnavailable)); return
        }
        if (!adapter.isEnabled) {
            Log.w(TAG, "start: NFC staat uit in de systeeminstellingen")
            onResult(Result.failure(TotemError.NfcDisabled)); return
        }

        this.onResult = onResult

        val flags = NfcAdapter.FLAG_READER_NFC_A or
            NfcAdapter.FLAG_READER_NFC_B or
            NfcAdapter.FLAG_READER_NFC_F or
            NfcAdapter.FLAG_READER_NFC_V or
            // Geen systeemgeluid en geen NDEF-afhandeling: wij doen het zelf.
            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK or
            NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS

        val extras = Bundle().apply {
            // Iets ruimer dan de standaard, zodat een korte tik ook aankomt.
            putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250)
        }

        runCatching {
            adapter.enableReaderMode(activity, { tag -> handle(tag) }, flags, extras)
        }.onSuccess {
            Log.i(TAG, "lezer AAN voor ${activity.javaClass.simpleName}")
        }.onFailure {
            Log.e(TAG, "lezer aanzetten mislukt voor ${activity.javaClass.simpleName}", it)
        }
    }

    /** Zet de lezer weer uit. Altijd aanroepen als het scherm weggaat. */
    fun stop() {
        onResult = null
        runCatching { adapter?.disableReaderMode(activity) }
        Log.i(TAG, "lezer UIT voor ${activity.javaClass.simpleName}")
    }

    /** De gebruiker klikte het scanscherm weg. */
    fun cancel() {
        val callback = onResult
        stop()
        callback?.invoke(Result.failure(TotemError.Cancelled))
    }

    /**
     * Let op: de lezer blijft hierna gewoon aan staan.
     *
     * Op iOS sloot het systeemvenster zichzelf na één tag; hier houden we de
     * lezer draaiend zolang het scherm er is. Zou hij na elke tik uitgaan, dan
     * pakt Android de volgende tik zelf op — en toont zijn eigen melding
     * "Lege tag" in plaats van dat Totem hem krijgt.
     */
    private fun handle(tag: Tag) {
        val callback = onResult
        Log.i(TAG, "TAG GELEZEN: uid=${hex(tag.id)} tech=${tag.techList.joinToString()} " +
            "wachtende=${callback != null}")
        if (callback == null) return
        val uid = hex(tag.id)
        if (uid.isEmpty()) {
            activity.runOnUiThread { callback(Result.failure(TotemError.WrongTag)) }
            return
        }

        val payload = runCatching { TagMemory.read(tag) }
            .onFailure { Log.d("Totem", "Taggeheugen lezen mislukt: $it") }
            .getOrNull()

        activity.runOnUiThread { callback(Result.success(TagRead(uid, payload))) }
    }

    companion object {
        /** Eén etiket voor alles wat met lezen te maken heeft: `adb logcat -s TotemNFC`. */
        const val TAG = "TotemNFC"

        fun hex(data: ByteArray): String = data.joinToString("") { "%02X".format(it) }
    }
}
