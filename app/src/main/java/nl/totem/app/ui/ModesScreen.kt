package nl.totem.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.totem.app.model.FocusMode
import nl.totem.app.shield.ShieldService
import nl.totem.app.store.AppStore

/**
 * Overzicht van alle modi. Vanaf hier open je een modus, de instellingen of de
 * statistieken.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModesScreen(
    store: AppStore,
    onOpenMode: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenStats: () -> Unit
) {
    val context = LocalContext.current
    val modes by store.modes.collectAsStateWithLifecycle()
    var bewerken by remember { mutableStateOf<FocusMode?>(null) }

    // Elke hertekening opnieuw: de gebruiker kan net terug zijn uit de instellingen.
    val klaar = ShieldService.isReady(context)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Totem") },
                actions = {
                    IconButton(onClick = onOpenStats) {
                        Icon(Icons.Filled.BarChart, contentDescription = "Statistieken")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Instellingen")
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
            if (!klaar) {
                item {
                    ToestemmingKaart(
                        onOpen = { onOpenSettings() }
                    )
                }
            }

            item {
                Text(
                    text = "Modi",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 6.dp)
                )
            }

            items(modes, key = { it.id }) { mode ->
                ModeRij(
                    mode = mode,
                    onClick = { onOpenMode(mode.id) },
                    onEdit = { bewerken = mode }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 70.dp))
            }

            item {
                TextButton(
                    onClick = {
                        val nieuw = FocusMode(name = "Nieuwe modus", symbol = "circle")
                        store.addMode(nieuw)
                        bewerken = nieuw
                    },
                    modifier = Modifier.padding(start = 12.dp, top = 8.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("Modus toevoegen", modifier = Modifier.padding(start = 8.dp))
                }
            }

            item {
                Text(
                    text = "Kies een modus en tik je Totem aan om te starten. " +
                        "Stoppen kan alleen door opnieuw te tikken.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }
        }
    }

    bewerken?.let { mode ->
        ModeEditorSheet(
            store = store,
            mode = mode,
            onDismiss = { bewerken = null }
        )
    }
}

@Composable
private fun ModeRij(mode: FocusMode, onClick: () -> Unit, onEdit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = TotemIcons.vector(mode.symbol),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = mode.name,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (mode.isConfigured) mode.summary else "Nog niets ingesteld",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onEdit) {
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = "Aanpassen",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Zonder toegankelijkheid en overlay kan Totem niets blokkeren. */
@Composable
private fun ToestemmingKaart(onOpen: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .clickable(onClick = onOpen)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = "Totem kan nog niets blokkeren",
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = "Geef toegankelijkheid en ‘over andere apps tekenen’ vrij in de instellingen.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}
