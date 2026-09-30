package nl.totem.app.ui

import nl.totem.app.R
import androidx.compose.ui.res.stringResource
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

    // Ook hier eerst de prominente kennisgeving; dit is de tweede plek waar
    // de gebruiker naar de Toegankelijkheidsinstelling kan doorlopen.
    var toonKennisgeving by remember { mutableStateOf(false) }

    // De toestemmingen worden buiten de app omgezet. Zonder deze teller blijft
    // hier 'Aan' staan terwijl de gebruiker de schakelaar net heeft uitgezet:
    // Compose ziet geen reden om opnieuw te lezen. De teller loopt op zodra dit
    // scherm weer op de voorgrond komt, en dwingt zo een nieuwe meting af.
    val meting = terugkeerTeller()
    val toegankelijkheid = remember(meting) { ShieldService.isAccessibilityEnabled(context) }
    val overlay = remember(meting) { ShieldService.canDrawOverlays(context) }
    val exacteWekkers = remember(meting) { ShieldService.canScheduleExactAlarms(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
            Kop(stringResource(R.string.set_permissions))

            ToestemmingRij(
                titel = stringResource(R.string.onb_a11y_title),
                uitleg = stringResource(R.string.set_a11y_help),
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
                titel = stringResource(R.string.onb_overlay_title),
                uitleg = stringResource(R.string.set_overlay_help),
                aan = overlay,
                onClick = { ShieldService.openOverlaySettings(context) }
            )
            ToestemmingRij(
                titel = stringResource(R.string.onb_alarms_title),
                uitleg = stringResource(R.string.set_alarms_help),
                aan = exacteWekkers,
                onClick = { ShieldService.openExactAlarmSettings(context) }
            )
            ToestemmingRij(
                titel = stringResource(R.string.onb_battery_title),
                uitleg = stringResource(R.string.set_battery_help),
                aan = null,
                onClick = { ShieldService.openBatterySettings(context) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            Kop(stringResource(R.string.set_your_totem))
            totem?.let {
                InfoRij(stringResource(R.string.set_name), it.name)
                InfoRij(stringResource(R.string.set_tag), it.tagUID)
                InfoRij(stringResource(R.string.set_paired), datum(LocalContext.current, it.pairedAt))
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            Kop(stringResource(R.string.onb_notif_title))
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.set_notify_schedule))
                    Text(
                        stringResource(R.string.set_notify_schedule_help),
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

            Kop(stringResource(R.string.set_strict))
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.set_pin_required))
                    Text(
                        stringResource(R.string.set_strict_help),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val pinUitTekst = stringResource(R.string.pin_strict_off)
                Switch(
                    checked = strict,
                    onCheckedChange = { aan ->
                        pincodeScherm = if (aan) {
                            if (PinService.isSet) null else PinPurpose.Create
                        } else {
                            PinPurpose.Verify(pinUitTekst)
                        }
                        if (aan && PinService.isSet) store.setStrictMode(true)
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            Kop(stringResource(R.string.set_emergency))
            InfoRij(
                stringResource(R.string.set_emergency_left),
                stringResource(R.string.set_x_of_y, AppStore.EMERGENCY_LIMIT - emergencyUsed, AppStore.EMERGENCY_LIMIT)
            )
            Text(
                stringResource(R.string.set_emergency_help, AppStore.EMERGENCY_LIMIT),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            TextButton(onClick = onOpenStats) { Text(stringResource(R.string.set_view_stats)) }

            TextButton(onClick = { bevestigOntkoppel = true }) {
                Text(stringResource(R.string.set_unpair), color = MaterialTheme.colorScheme.error)
            }
            Text(
                stringResource(R.string.set_unpair_help),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            Kop(stringResource(R.string.set_about))
            InfoRij(
                stringResource(R.string.set_version),
                LocalContext.current.let { c ->
                    runCatching { c.packageManager.getPackageInfo(c.packageName, 0).versionName }.getOrNull() ?: ""
                }
            )
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
            title = { Text(stringResource(R.string.set_unpair_q)) },
            confirmButton = {
                TextButton(onClick = {
                    bevestigOntkoppel = false
                    store.unpairTotem()
                    onBack()
                }) { Text(stringResource(R.string.set_unpair_confirm), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { bevestigOntkoppel = false }) { Text(stringResource(R.string.cancel)) }
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
                true -> stringResource(R.string.set_on)
                false -> stringResource(R.string.set_off)
                null -> stringResource(R.string.set_open)
            },
            color = when (aan) {
                true -> MaterialTheme.colorScheme.primary
                false -> MaterialTheme.colorScheme.error
                null -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

private fun datum(context: android.content.Context, millis: Long): String =
    android.text.format.DateFormat.getMediumDateFormat(context).format(Date(millis)) + ", " +
        android.text.format.DateFormat.getTimeFormat(context).format(Date(millis))
