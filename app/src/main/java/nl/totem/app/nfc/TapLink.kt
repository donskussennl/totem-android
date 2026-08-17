package nl.totem.app.nfc

import android.net.Uri

/**
 * Vertaalt de link die binnenkomt als iemand zijn telefoon tegen de Totem
 * houdt terwijl de app dicht is.
 *
 * Op de tag staat een NDEF-record met een adres als:
 *     https://tap2totem.app/t/04A1B2C3D4E580
 *
 * Android leest dat vanzelf en opent de app die zich voor dat adres heeft
 * aangemeld — zie de intent-filters van MainActivity in het manifest. Anders
 * dan op iOS is daar geen tussenbanner voor nodig: de app opent meteen.
 */
object TapLink {

    /** Jouw domein. Hoort bij het intent-filter en bij assetlinks.json. */
    const val HOST = "tap2totem.app"

    /** Voor testen zonder domein: totem://t/04A1B2C3D4E580 */
    const val SCHEME = "totem"

    /** Haalt de UID uit een binnenkomende link. Geeft null bij een ander adres. */
    fun uid(uri: Uri?): String? {
        if (uri == null) return null
        val scheme = uri.scheme?.lowercase()
        val isUniversal = scheme?.startsWith("http") == true && uri.host == HOST
        val isCustom = scheme == SCHEME
        if (!isUniversal && !isCustom) return null

        // pad ziet eruit als /t/<uid>
        val parts = uri.pathSegments.orEmpty().filter { it.isNotEmpty() }
        val index = parts.indexOf("t")
        if (index >= 0 && parts.size > index + 1) {
            return parts[index + 1].uppercase()
        }
        // bij het eigen schema zit het pad soms in de host
        val host = uri.host
        if (isCustom && host != null && host != "t") return host.uppercase()
        return null
    }

    /** Het adres dat je op de tag schrijft. */
    fun url(forTag: String): String = "https://$HOST/t/${forTag.uppercase()}"
}
