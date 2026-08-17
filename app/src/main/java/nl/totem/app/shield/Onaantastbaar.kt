package nl.totem.app.shield

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.telecom.TelecomManager

/**
 * De apps die Totem nooit mag blokkeren.
 *
 * Dit bestaat op iOS niet: daar bepaalde Apple zelf welke apps buiten schot
 * bleven, en Schermtijd liet de instellingen en de telefoon-app altijd door.
 * Op Android bepalen wíj wie er wordt tegengehouden, en dan is deze lijst het
 * verschil tussen een focus-app en een baksteen.
 *
 * Wat er zonder deze lijst misgaat:
 *
 * - **Instellingen geblokkeerd** — dan kun je de toegankelijkheidsdienst niet
 *   meer uitzetten en de app niet meer verwijderen. Er is dan geen weg terug
 *   behalve de veilige modus.
 * - **De launcher geblokkeerd** — de dienst drukt op "home", de launcher komt
 *   naar voren, de dienst drukt weer op "home". Het toestel is onbruikbaar tot
 *   de sessie afloopt.
 * - **De telefoon-app geblokkeerd** — dan kun je niet meer bellen, ook niet
 *   naar 112.
 * - **Het toetsenbord geblokkeerd** — dan kun je nergens meer iets intypen,
 *   ook niet je pincode.
 *
 * De lijst wordt op twee plekken gebruikt: de app-kiezer laat ze niet zien, en
 * de dienst slaat ze over. Dat tweede is nodig omdat een opgeslagen modus nog
 * een pakket kan bevatten van vóór je van launcher wisselde.
 */
object Onaantastbaar {

    /** Vaste pakketten die altijd door mogen. */
    private val vast = setOf(
        "com.android.systemui",
        "com.android.settings",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.android.emergency",
        "com.android.server.telecom"
    )

    /**
     * De volledige lijst voor dit toestel. Wordt bij elke aanroep opnieuw
     * bepaald, want de gebruiker kan van launcher, toetsenbord of telefoon-app
     * wisselen terwijl de app draait.
     */
    fun voor(context: Context): Set<String> {
        val pakketten = vast.toMutableSet()
        pakketten += context.packageName

        // De launcher: het startscherm waar we mensen naartoe sturen.
        launcher(context)?.let { pakketten += it }

        // De instellingen, zoals dit toestel ze noemt.
        resolve(context, Intent(Settings.ACTION_SETTINGS))?.let { pakketten += it }

        // De telefoon-app, zodat bellen altijd kan.
        dialer(context)?.let { pakketten += it }

        // Het toetsenbord dat nu gekozen is.
        toetsenbord(context)?.let { pakketten += it }

        return pakketten
    }

    private fun launcher(context: Context): String? = resolve(
        context,
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    )

    private fun dialer(context: Context): String? = runCatching {
        val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        telecom?.defaultDialerPackage
    }.getOrNull() ?: resolve(context, Intent(Intent.ACTION_DIAL))

    private fun toetsenbord(context: Context): String? = runCatching {
        Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD
        )?.substringBefore('/')?.takeIf { it.isNotBlank() }
    }.getOrNull()

    private fun resolve(context: Context, intent: Intent): String? = runCatching {
        context.packageManager
            .resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo
            ?.packageName
            ?.takeIf { it != "android" }
    }.getOrNull()
}
