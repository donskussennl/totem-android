package nl.totem.app.nfc

import android.util.Base64
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

/**
 * Controleert of een gescande tag een échte Totem is.
 *
 * Hoe het werkt: bij het produceren tekenen wij de UID van de chip met onze
 * geheime sleutel (ECDSA P-256) en schrijven die handtekening in het geheugen
 * van de tag. De app kent alleen de publieke sleutel en kan daarmee
 * controleren of de handtekening klopt.
 *
 * Waarom asymmetrisch: de geheime sleutel blijft bij ons. Iemand die de app
 * uit elkaar haalt vindt alleen de publieke sleutel, en daarmee kun je wel
 * controleren maar niets namaken.
 *
 * Let op het formaatverschil met iOS. CryptoKit werkt met een ruwe
 * handtekening van 64 bytes (r‖s); Java's `Signature` wil DER. Daarom zetten
 * we hem hieronder eerst om. De tags zelf blijven dus precies hetzelfde als
 * bij de iOS-app — dezelfde Totem werkt op beide telefoons.
 */
object TotemAuth {

    /**
     * Onze publieke sleutel in DER-formaat (X.509 SubjectPublicKeyInfo), als
     * base64. Vervang deze waarde door je eigen sleutel; zie SLEUTELS.md.
     */
    private const val PUBLIC_KEY_BASE64 = "VERVANG_DIT_DOOR_JE_PUBLIEKE_SLEUTEL"

    /** Herkenningstekens die vooraan in het taggeheugen staan: "TOTM". */
    private val MAGIC = byteArrayOf(0x54, 0x4F, 0x54, 0x4D)
    private const val FORMAT_VERSION: Byte = 1

    /**
     * Zolang er geen echte sleutel is ingevuld, accepteert de app elke tag.
     * Zo blijft ontwikkelen mogelijk voordat de productie loopt.
     */
    val isConfigured: Boolean get() = !PUBLIC_KEY_BASE64.startsWith("VERVANG")

    private val publicKey: PublicKey? by lazy {
        runCatching {
            val der = Base64.decode(PUBLIC_KEY_BASE64, Base64.DEFAULT)
            KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(der))
        }.getOrNull()
    }

    /**
     * Controleert de handtekening die op de tag staat tegen de UID.
     *
     * @param uid de hardware-UID van de chip, zoals gelezen door de NfcAdapter
     * @param payload de bytes uit het taggeheugen vanaf pagina 4
     */
    fun isGenuine(uid: ByteArray, payload: ByteArray?): Boolean {
        if (!isConfigured) return true
        if (payload == null) return false

        // 1. kopregel controleren: "TOTM" + versie
        if (payload.size < 5) return false
        if (!payload.copyOfRange(0, 4).contentEquals(MAGIC)) return false
        if (payload[4] != FORMAT_VERSION) return false

        // 2. handtekening eruit halen (64 bytes ruwe r‖s, kop is aangevuld tot 8)
        val start = 8
        if (payload.size < start + 64) return false
        val raw = payload.copyOfRange(start, start + 64)

        // 3. controleren tegen onze publieke sleutel
        val key = publicKey ?: return false
        val der = rawToDer(raw) ?: return false
        return runCatching {
            Signature.getInstance("SHA256withECDSA").run {
                initVerify(key)
                update(uid)
                verify(der)
            }
        }.getOrDefault(false)
    }

    /**
     * Zet een ruwe r‖s-handtekening van 64 bytes om naar de DER-vorm die Java
     * verwacht: SEQUENCE { INTEGER r, INTEGER s }.
     */
    private fun rawToDer(raw: ByteArray): ByteArray? {
        if (raw.size != 64) return null
        val r = toDerInteger(raw.copyOfRange(0, 32))
        val s = toDerInteger(raw.copyOfRange(32, 64))
        val body = r + s
        if (body.size > 0xFF) return null
        return if (body.size <= 0x7F) {
            byteArrayOf(0x30, body.size.toByte()) + body
        } else {
            byteArrayOf(0x30, 0x81.toByte(), body.size.toByte()) + body
        }
    }

    private fun toDerInteger(value: ByteArray): ByteArray {
        // Voorloopnullen weg, want DER wil de kortste weergave...
        var from = 0
        while (from < value.size - 1 && value[from] == 0.toByte()) from++
        var trimmed = value.copyOfRange(from, value.size)
        // ...maar een gezette hoogste bit zou negatief betekenen, dus dan een
        // nulbyte ervoor.
        if (trimmed[0].toInt() and 0x80 != 0) trimmed = byteArrayOf(0) + trimmed
        return byteArrayOf(0x02, trimmed.size.toByte()) + trimmed
    }

    /** Hexweergave van de UID, zoals de app hem bewaart. */
    fun hex(data: ByteArray): String = data.joinToString("") { "%02X".format(it) }
}
