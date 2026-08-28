package nl.totem.app.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import nl.totem.app.data.SharedStore
import nl.totem.app.notify.NotificationService
import nl.totem.app.shield.ShieldService

/**
 * Het scherm dat Totem klaarzet voordat je begint.
 *
 * Dit heeft op iOS geen tegenhanger, en dat is precies waarom het er moet zijn.
 * Daar vroeg de app één keer om Schermtijd en klaar; Android kent voor
 * toegankelijkheid en schermoverlays helemaal geen dialoogvenster. Een app mág
 * er niet eens om vragen — de gebruiker moet ze zelf omzetten, diep in de
 * systeeminstellingen, op een plek waar niets uitlegt waarom hij daar is.
 *
 * Dus doen we het hier: per toestemming één regel, wat hij doet, waar je hem
 * vindt, en of hij al aan staat. De stand wordt opnieuw gemeten zodra je uit de
 * instellingen terugkomt.
 */
@Composable
fun OnboardingScreen(
    onKlaar: () -> Unit,
    onOverslaan: () -> Unit
) {
    val context = LocalContext.current

    // Bij terugkeer uit de systeeminstellingen opnieuw meten. Zonder deze teller
    // blijft er "Uit" staan terwijl je hem net hebt aangezet.
    var meting by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) meting++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // De prominente kennisgeving staat vóór de toestemming. Pas na 'Ik ga
    // akkoord' gaat de gebruiker door naar de Android-instelling.
    var toonKennisgeving by remember { mutableStateOf(false) }

    val toegankelijkheid = remember(meting) { ShieldService.isAccessibilityEnabled(context) }
    val overlay = remember(meting) { ShieldService.canDrawOverlays(context) }
    val wekkers = remember(meting) { ShieldService.canScheduleExactAlarms(context) }
    val meldingen = remember(meting) { NotificationService.hasPermission(context) }

    val klaar = toegankelijkheid && overlay

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(48.dp))

        Text(
            text = "Totem klaarzetten",
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Android laat een app niet zelf om deze rechten vragen — je zet ze " +
                "één keer met de hand aan. Daarna heb je er geen omkijken meer naar.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(Modifier.height(28.dp))

        Stap(
            nummer = 1,
            titel = "Toegankelijkheid",
            aan = toegankelijkheid,
            verplicht = true,
            // Dit is de 'prominente kennisgeving' die Google Play verlangt bij
            // gebruik van de AccessibilityServices API. Drie dingen moeten
            // erin staan: dat we die dienst gebruiken, wat hij uitleest, en
            // waarvoor. De laatste zin gaat over verzamelen en delen -- daar
            // vraagt de beoordeling apart naar.
            waarom = "Totem gebruikt de Toegankelijkheidsservice van Android om te zien " +
                "welke app je op de voorgrond opent. Alleen zo kan Totem een " +
                "geblokkeerde app herkennen en het blokkadescherm tonen. Dit gebeurt " +
                "volledig op je eigen toestel: er wordt niets opgeslagen en niets " +
                "verstuurd naar ons of naar derden.",
            waar = "Op de meeste toestellen springt de lijst meteen naar Totem. Zo " +
                "niet, zoek hem dan onder ‘Geïnstalleerde apps’ of ‘Gedownloade " +
                "apps’. Tik erop en zet de schakelaar aan.",
            knop = "Toegankelijkheid openen",
            onClick = {
                // Al eerder akkoord gegaan? Dan hoeft de kennisgeving niet opnieuw.
                if (SharedStore.accessibilityConsent) {
                    ShieldService.openAccessibilitySettings(context)
                } else {
                    toonKennisgeving = true
                }
            }
        )

        // Deze valkuil kost anders een half uur zoeken.
        if (!toegankelijkheid) {
            Waarschuwing(
                "Staat de schakelaar grijs, of verdwijnt hij meteen weer? Dan houdt " +
                    "Android hem tegen omdat Totem niet uit de Play Store komt. Ga naar " +
                    "Instellingen → Apps → Totem → de drie puntjes rechtsboven → " +
                    "‘Beperkte instellingen toestaan’, en probeer het dan opnieuw."
            )
        }

        Stap(
            nummer = 2,
            titel = "Over andere apps tekenen",
            aan = overlay,
            verplicht = true,
            waarom = "Hiermee mag Totem zijn blokkadescherm tonen bovenop de app die " +
                "je net opende.",
            waar = "Zet de schakelaar bij Totem aan.",
            knop = "Instelling openen",
            onClick = { ShieldService.openOverlaySettings(context) }
        )

        Stap(
            nummer = 3,
            titel = "Wekkers en herinneringen",
            aan = wekkers,
            verplicht = false,
            waarom = "Nodig als je schema's gebruikt: hiermee begint een blokkade op de " +
                "minuut in plaats van soms uren later.",
            waar = "Zet ‘Alarmen en herinneringen toestaan’ aan.",
            knop = "Instelling openen",
            onClick = { ShieldService.openExactAlarmSettings(context) }
        )

        Stap(
            nummer = 4,
            titel = "Batterij zonder beperkingen",
            aan = null,
            verplicht = false,
            waarom = "Voorkomt dat je toestel Totem op stil zet en een schema mist. " +
                "Vooral bij Xiaomi, Oppo en Samsung is dit het verschil.",
            waar = "Zoek Totem op en kies ‘Niet optimaliseren’ of ‘Onbeperkt’.",
            knop = "Instelling openen",
            onClick = { ShieldService.openBatterySettings(context) }
        )

        if (!meldingen) {
            Stap(
                nummer = 5,
                titel = "Meldingen",
                aan = false,
                verplicht = false,
                waarom = "Zodat je bericht krijgt als een schema begint, en de teller " +
                    "tijdens een blokkade zichtbaar blijft.",
                waar = "Deze vraagt de app zelf; open je de app opnieuw, dan komt het " +
                    "venster vanzelf.",
                knop = null,
                onClick = {}
            )
        }

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = onKlaar,
            enabled = klaar,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (klaar) "Aan de slag" else "Zet eerst stap 1 en 2 aan")
        }

        TextButton(
            onClick = onOverslaan,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        ) {
            Text("Later doen", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Text(
            text = "Je kunt dit later altijd terugvinden onder Instellingen.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 40.dp)
        )
    }

    if (toonKennisgeving) {
        ToegankelijkheidKennisgeving(
            onAkkoord = {
                toonKennisgeving = false
                ShieldService.openAccessibilitySettings(context)
            },
            onNietAkkoord = { toonKennisgeving = false }
        )
    }
}

/**
 * Eén toestemming.
 *
 * @param aan null als we niet kunnen zien of hij aan staat — bij de
 *            batterij-instelling geeft Android daar geen antwoord op.
 */
@Composable
private fun Stap(
    nummer: Int,
    titel: String,
    aan: Boolean?,
    verplicht: Boolean,
    waarom: String,
    waar: String,
    knop: String?,
    onClick: () -> Unit
) {
    Row(modifier = Modifier.padding(bottom = 22.dp)) {
        // Het bolletje: een vinkje als het geregeld is, anders het stapnummer.
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    if (aan == true) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            if (aan == true) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(17.dp)
                )
            } else {
                Text(
                    "$nummer",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(modifier = Modifier.padding(start = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    titel,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (!verplicht) {
                    Text(
                        "  optioneel",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                waarom,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp)
            )

            if (aan != true) {
                Text(
                    waar,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
                if (knop != null) {
                    TextButton(
                        onClick = onClick,
                        modifier = Modifier.padding(top = 2.dp)
                    ) { Text(knop) }
                }
            }
        }
    }
}

@Composable
private fun Waarschuwing(tekst: String) {
    Text(
        text = tekst,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 22.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(14.dp)
    )
}

/** Of het toestemmingsscherm nog getoond moet worden. */
fun heeftOnboardingNodig(context: Context): Boolean = !ShieldService.isReady(context)
