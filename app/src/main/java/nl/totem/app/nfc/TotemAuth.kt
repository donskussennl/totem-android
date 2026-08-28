package nl.totem.app.nfc

import android.net.Uri
import android.nfc.NdefMessage
import android.util.Base64
import android.util.Log
import java.math.BigInteger

/**
 * Controleert of een gescande tag een echte Totem is.
 *
 * Twee controles, en ze zijn allebei nodig:
 *
 *  1. **Handtekening.** Bij het programmeren tekent de productie-tool de tekst
 *     `<artikelnummer><chip-UID>` met een geheime sleutel (ECDSA P-192,
 *     SHA-256). Die handtekening staat als base64 in de URL op de tag. De app
 *     kent alleen de publieke sleutel: daarmee kun je controleren, maar niets
 *     namaken.
 *
 *  2. **UID-vergelijking.** De UID uit de URL wordt vergeleken met de UID die
 *     de chip zelf teruggeeft. Zonder deze stap kan iemand de URL overtypen en
 *     op een eigen chip zetten -- de handtekening zou dan nog steeds kloppen.
 *     Alleen de app kan dit; de website ziet de chip nooit.
 *
 * Het formaat komt een op een uit `crypto.py` van de tag-programmer:
 *
 *     bericht   = totem_uid + nfc_uid          (ASCII, aan elkaar geplakt)
 *     curve     = NIST P-192 (secp192r1)
 *     digest    = SHA-256
 *     encoding  = r.to_bytes(24) + s.to_bytes(24)   -> 48 bytes
 *     transport = standaard base64                  -> 64 tekens, in de URL
 *
 * Anders dan bij iOS is P-192 hier geen probleem: Android's JCA leest de curve
 * uit de X.509-encoding van de sleutel en heeft geen aparte ondersteuning
 * nodig.
 */
object TotemAuth {

    private const val TAG = "TotemAuth"

    /**
     * Publieke sleutel als losse coordinaten (het ongecomprimeerde punt
     * 0x04 || X || Y, opgesplitst). Geen X.509 meer: er is geen KeyFactory op
     * Android die deze curve nog inleest.
     */
    private val QX = BigInteger("F0A915C4BDACD97DCCEE4BF477CD7667E39D12719E4B68D3", 16)
    private val QY = BigInteger("AE1F8038E0D38B91543EA0719501C6392B56CB2D28313D37", 16)

    /** De sleutel is ingevuld; er wordt altijd gecontroleerd. */
    val isConfigured: Boolean get() = true

    /**
     * @param uid hardware-UID van de chip, zoals de NfcAdapter hem teruggeeft
     * @param payload bytes uit het taggeheugen vanaf pagina 4
     */
    fun isGenuine(uid: ByteArray, payload: ByteArray?): Boolean {
        val uri = ndefUri(payload)
        if (uri == null) {
            Log.w(TAG, "geen leesbare NDEF-URL op de tag")
            return false
        }

        Log.i(TAG, "URL van de tag: $uri")

        val scan = TapLink.scan(uri)
        if (scan == null) {
            Log.w(TAG, "URL hoort niet bij Totem: $uri")
            return false
        }

        // Staat deze URL wel op de chip waarvoor hij gemaakt is?
        if (hex(uid) != scan.tagUid) {
            Log.w(TAG, "UID komt niet overeen: chip ${hex(uid)}, URL ${scan.tagUid}")
            return false
        }

        val itemCode = scan.itemCode
        val signature = scan.signature
        if (itemCode == null || signature == null) {
            Log.w(TAG, "artikelnummer of handtekening ontbreekt in de URL")
            return false
        }

        val bytes = decodeBase64(signature)
        Log.i(TAG, "controle $itemCode/${scan.tagUid}")
        Log.i(TAG, "handtekening: $signature")
        Log.i(TAG, "bericht: '$itemCode${scan.tagUid}' -> ${bytes?.size ?: -1} bytes handtekening")

        val ok = verify(itemCode, scan.tagUid, signature)
        if (!ok) Log.w(TAG, "handtekening ongeldig voor $itemCode/${scan.tagUid}")
        return ok
    }

    /** Controleert de handtekening over `itemCode + tagUid`. */
    fun verify(itemCode: String, tagUid: String, signature: String): Boolean {
        val raw = decodeBase64(signature) ?: return false
        if (raw.size != P192.COORDINATE_BYTES * 2) {
            Log.w(TAG, "handtekening is ${raw.size} bytes, verwacht 48")
            return false
        }
        val message = (itemCode + tagUid).toByteArray(Charsets.UTF_8)
        return runCatching { P192.verify(message, raw, QX, QY) }
            .onFailure { Log.w(TAG, "verificatie klapte", it) }
            .getOrDefault(false)
    }

    /**
     * Draai dit een keer bij het opstarten en kijk in logcat.
     * Gebruikt een echte, elders geverifieerde tag.
     */
    fun selfTest(): Boolean {
        val ok = verify(
            "07I5000101",
            "045FA25A2D1291",
            "moWpvuXrEFr88ConsJYejnIKtl7BB85QzD5z27iNzCtY7eh0wcFKR2tbiY9Q5tSq",
        )
        if (!ok) {
            Log.w(TAG, "punt ligt op de curve: ${P192.isOnCurve(QX, QY)}")
        }
        Log.i(TAG, if (ok) "ZELFTEST GESLAAGD: P-192 verificatie werkt."
                   else "ZELFTEST MISLUKT: controleer sleutel en formaat.")
        return ok
    }

    /**
     * Haalt de URL uit het taggeheugen. Verwacht het standaard NDEF-formaat:
     * TLV 0x03, lengte, dan het NDEF-bericht.
     */
    fun ndefUri(payload: ByteArray?): Uri? {
        if (payload == null) return null
        var i = 0
        while (i < payload.size) {
            when (payload[i].toInt() and 0xFF) {
                0x00 -> i++                       // opvulling
                0xFE -> return null               // einde, geen bericht gevonden
                0x03 -> {                         // NDEF Message TLV
                    if (i + 1 >= payload.size) return null
                    var length = payload[i + 1].toInt() and 0xFF
                    var start = i + 2
                    if (length == 0xFF) {         // lange vorm, twee lengtebytes
                        if (i + 3 >= payload.size) return null
                        length = ((payload[i + 2].toInt() and 0xFF) shl 8) or
                            (payload[i + 3].toInt() and 0xFF)
                        start = i + 4
                    }
                    if (length <= 0 || start + length > payload.size) return null
                    return runCatching {
                        NdefMessage(payload.copyOfRange(start, start + length))
                            .records.firstOrNull()?.toUri()
                    }.getOrNull()
                }
                else -> {                          // andere TLV: overslaan
                    if (i + 1 >= payload.size) return null
                    i += 2 + (payload[i + 1].toInt() and 0xFF)
                }
            }
        }
        return null
    }

    /** Hexweergave van de UID, zoals de app hem bewaart. */
    fun hex(data: ByteArray): String = data.joinToString("") { "%02X".format(it) }

    /**
     * Accepteert base64 en base64url, met of zonder opvulling.
     *
     * Let op: `Base64.DEFAULT or Base64.URL_SAFE` doet NIET wat het lijkt.
     * DEFAULT is 0, dus die combinatie is puur URL_SAFE -- en dat alfabet kent
     * `+` en `/` niet. Een gewone base64-handtekening klapt daar dus op.
     * Daarom eerst zelf normaliseren en dan met het standaard alfabet lezen.
     */
    private fun decodeBase64(text: String): ByteArray? {
        var s = text.trim().replace('-', '+').replace('_', '/')
        val remainder = s.length % 4
        if (remainder > 0) s += "=".repeat(4 - remainder)
        return runCatching { Base64.decode(s, Base64.DEFAULT) }
            .onFailure { Log.w(TAG, "handtekening is geen geldige base64: $text", it) }
            .getOrNull()
    }

}
