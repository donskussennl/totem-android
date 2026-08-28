package nl.totem.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import nl.totem.app.data.SharedStore

/**
 * De prominente kennisgeving bij de AccessibilityService API.
 *
 * Google Play keurde de app eerder af omdat de uitleg alleen als tekst in het
 * instelscherm stond, met één knop ernaast. Het beleid stelt vier eisen, en
 * die zijn hier stuk voor stuk ingebouwd:
 *
 * 1. de kennisgeving staat in een eigen venster, vóór de toestemming wordt
 *    gevraagd -- de knop naar de Android-instelling zit er achter, niet ervoor;
 * 2. er is een bevestigende handeling nodig: de gebruiker moet 'Ik ga akkoord'
 *    aantikken;
 * 3. weglopen telt niet als toestemming. Wegtikken naast het venster en de
 *    terugknop doen niets (dismissOnClickOutside en dismissOnBackPress staan
 *    uit), en er zijn twee knoppen zodat weigeren net zo bereikbaar is als
 *    aannemen;
 * 4. er zit geen tijdslimiet op. Het venster sluit alleen door een van beide
 *    knoppen.
 *
 * De tekst zelf noemt drie dingen die de beoordeling nakijkt: welke gegevens
 * Totem benadert, waarvoor, en dat er niets wordt verzameld of gedeeld.
 */
@Composable
fun ToegankelijkheidKennisgeving(
    onAkkoord: () -> Unit,
    onNietAkkoord: () -> Unit
) {
    AlertDialog(
        // Leeg: dit venster gaat niet uit zichzelf dicht. Zie punt 3 hierboven.
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        title = {
            Text(
                "Totem wil de Toegankelijkheidsservice gebruiken",
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Alinea(
                    "Totem gebruikt de Toegankelijkheidsservice (AccessibilityService) " +
                        "van Android. Hieronder staat wat dat betekent."
                )
                Alinea(
                    "Wat Totem uitleest: zodra je een andere app naar de voorgrond " +
                        "haalt, krijgt Totem daar een seintje van en leest daaruit " +
                        "alleen de naam van die app."
                )
                Alinea(
                    "Waarvoor: om te zien of die app hoort bij de focusmodus die je " +
                        "hebt aangezet, en om er in dat geval het blokkadescherm " +
                        "overheen te tonen. Zonder deze toestemming kan Totem niets " +
                        "blokkeren."
                )
                Alinea(
                    "Wat Totem niet doet: de inhoud van je scherm wordt niet gelezen, " +
                        "er worden geen toetsaanslagen opgeslagen en er worden geen " +
                        "schermafbeeldingen gemaakt."
                )
                Alinea(
                    "Gegevens: de naam van de geopende app blijft op dit toestel. Die " +
                        "wordt niet bewaard en niet verzameld, gedeeld of verkocht -- " +
                        "niet aan ons en niet aan derden."
                )
                Alinea(
                    "Je kunt de service later altijd uitzetten via Instellingen → " +
                        "Toegankelijkheid op je toestel, of via Instellingen in Totem."
                )
                Alinea(
                    "Tik je op 'Ik ga akkoord', dan brengt Totem je naar de " +
                        "Android-instelling waar je de service zelf aanzet. Tik je op " +
                        "'Niet akkoord', dan gebeurt er niets en blijft de service uit."
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                SharedStore.accessibilityConsent = true
                onAkkoord()
            }) { Text("Ik ga akkoord") }
        },
        dismissButton = {
            TextButton(onClick = onNietAkkoord) { Text("Niet akkoord") }
        }
    )
}

@Composable
private fun Alinea(tekst: String) {
    Text(
        text = tekst,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}
