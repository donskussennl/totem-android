package nl.totem.app.nfc

import android.net.Uri

/**
 * Leest een binnenkomende Totem-link, in alle vormen die de tags en de website
 * kunnen opleveren.
 *
 *   https://app.tap2totem.com/nl/nfc/07I5000101/045FA25A2D1291/<handtekening>
 *   https://app.tap2totem.com/nfc/07I5000101/045FA25A2D1291/<handtekening>
 *   https://app.tap2totem.com/verify?uid=...&tag=...&sig=...
 *   https://app.tap2totem.com/t/045FA25A2D1291
 *   totem://nfc/07I5000101/045FA25A2D1291/<handtekening>
 *
 * De handtekening is standaard base64 en bevat dus zelf `/`. Die valt in de URL
 * uiteen over meerdere padsegmenten; hieronder wordt hij weer aan elkaar
 * geplakt.
 *
 * Let op: Android stuurt bij een tik op een link ACTION_VIEW, maar bij het
 * lezen van dezelfde link van een NFC-tag NDEF_DISCOVERED. Beide staan in het
 * manifest.
 */
object TapLink {

    /** Moet overeenkomen met de intent-filters en met assetlinks.json. */
    val HOSTS = setOf("app.tap2totem.com", "tap2totem.com")

    /** Voor testen zonder domein: totem://nfc/... */
    const val SCHEME = "totem"

    private val KINDS = setOf("nfc", "t", "verify")

    data class Scan(
        /** Artikelnummer, bijvoorbeeld 07I5000101 */
        val itemCode: String?,
        /** Hardware-UID van de chip, bijvoorbeeld 045FA25A2D1291 */
        val tagUid: String,
        /** Base64 handtekening, 64 tekens */
        val signature: String?,
    )

    fun scan(uri: Uri?): Scan? {
        if (uri == null) return null
        val scheme = uri.scheme?.lowercase()
        val host = uri.host?.lowercase() ?: ""
        val isUniversal = (scheme == "https" || scheme == "http") && host in HOSTS
        val isCustom = scheme == SCHEME
        if (!isUniversal && !isCustom) return null

        val parts = ArrayList(uri.pathSegments.orEmpty().filter { it.isNotEmpty() })

        // Bij totem://nfc/... staat "nfc" in de host in plaats van in het pad.
        if (isCustom && host.isNotEmpty()) parts.add(0, host)

        // Taalprefix overslaan: /nl/nfc/... gedraagt zich als /nfc/...
        val first = parts.firstOrNull()
        if (first != null && first !in KINDS && first.length <= 5) parts.removeAt(0)

        return when (parts.firstOrNull()) {
            "nfc" -> {
                if (parts.size < 3) return null
                val tag = normalize(parts[2])
                if (!isValidTag(tag)) return null
                // Alles na de UID hoort bij de handtekening, inclusief de `/`
                // die base64 zelf produceert.
                val signature = if (parts.size > 3) {
                    parts.subList(3, parts.size).joinToString("/")
                } else null
                Scan(parts[1].uppercase(), tag, signature)
            }

            "t" -> {
                if (parts.size < 2) return null
                val tag = normalize(parts[1])
                if (!isValidTag(tag)) return null
                Scan(uri.getQueryParameter("uid")?.uppercase(),
                     tag,
                     uri.getQueryParameter("sig"))
            }

            "verify" -> {
                val raw = uri.getQueryParameter("tag") ?: return null
                val tag = normalize(raw)
                if (!isValidTag(tag)) return null
                Scan(uri.getQueryParameter("uid")?.uppercase(),
                     tag,
                     uri.getQueryParameter("sig"))
            }

            else -> null
        }
    }

    /** Blijft werken zoals voorheen: alleen de chip-UID. */
    fun uid(uri: Uri?): String? = scan(uri)?.tagUid

    /** Het adres dat je op de tag schrijft. */
    fun url(forTag: String): String =
        "https://app.tap2totem.com/t/${forTag.uppercase()}"

    private fun normalize(raw: String): String =
        raw.replace(":", "").replace("-", "").replace(" ", "").uppercase()

    /** ISO 14443-A: 4, 7 of 10 bytes, dus 8, 14 of 20 hex-tekens. */
    private fun isValidTag(tag: String): Boolean =
        tag.length in setOf(8, 14, 20) &&
            tag.all { it in '0'..'9' || it in 'A'..'F' }
}
