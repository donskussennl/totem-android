package nl.totem.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import nl.totem.app.model.Schedule
import nl.totem.app.R
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.totem.app.model.FocusMode
import nl.totem.app.model.ScheduleEnd
import nl.totem.app.store.AppStore

/**
 * Naam, icoon, geblokkeerde apps en het schema van één modus.
 *
 * Op iOS was dit een Form in een NavigationStack; hier een bottom sheet, want
 * dat is op Android het gebruikelijke gebaar voor "even iets aanpassen".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeEditorSheet(
    store: AppStore,
    mode: FocusMode,
    onDismiss: () -> Unit,
    /** Een nieuwe modus: pas bij "Bewaar" toevoegen, en geen verwijderknop. */
    isNew: Boolean = false
) {
    var concept by remember(mode.id) { mutableStateOf(mode) }
    var kiezerOpen by remember { mutableStateOf(false) }
    var tijdKiezer by remember { mutableStateOf<TijdSoort?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text("Modus", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)

            OutlinedTextField(
                value = concept.name,
                onValueChange = { concept = concept.copy(name = it) },
                label = { Text("Naam") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )

            Kop("Icoon")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TotemIcons.all.take(4).forEach { symbool ->
                    IcoonVak(symbool, concept.symbol == symbool) {
                        concept = concept.copy(symbol = symbool)
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TotemIcons.all.drop(4).forEach { symbool ->
                    IcoonVak(symbool, concept.symbol == symbool) {
                        concept = concept.copy(symbol = symbool)
                    }
                }
            }

            Kop("Blokkeren")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { kiezerOpen = true }
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Apps, contentDescription = null)
                Text(
                    "Apps om te blokkeren",
                    modifier = Modifier.padding(start = 12.dp).weight(1f)
                )
                Text(
                    "${concept.blockedCount}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "Deze apps zijn niet bereikbaar zolang deze modus actief is.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // MARK: - Schema
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Schema", fontWeight = FontWeight.Medium)
                    Text(
                        "Laat deze modus vanzelf beginnen op vaste tijden.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = concept.schedule.isOn,
                    onCheckedChange = {
                        concept = concept.copy(schedule = concept.schedule.copy(isOn = it))
                    }
                )
            }

            if (concept.schedule.isOn) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TijdVak(
                        label = "Van",
                        waarde = "%02d:%02d".format(
                            concept.schedule.startHour,
                            concept.schedule.startMinute
                        ),
                        modifier = Modifier.weight(1f)
                    ) { tijdKiezer = TijdSoort.START }

                    val eind = concept.schedule.end
                    TijdVak(
                        label = "Tot",
                        waarde = when (eind) {
                            is ScheduleEnd.Time -> "%02d:%02d".format(eind.hour, eind.minute)
                            is ScheduleEnd.Tap, is ScheduleEnd.Leave -> "Als je tikt"
                        },
                        modifier = Modifier.weight(1f)
                    ) { tijdKiezer = TijdSoort.EIND }
                }

                TextButton(
                    onClick = {
                        concept = concept.copy(
                            schedule = concept.schedule.copy(
                                end = if (concept.schedule.end is ScheduleEnd.Tap)
                                    ScheduleEnd.Time(17, 0) else ScheduleEnd.Tap
                            )
                        )
                    },
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        if (concept.schedule.end is ScheduleEnd.Tap)
                            "Toch een vaste eindtijd" else "Laat lopen tot ik tik"
                    )
                }

                Kop("Dagen")
                WeekdayPicker(
                    selection = concept.schedule.weekdays,
                    onChange = {
                        concept = concept.copy(schedule = concept.schedule.copy(weekdays = it))
                    }
                )

                // Alleen bij een schema dat zelf eindigt: dan ontdooit een tik
                // de apps even in plaats van het schema te stoppen.
                if (concept.schedule.pausesOnTap) {
                    Kop(stringResource(R.string.editor_unfreeze_on_tap))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        Schedule.PAUSE_OPTIONS.forEachIndexed { i, minuten ->
                            SegmentedButton(
                                selected = concept.schedule.pauseMinutes == minuten,
                                onClick = {
                                    concept = concept.copy(
                                        schedule = concept.schedule.copy(pauseMinutes = minuten)
                                    )
                                },
                                shape = SegmentedButtonDefaults.itemShape(i, Schedule.PAUSE_OPTIONS.size)
                            ) { Text(stringResource(R.string.editor_unfreeze_minutes, minuten)) }
                        }
                    }
                    Text(
                        stringResource(R.string.editor_unfreeze_help),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!isNew) TextButton(
                    onClick = {
                        store.deleteMode(concept.id)
                        onDismiss()
                    }
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        "Verwijder",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }

                Box(modifier = Modifier.weight(1f))

                TextButton(onClick = onDismiss) { Text("Annuleer") }

                Button(
                    onClick = {
                        if (isNew) store.addMode(concept) else store.update(concept)
                        onDismiss()
                    },
                    enabled = concept.name.isNotBlank()
                ) { Text("Bewaar") }
            }
        }
    }

    if (kiezerOpen) {
        AppPickerSheet(
            geselecteerd = concept.blockedPackages,
            onDismiss = { kiezerOpen = false },
            onGereed = {
                concept = concept.copy(blockedPackages = it)
                kiezerOpen = false
            }
        )
    }

    tijdKiezer?.let { soort ->
        val huidigUur = if (soort == TijdSoort.START) concept.schedule.startHour
        else (concept.schedule.end as? ScheduleEnd.Time)?.hour ?: 17
        val huidigeMinuut = if (soort == TijdSoort.START) concept.schedule.startMinute
        else (concept.schedule.end as? ScheduleEnd.Time)?.minute ?: 0

        TijdKiezerDialoog(
            beginUur = huidigUur,
            beginMinuut = huidigeMinuut,
            onDismiss = { tijdKiezer = null },
            onGereed = { uur, minuut ->
                concept = if (soort == TijdSoort.START) {
                    concept.copy(
                        schedule = concept.schedule.copy(startHour = uur, startMinute = minuut)
                    )
                } else {
                    concept.copy(schedule = concept.schedule.copy(end = ScheduleEnd.Time(uur, minuut)))
                }
                tijdKiezer = null
            }
        )
    }
}

private enum class TijdSoort { START, EIND }

@Composable
private fun Kop(tekst: String) {
    Text(
        text = tekst,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.IcoonVak(
    symbool: String,
    gekozen: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(
                if (gekozen) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = TotemIcons.vector(symbool),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun TijdVak(
    label: String,
    waarde: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(waarde, fontSize = 18.sp, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TijdKiezerDialoog(
    beginUur: Int,
    beginMinuut: Int,
    onDismiss: () -> Unit,
    onGereed: (Int, Int) -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = beginUur,
        initialMinute = beginMinuut,
        is24Hour = true
    )

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onGereed(state.hour, state.minute) }) { Text("Gereed") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuleer") }
        },
        text = { TimePicker(state = state) }
    )
}
