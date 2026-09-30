package nl.totem.app.ui

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.totem.app.R
import nl.totem.app.model.Points
import nl.totem.app.model.SessionLog
import nl.totem.app.notify.Reminders
import nl.totem.app.store.AppStore
import java.util.Calendar
import java.util.Date

/**
 * Hoeveel punten je hebt verdiend, en hoe vaak, hoe lang en wanneer Totem
 * actief was, met grafieken. Zelfde opbouw als op iOS.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(store: AppStore, onBack: () -> Unit) {
    val context = LocalContext.current
    val history by store.history.collectAsStateWithLifecycle()
    val modes by store.modes.collectAsStateWithLifecycle()
    var periode by remember { mutableStateOf(Periode.WEEK) }
    val nu = remember { System.currentTimeMillis() }

    val logs = remember(history, periode) {
        val dagen = periode.dagen ?: return@remember history
        val grens = nu - dagen * 24L * 60 * 60 * 1000
        history.filter { it.startedAt >= grens }
    }
    val totaal = logs.sumOf { it.blockedSeconds }
    val gemiddeld = if (logs.isEmpty()) 0L else totaal / logs.size

    val totaalPunten = remember(history, modes) { history.sumOf { Points.forLog(it, modes) } }
    val dezeWeek = remember(history, modes) { Points.week(history, modes, nu) }
    val perWeek = remember(history, modes) { puntenPerWeek(history, modes, nu) }
    val tijd = remember(logs, periode) { tijdPerPeriode(logs, history, periode, nu) }
    val perModus = remember(logs) {
        logs.groupBy { it.modeName }
            .map { (naam, l) -> Triple(naam, l.size, l.sumOf { it.blockedSeconds }) }
            .sortedByDescending { it.third }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stats_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { PuntenKaart(totaalPunten, dezeWeek, stringResource(Reminders.nickname(nu))) }

            item {
                Kaart(stringResource(R.string.stats_points_per_week), stringResource(R.string.stats_points_footer)) {
                    val weekFormaat = DateFormat.getBestDateTimePattern(context.resources.configuration.locales[0], "dMMM")
                    Staven(
                        waarden = perWeek.map { it.second.toFloat() },
                        labels = perWeek.map { android.text.format.DateFormat.format(weekFormaat, Date(it.first)).toString() },
                        bovenschriften = perWeek.map { if (it.second > 0) "${it.second}" else "" },
                        uitgelicht = perWeek.lastIndex
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Periode.entries.forEach { p ->
                        FilterChip(selected = periode == p, onClick = { periode = p }, label = { Text(stringResource(p.label)) })
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Stat("${logs.size}", stringResource(R.string.stats_times_active))
                    Stat(formatDuration(context, totaal), stringResource(R.string.stats_blocked))
                    Stat(formatDuration(context, gemiddeld), stringResource(R.string.stats_average))
                }
            }

            item {
                Kaart(stringResource(if (periode == Periode.ALLES) R.string.stats_time_per_week else R.string.stats_time_per_day)) {
                    if (tijd.all { it.second == 0L }) {
                        Text(stringResource(R.string.stats_no_time), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        val dagLetters = remember { Texts.dayLetters(context) }
                        Staven(
                            waarden = tijd.map { it.second.toFloat() },
                            labels = tijd.mapIndexed { i, (start, _) ->
                                when (periode) {
                                    Periode.WEEK -> dagLetters[Calendar.getInstance().apply { timeInMillis = start }.get(Calendar.DAY_OF_WEEK)] ?: ""
                                    else -> if (i % maxOf(1, tijd.size / 6) == 0)
                                        android.text.format.DateFormat.format(
                                            DateFormat.getBestDateTimePattern(context.resources.configuration.locales[0], "dMMM"),
                                            Date(start)
                                        ).toString() else ""
                                }
                            },
                            bovenschriften = emptyList(),
                            uitgelicht = null
                        )
                    }
                }
            }

            if (perModus.isNotEmpty()) {
                item {
                    Kaart(stringResource(R.string.stats_per_mode)) {
                        val max = perModus.maxOf { it.third }.coerceAtLeast(1)
                        perModus.forEach { (naam, aantal, som) ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(naam, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.7f * som / max + 0.02f)
                                            .height(14.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                    Text(
                                        "$aantal× · ${formatDuration(context, som)}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    stringResource(R.string.stats_recent),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (logs.isEmpty()) {
                item { Text(stringResource(R.string.stats_no_sessions), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(logs.takeLast(15).reversed(), key = { it.id }) { log ->
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row {
                            Text(log.modeName, modifier = Modifier.weight(1f))
                            Text(
                                "+${Points.forLog(log, modes)}",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Row {
                            Text(
                                DateFormat.getMediumDateFormat(context).format(Date(log.startedAt)) + ", " +
                                    DateFormat.getTimeFormat(context).format(Date(log.startedAt)),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Text(formatDuration(context, log.blockedSeconds), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

private enum class Periode(val label: Int, val dagen: Int?) {
    WEEK(R.string.stats_this_week, 7),
    MAAND(R.string.stats_this_month, 30),
    ALLES(R.string.stats_all, null)
}

/** De laatste acht weken, ook weken zonder punten. */
private fun puntenPerWeek(history: List<SessionLog>, modes: List<nl.totem.app.model.FocusMode>, nu: Long): List<Pair<Long, Int>> {
    val huidige = Points.weekStart(nu)
    val week = 7L * 24 * 60 * 60 * 1000
    return (7 downTo 0).map { terug ->
        val start = huidige - terug * week
        start to Points.week(history, modes, start)
    }
}

/**
 * Geblokkeerde seconden per dag (week, maand) of per week (alles). Lege
 * perioden doen mee, anders zie je gaten niet.
 */
private fun tijdPerPeriode(logs: List<SessionLog>, alles: List<SessionLog>, periode: Periode, nu: Long): List<Pair<Long, Long>> {
    val dag = 24L * 60 * 60 * 1000
    return if (periode == Periode.ALLES) {
        val huidige = Points.weekStart(nu)
        val eerste = alles.minOfOrNull { it.startedAt } ?: nu
        val weken = (((huidige - Points.weekStart(eerste)) / (7 * dag)) + 1).toInt().coerceIn(4, 52)
        (weken - 1 downTo 0).map { terug ->
            val start = huidige - terug * 7 * dag
            start to logs.filter { it.startedAt in start until start + 7 * dag }.sumOf { it.blockedSeconds }
        }
    } else {
        val vandaag = Calendar.getInstance().apply {
            timeInMillis = nu
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val aantal = periode.dagen ?: 7
        (aantal - 1 downTo 0).map { terug ->
            val start = vandaag - terug * dag
            start to logs.filter { it.startedAt in start until start + dag }.sumOf { it.blockedSeconds }
        }
    }
}

@Composable
private fun PuntenKaart(totaal: Int, week: Int, bijnaam: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.stats_total), color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            Text("%,d".format(totaal), color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.stats_points_unit), color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(stringResource(R.string.stats_this_week), color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            Text("$week", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
            Text(
                bijnaam,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun Kaart(titel: String, voet: String? = null, inhoud: @Composable () -> Unit) {
    Column {
        Text(
            titel,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp, top = 4.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .padding(16.dp)
        ) { inhoud() }
        if (voet != null) {
            Text(voet, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

/** Een eenvoudige staafgrafiek: één staaf per waarde, met labels eronder. */
@Composable
private fun Staven(waarden: List<Float>, labels: List<String>, bovenschriften: List<String>, uitgelicht: Int?) {
    val max = (waarden.maxOrNull() ?: 0f).coerceAtLeast(1f)
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier.fillMaxWidth().height(170.dp),
        horizontalArrangement = Arrangement.spacedBy(if (waarden.size > 14) 2.dp else 6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        waarden.forEachIndexed { i, w ->
            // Een staaf is een kolom: lege ruimte bovenin, het getal, en de
            // staaf zelf. De verhouding tussen ruimte en staaf is de waarde.
            val deel = (w / max).coerceIn(0.01f, 1f)
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.weight(1.001f - deel))
                bovenschriften.getOrNull(i)?.takeIf { it.isNotEmpty() }?.let {
                    Text(it, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(deel)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(
                            if (uitgelicht == null || i == uitgelicht) accent else accent.copy(alpha = 0.35f)
                        )
                )
            }
        }
    }
    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(if (waarden.size > 14) 2.dp else 6.dp)) {
        labels.forEach {
            Text(
                it,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Visible,
                softWrap = false,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun Stat(waarde: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(waarde, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
