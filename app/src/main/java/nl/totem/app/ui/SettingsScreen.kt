package nl.totem.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.totem.app.data.PinService
import nl.totem.app.notify.NotificationService
import nl.totem.app.shield.ShieldService
import nl.totem.app.store.AppStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Apparaatinfo, toestemmingen, strikte modus en ontkoppelen.
 *
 * Het grootste verschil met iOS staat bovenaan: daar was één schakelaar voor
 * Schermtijd genoeg, hier zijn het vier losse toestemmingen die de gebruiker
 * zelf in de systeeminstellingen moet omzetten. Vandaar dat ze hier met uitleg
 * en hun stand erbij staan in plaats van verstopt onder één knop.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    store: AppStore,
    onBack: () -> Unit,
    onOpenStats: () -> Unit
) {
    val context = LocalContext.current
    val totem by store.totem.collectAsStateWithLifecycle()
    val strict by store.strictMode.collectAsStateWithLifecycle()
    val emergencyUsed by store.emergencyUsed.collectAsStateWithLifecycle()

    var bevestigOntkoppel by remember { mutableStateOf(false) }
    var pincodeScherm by remember { mutableStateOf<PinPurpose?>(null) }
    var meldingen by remember { mutableStateOf(NotificationService.isEnabled) }

    // Bij elke keer openen opnieuw kijken; de gebruiker kan net terug zijn uit
    // de systeeminstellingen.
    // Ook hier eerst de prominente kennisgeving; dit is de tweede plek waar
    // de gebruiker naar de Toegankelijkheidsinstelling kan doorlopen.
    var toonKennisgeving by remember { mutableStateOf(false) }

    val toegankelijkheid = ShieldService.isAccessibilityEnabled(context)
    val overlay = ShieldService.canDrawOverlays(context)
    val exacteWekkers = ShieldService.canScheduleExactAlarms(context)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Instellingen") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Terug")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Kop("Toestemmingen")

            ToestemmingRij(
                titel = "Toegankelijkheid",
                uitleg = "Nodig om te zien welke app je opent. Zonder dit kan Totem niets blokkeren.",
                aan = toegankelijkheid,
                onClick = {
                    // Staat de dienst uit, dan wordt er opnieuw toestemming
                    // gevraagd -- uitzetten is intrekken.
                    if (toegankelijkheid) {
                        ShieldService.openAccessibilitySettings(context)
                    } else {
                        toonKennisgeving = true
                    }
                }
            )
            ToestemmingRij(
                titel = "Over andere apps tekenen",
                uitleg = "Nodig om het blokkadescherm te tonen bovenop de app die je opende.",
                aan = overlay,
                onClick = { ShieldService.openOverlaySettings(context) }
            )
            ToestemmingRij(
                titel = "Wekkers en herinneringen",
                uitleg = "Nodig om een schema op de minuut te laten beginnen. Zonder dit kan een blokkade uren te laat aanslaan.",
                aan = exacteWekkers,
                onClick = { ShieldService.openExactAlarmSettings(context) }
            )
            ToestemmingRij(
                titel = "Batterij zonder beperkingen",
                uitleg = "Voorkomt dat je toestel Totem stilzet en een schema mist.",
                aan = null,
                onClick = { ShieldService.openBatterySettings(context) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            Kop("Jouw Totem")
            totem?.let {
                InfoRij("Naam", it.name)
                InfoRij("Tag", it.tagUID)
                InfoRij("Gekoppeld", datum(it.pairedAt))
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            Kop("Meldingen")
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Bericht bij een schema")
                    Text(
                        "Je hoort het als een geplande blokkade start of stopt.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = meldingen,
                    onCheckedChange = {
                        meldingen = it
                        NotificationService.isEnabled = it
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            Kop("Strikte modus")
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Pincode vereist")
                    Text(
                        "Dan vraagt ook de noodontgrendeling om je pincode, en zet je de strikte modus niet zonder uit.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = strict,
                    onCheckedChange = { aan ->
                        pincodeScherm = if (aan) {
                            if (PinService.isSet) null else PinPurpose.Create
                        } else {
                            PinPurpose.Verify("Voer je pincode in om de strikte modus uit te zetten.")
                        }
                        if (aan && PinService.isSet) store.setStrictMode(true)
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            Kop("Noodgeval")
            InfoRij(
                "Noodontgrendelingen over",
                "${AppStore.EMERGENCY_LIMIT - emergencyUsed} van ${AppStore.EMERGENCY_LIMIT}"
            )
            Text(
                "Ben je je Totem kwijt terwijl een blokkade loopt? Tik dan tijdens de " +
                    "blokkade drie keer op de Totem-afbeelding om te ontgrendelen. Op de " +
                    "eerste van de maand krijg je er weer ${AppStore.EMERGENCY_LIMIT}.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            TextButton(onClick = onOpenStats) { Text("Statistieken bekijken") }

            TextButton(onClick = { bevestigOntkoppel = true }) {
                Text("Totem ontkoppelen", color = MaterialTheme.colorScheme.error)
            }
            Text(
                "Je kunt daarna een andere Totem koppelen. Een lopende sessie wordt beëindigd.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            Kop("Over")
            InfoRij("Versie", "1.0")
            Text(
                "Tap back to reality.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 40.dp)
            )
        }
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

    if (bevestigOntkoppel) {
        AlertDialog(
            onDismissRequest = { bevestigOntkoppel = false },
            title = { Text("Totem ontkoppelen?") },
            confirmButton = {
                TextButton(onClick = {
                    bevestigOntkoppel = false
                    store.unpairTotem()
                    onBack()
                }) { Text("Ontkoppelen", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { bevestigOntkoppel = false }) { Text("Annuleer") }
            }
        )
    }

    pincodeScherm?.let { doel ->
        PinDialog(
            purpose = doel,
            onDismiss = { pincodeScherm = null },
            onSuccess = {
                when (doel) {
                    is PinPurpose.Create -> store.setStrictMode(true)
                    is PinPurpose.Verify -> store.setStrictMode(false)
                }
                pincodeScherm = null
            }
        )
    }
}

@Composable
private fun Kop(tekst: String) {
    Text(
        text = tekst,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
    )
}

@Composable
private fun InfoRij(label: String, waarde: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Text(waarde, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** @param aan null als we niet kunnen zien of het aan staat. */
@Composable
private fun ToestemmingRij(
    titel: String,
    uitleg: String,
    aan: Boolean?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(titel)
            Text(
                uitleg,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = when (aan) {
                true -> "Aan"
                false -> "Uit"
                null -> "Openen ›"
            },
            color = when (aan) {
                true -> MaterialTheme.colorScheme.primary
                false -> MaterialTheme.colorScheme.error
                null -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

private fun datum(millis: Long): String =
    SimpleDateFormat("d MMM yyyy, HH:mm", Locale("nl")).format(Date(millis))
