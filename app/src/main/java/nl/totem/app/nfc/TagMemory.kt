package nl.totem.app.nfc

import android.nfc.Tag
import android.nfc.tech.MifareUltralight
import android.nfc.tech.NfcA

/**
 * Leest het gebruikersgeheugen van de tag, vanaf pagina 4.
 *
 * Daar staat de handtekening die bewijst dat dit een echte Totem is; zie
 * [TotemAuth]. NTAG213/215/216 en MIFARE Ultralight lezen vier pagina's van
 * vier bytes tegelijk, dus 16 bytes per opdracht.
 */
object TagMemory {

    /** Zoveel bytes hebben we nodig: 8 kop + 64 handtekening. */
    private const val WANTED = 80

    fun read(tag: Tag): ByteArray? {
        MifareUltralight.get(tag)?.let { return readUltralight(it) }
        NfcA.get(tag)?.let { return readNfcA(it) }
        return null
    }

    private fun readUltralight(tech: MifareUltralight): ByteArray? = try {
        tech.connect()
        val out = ByteArray(WANTED)
        var written = 0
        var page = 4
        while (written < WANTED) {
            val chunk = tech.readPages(page)      // 16 bytes = 4 pagina's
            val take = minOf(chunk.size, WANTED - written)
            System.arraycopy(chunk, 0, out, written, take)
            written += take
            page += 4
        }
        out
    } catch (_: Exception) {
        null
    } finally {
        runCatching { tech.close() }
    }

    /**
     * Voor tags die zich niet als Ultralight melden maar wel het READ-commando
     * (0x30) kennen. Werkt op vrijwel alle NTAG-chips.
     */
    private fun readNfcA(tech: NfcA): ByteArray? = try {
        tech.connect()
        val out = ByteArray(WANTED)
        var written = 0
        var page = 4
        while (written < WANTED) {
            val chunk = tech.transceive(byteArrayOf(0x30, page.toByte()))
            if (chunk.size < 4) break
            val take = minOf(chunk.size, WANTED - written)
            System.arraycopy(chunk, 0, out, written, take)
            written += take
            page += 4
        }
        if (written == 0) null else out
    } catch (_: Exception) {
        null
    } finally {
        runCatching { tech.close() }
    }
}
