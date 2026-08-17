package nl.totem.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import nl.totem.app.nfc.NfcService
import nl.totem.app.nfc.TapLink
import nl.totem.app.store.AppStore
import nl.totem.app.ui.RootScreen
import nl.totem.app.ui.TotemTheme

/**
 * Het enige scherm van de app; alle navigatie gebeurt in Compose.
 *
 * Deze activiteit doet drie dingen die alleen hier kunnen:
 *
 * 1. **Lezen** — de NfcAdapter heeft een activiteit nodig om aan te haken. De
 *    lezer staat aan zolang dit scherm op de voorgrond is, en élke tik gaat
 *    naar het model. Wat ermee gebeurt — koppelen, starten, stoppen — beslist
 *    het model zelf.
 * 2. **Tikken terwijl de app dicht was** — dan opent Android ons rechtstreeks
 *    met de link van de tag. Zie [TapLink].
 * 3. **Toestemming voor meldingen** vragen, sinds Android 13.
 */
class MainActivity : ComponentActivity() {

    private val store: AppStore by viewModels()
    private lateinit var nfc: NfcService

    private val vraagMeldingen = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* de uitkomst mag Totem niet in de weg zitten */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        nfc = NfcService(this)

        verwerkTapLink(intent)
        vraagMeldingtoestemming()

        setContent {
            TotemTheme {
                RootScreen(store)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        verwerkTapLink(intent)
    }

    override fun onResume() {
        super.onResume()

        // De lezer staat áltijd aan zolang Totem op de voorgrond is, niet
        // alleen tijdens een scanvenster. Dat is het grote verschil met iOS:
        // daar opent het systeem een venster dat het scannen afbakent, hier
        // luistert de telefoon gewoon mee. Zetten we hem alleen tijdens een
        // scan aan, dan vangt Android elke andere tik zelf op en zie je zijn
        // melding "Lege tag" — precies wat je níét wilt.
        android.util.Log.i(NfcService.TAG, "MainActivity.onResume — lezer wordt aangezet")
        nfc.start { result ->
            android.util.Log.i(NfcService.TAG, "MainActivity kreeg een tik binnen")
            store.onTagRead(result.map { lezing -> lezing.uid to lezing.payload })
        }

        // Terug uit de systeeminstellingen: opnieuw kijken hoe alles ervoor staat.
        store.onAppear()
    }

    override fun onPause() {
        super.onPause()
        // Buiten de voorgrond mogen we de lezer niet claimen.
        nfc.stop()
    }

    /**
     * Je hebt de Totem aangetikt terwijl de app dicht was; Android opende ons
     * met het adres dat op de tag staat.
     */
    private fun verwerkTapLink(intent: Intent?) {
        if (intent == null) return
        val uid = TapLink.uid(intent.data) ?: return

        // Meteen opeten. Zonder dit zou dezelfde tik opnieuw worden verwerkt
        // bij elke hertekening van de activiteit — draaien, donkere modus,
        // lettergrootte — en dan zou het toestel je blokkade zomaar stoppen
        // zonder dat je je Totem hebt aangeraakt.
        intent.data = null
        setIntent(intent)

        store.handleTapLink(uid)
    }

    private fun vraagMeldingtoestemming() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        runCatching { vraagMeldingen.launch(Manifest.permission.POST_NOTIFICATIONS) }
    }
}
