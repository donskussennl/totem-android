package nl.totem.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.totem.app.model.SessionLog
import nl.totem.app.store.AppStore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Laat zien hoe vaak, hoe lang en wanneer Totem actief was. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(store: AppStore, onBack: () -> Unit) {
    val history by store.history.collectAsStateWithLifecycle()
    var periode by remember { mutableStateOf(Periode.WEEK) }

    val logs = remember(history, periode) {
        val dagen = periode.dagen ?: return@remember history
        val grens = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -dagen)
        }.timeInMillis
        history.filter { it.startedAt >= grens }
    }

    val totaal = logs.sumOf { it.duration }
    val gemiddeld = if (logs.isEmpty()) 0L else totaal / logs.size

    // Buiten de LazyColumn berekend: de bouwers daarvan zijn geen composables.
    val perDag = remember(logs) { groepeerPerDag(logs) }
    val perModus = remember(logs) { groepeerPerModus(logs) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistieken") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Terug")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Periode.entries.forEach { p ->
                        FilterChip(
                            selected = periode == p,
                            onClick = { periode = p },
                            label = { Text(p.label) }
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Stat("${logs.size}", "keer actief")
                    Stat(formatDuration(totaal), "totaal")
                    Stat(formatDuration(gemiddeld), "gemiddeld")
                }
                HorizontalDivider()
            }

            if (perDag.isNotEmpty()) {
                item { Kop("Per dag") }
                items(perDag, key = { it.first }) { (dag, som) ->
                    Regel(dagFormat.format(Date(dag)), formatDuration(som))
                }
            }

            if (perModus.isNotEmpty()) {
                item { Kop("Per modus") }
                items(perModus, key = { it.naam }) { entry ->
                    Regel(entry.naam, "${entry.aantal}×  ${formatDuration(entry.totaal)}")
                }
            }

            item { Kop("Laatste sessies") }
            if (logs.isEmpty()) {
                item {
                    Text(
                        "Nog geen sessies in deze periode.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
            } else {
                items(logs.takeLast(15).reversed(), key = { it.id }) { log ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(log.modeName, modifier = Modifier.weight(1f))
                            Text(
                                formatDuration(log.duration),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            sessieFormat.format(Date(log.startedAt)),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item { Column(modifier = Modifier.padding(bottom = 40.dp)) {} }
        }
    }
}

private enum class Periode(val label: String, val dagen: Int?) {
    WEEK("Deze week", 7),
    MAAND("Deze maand", 30),
    ALLES("Alles", null)
}

private data class ModusTotaal(val naam: String, val aantal: Int, val totaal: Long)

private fun groepeerPerDag(logs: List<SessionLog>): List<Pair<Long, Long>> =
    logs.groupBy { log ->
        Calendar.getInstance().apply {
            timeInMillis = log.startedAt
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
        .map { (dag, lijst) -> dag to lijst.sumOf { it.duration } }
        .sortedByDescending { it.first }
        .take(14)

private fun groepeerPerModus(logs: List<SessionLog>): List<ModusTotaal> =
    logs.groupBy { it.modeName }
        .map { (naam, lijst) ->
            ModusTotaal(naam, lijst.size, lijst.sumOf { it.duration })
        }
        .sortedByDescending { it.totaal }

@Composable
private fun Stat(waarde: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(waarde, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Text(
            label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
        modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 6.dp)
    )
}

@Composable
private fun Regel(links: String, rechts: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        Text(links, modifier = Modifier.weight(1f))
        Text(rechts, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private val dagFormat = SimpleDateFormat("EEE d MMM", Locale("nl"))
private val sessieFormat = SimpleDateFormat("d MMM HH:mm", Locale("nl"))
