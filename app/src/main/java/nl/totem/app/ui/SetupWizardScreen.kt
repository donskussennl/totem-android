package nl.totem.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.totem.app.R
import nl.totem.app.model.FocusMode
import nl.totem.app.model.Schedule
import nl.totem.app.model.ScheduleEnd
import nl.totem.app.model.ScheduleTrigger
import nl.totem.app.store.AppStore

/**
 * Neemt je stap voor stap mee bij het instellen van een modus. Wat er
 * gevraagd wordt hangt af van waarvoor je hem maakt: bij Werk en Studeren tijd
 * of locatie, bij Sport alleen locatie, bij Relaxen niets. Er wordt pas iets
 * opgeslagen bij de laatste stap.
 *
 * @param isFirstRun na het koppelen ("Overslaan"), of via "Modus toevoegen"
 *                   ("Annuleer").
 */
@Composable
fun SetupWizardScreen(store: AppStore, isFirstRun: Boolean, onClose: () -> Unit) {
    val context = LocalContext.current
    var stap by remember { mutableStateOf(Stap.DOEL) }
    var doel by remember { mutableStateOf<Doel?>(null) }
    var eigenNaam by remember { mutableStateOf("") }
    var concept by remember { mutableStateOf(FocusMode(name = "", symbol = "circle")) }
    var automatisch by remember { mutableStateOf(Automatisch.GEEN) }
    var appKiezer by remember { mutableStateOf(false) }
    var plekKiezer by remember { mutableStateOf(false) }
    var tijdKiezer by remember { mutableStateOf<Boolean?>(null) }  // true = begin, false = eind

    val stappen = if (doel == Doel.RELAX) listOf(Stap.DOEL, Stap.APPS, Stap.KLAAR)
    else listOf(Stap.DOEL, Stap.APPS, Stap.AUTOMATISCH, Stap.KLAAR)
    val index = stappen.indexOf(stap).coerceAtLeast(0)

    fun terug() { if (index > 0) stap = stappen[index - 1] }
    fun verder() { if (index + 1 < stappen.size) stap = stappen[index + 1] }

    /** Naam, icoon en standaardinstellingen voor dit doel. */
    fun bereidVoor() {
        val d = doel ?: return
        concept = concept.copy(symbol = d.symbool, name = if (d == Doel.ANDERS) eigenNaam.trim() else context.getString(d.naam))
        when (d) {
            Doel.WERK -> {
                concept = concept.copy(schedule = concept.schedule.copy(
                    startHour = 9, startMinute = 0, end = ScheduleEnd.Time(17, 0), weekdays = setOf(2, 3, 4, 5, 6)))
                automatisch = Automatisch.TIJD
            }
            Doel.STUDEREN -> {
                concept = concept.copy(schedule = concept.schedule.copy(
                    startHour = 19, startMinute = 0, end = ScheduleEnd.Time(21, 0), weekdays = setOf(2, 3, 4, 5, 6)))
                automatisch = Automatisch.TIJD
            }
            Doel.SPORT -> {
                concept = concept.copy(schedule = concept.schedule.copy(weekdays = (1..7).toSet()))
                automatisch = Automatisch.LOCATIE
            }
            Doel.RELAX, Doel.ANDERS -> automatisch = Automatisch.GEEN
        }
    }

    /** De keuze vertalen naar het schema van de modus. */
    fun pasAutomatischToe() {
        concept = concept.copy(schedule = when (automatisch) {
            Automatisch.GEEN -> concept.schedule.copy(isOn = false)
            Automatisch.TIJD -> concept.schedule.copy(
                isOn = true, trigger = ScheduleTrigger.TIME,
                end = concept.schedule.end.takeIf { it !is ScheduleEnd.Leave } ?: ScheduleEnd.Time(17, 0))
            Automatisch.LOCATIE -> concept.schedule.copy(
                isOn = true, trigger = ScheduleTrigger.LOCATION, end = ScheduleEnd.Leave)
        })
    }

    val kanVerder = when (stap) {
        Stap.DOEL -> doel != null && (doel != Doel.ANDERS || eigenNaam.isNotBlank())
        Stap.APPS -> concept.isConfigured
        Stap.AUTOMATISCH -> automatisch != Automatisch.LOCATIE || concept.schedule.place != null
        Stap.KLAAR -> true
    }

    BackHandler(enabled = index > 0) { terug() }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            // Kop: terug, overslaan, voortgang.
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(40.dp)) {
                    if (stap != Stap.DOEL && stap != Stap.KLAAR) {
                        IconButton(onClick = { terug() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    if (stap != Stap.KLAAR) {
                        TextButton(onClick = {
                            if (isFirstRun) store.finishSetup(null)
                            onClose()
                        }) {
                            Text(
                                stringResource(if (isFirstRun) R.string.wiz_skip else R.string.cancel),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                LinearProgressIndicator(
                    progress = { (index + 1f) / stappen.size },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }

            AnimatedContent(
                targetState = stap,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "wizard",
                modifier = Modifier.weight(1f)
            ) { huidige ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (huidige) {
                        Stap.DOEL -> {
                            Titel(
                                stringResource(if (isFirstRun) R.string.wiz_purpose_first else R.string.wiz_purpose_new),
                                stringResource(if (isFirstRun) R.string.wiz_purpose_first_sub else R.string.wiz_purpose_new_sub)
                            )
                            listOf(Doel.WERK to Doel.SPORT, Doel.RELAX to Doel.STUDEREN).forEach { (a, b) ->
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    listOf(a, b).forEach { d ->
                                        DoelTegel(d, gekozen = doel == d, modifier = Modifier.weight(1f)) { doel = d }
                                    }
                                }
                            }
                            KeuzeKaart(
                                icoon = Icons.Filled.AutoAwesome,
                                titel = stringResource(R.string.mode_other),
                                uitleg = stringResource(R.string.wiz_other_sub),
                                gekozen = doel == Doel.ANDERS
                            ) { doel = Doel.ANDERS }
                            if (doel == Doel.ANDERS) {
                                OutlinedTextField(
                                    value = eigenNaam,
                                    onValueChange = { eigenNaam = it },
                                    label = { Text(stringResource(R.string.wiz_name_label)) },
                                    placeholder = { Text(stringResource(R.string.wiz_name_hint)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Stap.APPS -> {
                            Titel(
                                stringResource(R.string.wiz_apps_title, concept.name),
                                stringResource(R.string.wiz_apps_sub, concept.name)
                            )
                            Tip(Icons.Filled.TouchApp, stringResource(R.string.wiz_apps_tip))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                    .clickable { appKiezer = true }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Apps, contentDescription = null)
                                Text(
                                    if (concept.isConfigured) stringResource(R.string.wiz_apps_chosen, concept.blockedCount)
                                    else stringResource(R.string.wiz_apps_choose),
                                    modifier = Modifier.weight(1f).padding(start = 12.dp)
                                )
                                Icon(Icons.Filled.ChevronRight, contentDescription = null)
                            }
                        }

                        Stap.AUTOMATISCH -> {
                            if (doel == Doel.SPORT) {
                                Titel(stringResource(R.string.wiz_sport_title), stringResource(R.string.wiz_sport_sub))
                                KeuzeKaart(Icons.Filled.Place, stringResource(R.string.wiz_sport_yes), stringResource(R.string.wiz_sport_yes_sub),
                                    automatisch == Automatisch.LOCATIE) { automatisch = Automatisch.LOCATIE }
                                KeuzeKaart(Icons.Filled.TouchApp, stringResource(R.string.wiz_sport_no), stringResource(R.string.wiz_sport_no_sub),
                                    automatisch == Automatisch.GEEN) { automatisch = Automatisch.GEEN }
                            } else {
                                Titel(stringResource(R.string.wiz_when_title, concept.name), stringResource(R.string.wiz_when_sub))
                                KeuzeKaart(Icons.Filled.Schedule, stringResource(R.string.wiz_opt_time), stringResource(R.string.wiz_opt_time_sub),
                                    automatisch == Automatisch.TIJD) { automatisch = Automatisch.TIJD }
                                KeuzeKaart(Icons.Filled.Place, stringResource(R.string.wiz_opt_location), stringResource(
                                    when (doel) {
                                        Doel.WERK -> R.string.wiz_opt_location_work
                                        Doel.STUDEREN -> R.string.wiz_opt_location_study
                                        else -> R.string.wiz_opt_location_other
                                    }), automatisch == Automatisch.LOCATIE) { automatisch = Automatisch.LOCATIE }
                                KeuzeKaart(Icons.Filled.TouchApp, stringResource(R.string.wiz_opt_none), stringResource(R.string.wiz_opt_none_sub),
                                    automatisch == Automatisch.GEEN) { automatisch = Automatisch.GEEN }
                            }

                            if (automatisch == Automatisch.TIJD) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    TijdVakje(stringResource(R.string.wiz_from),
                                        Texts.clock(context, concept.schedule.startHour, concept.schedule.startMinute),
                                        Modifier.weight(1f)) { tijdKiezer = true }
                                    val eind = concept.schedule.end as? ScheduleEnd.Time ?: ScheduleEnd.Time(17, 0)
                                    TijdVakje(stringResource(R.string.wiz_to), Texts.clock(context, eind.hour, eind.minute),
                                        Modifier.weight(1f)) { tijdKiezer = false }
                                }
                                WeekdayPicker(
                                    selection = concept.schedule.weekdays,
                                    onChange = { concept = concept.copy(schedule = concept.schedule.copy(weekdays = it)) }
                                )
                            }
                            if (automatisch == Automatisch.LOCATIE) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                        .clickable { plekKiezer = true }
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Text(
                                        concept.schedule.place?.name ?: stringResource(R.string.loc_choose),
                                        color = if (concept.schedule.place == null) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f).padding(start = 10.dp)
                                    )
                                    Icon(Icons.Filled.ChevronRight, contentDescription = null)
                                }
                            }
                        }

                        Stap.KLAAR -> {
                            Titel(stringResource(R.string.wiz_done_title, concept.name), stringResource(R.string.wiz_done_sub))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(TotemIcons.vector(concept.symbol), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                                Column(modifier = Modifier.padding(start = 14.dp)) {
                                    Text(concept.name, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                                    Text(Texts.modeSummary(context, concept), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    scheduleSummary(concept.schedule)?.let {
                                        Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                            if (concept.schedule.isLocationBased) {
                                val toegang = rememberLocationAccess()
                                if (!toegang.background) LocationPermissionCard(toegang)
                            }
                            Tip(Icons.Filled.Lightbulb, stringResource(R.string.tip_emergency))
                        }
                    }
                }
            }

            // Onderin: één grote knop.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .height(56.dp)
                    .clip(CircleShape)
                    .background(if (kanVerder) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f))
                    .clickable(enabled = kanVerder) {
                        when (stap) {
                            Stap.DOEL -> { bereidVoor(); verder() }
                            Stap.AUTOMATISCH -> { pasAutomatischToe(); verder() }
                            Stap.KLAAR -> {
                                if (isFirstRun) store.finishSetup(concept)
                                else store.addMode(concept)
                                onClose()
                            }
                            else -> verder()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(if (stap == Stap.KLAAR) R.string.wiz_finish else R.string.wiz_next),
                    color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 17.sp
                )
            }
        }
    }

    if (appKiezer) {
        AppPickerSheet(
            geselecteerd = concept.blockedPackages,
            onDismiss = { appKiezer = false },
            onGereed = {
                concept = concept.copy(blockedPackages = it)
                appKiezer = false
            }
        )
    }
    if (plekKiezer) {
        PlacePickerSheet(
            initial = concept.schedule.place,
            onDismiss = { plekKiezer = false },
            onChoose = {
                concept = concept.copy(schedule = concept.schedule.copy(place = it))
                plekKiezer = false
            }
        )
    }
    tijdKiezer?.let { begin ->
        val eind = concept.schedule.end as? ScheduleEnd.Time ?: ScheduleEnd.Time(17, 0)
        TijdKiezerDialoog(
            beginUur = if (begin) concept.schedule.startHour else eind.hour,
            beginMinuut = if (begin) concept.schedule.startMinute else eind.minute,
            onDismiss = { tijdKiezer = null },
            onGereed = { uur, minuut ->
                concept = concept.copy(schedule = if (begin)
                    concept.schedule.copy(startHour = uur, startMinute = minuut)
                else concept.schedule.copy(end = ScheduleEnd.Time(uur, minuut)))
                tijdKiezer = null
            }
        )
    }
}

private enum class Stap { DOEL, APPS, AUTOMATISCH, KLAAR }

private enum class Automatisch { TIJD, LOCATIE, GEEN }

private enum class Doel(val naam: Int, val uitleg: Int, val symbool: String, val icoon: ImageVector) {
    WERK(R.string.mode_work, R.string.wiz_work_sub, "laptop", Icons.Filled.LaptopMac),
    SPORT(R.string.mode_sport, R.string.wiz_sport_sub_tile, "run", Icons.AutoMirrored.Filled.DirectionsRun),
    RELAX(R.string.mode_relax, R.string.wiz_relax_sub, "leaf", Icons.Filled.LocalFlorist),
    STUDEREN(R.string.mode_study, R.string.wiz_study_sub, "book", Icons.Filled.Book),
    ANDERS(R.string.mode_other, R.string.wiz_other_sub, "circle", Icons.Filled.AutoAwesome)
}

@Composable
private fun Titel(tekst: String, uitleg: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(tekst, fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp)
        Text(uitleg, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DoelTegel(doel: Doel, gekozen: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier
            .heightIn(min = 140.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .border(BorderStroke(2.dp, if (gekozen) accent else Color.Transparent), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                .background(if (gekozen) accent else accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(doel.icoon, contentDescription = null, tint = if (gekozen) Color.White else accent)
        }
        Text(stringResource(doel.naam), fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
        Text(stringResource(doel.uitleg), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun KeuzeKaart(icoon: ImageVector, titel: String, uitleg: String, gekozen: Boolean, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .border(BorderStroke(2.dp, if (gekozen) accent else Color.Transparent), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icoon, contentDescription = null, tint = accent)
        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
            Text(titel, fontWeight = FontWeight.Medium)
            Text(uitleg, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            if (gekozen) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (gekozen) accent else Color.Gray.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun Tip(icoon: ImageVector, tekst: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icoon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(tekst, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TijdVakje(label: String, waarde: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(waarde, fontSize = 18.sp, fontWeight = FontWeight.Medium)
    }
}
