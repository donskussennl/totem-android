package nl.totem.app.ui

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.totem.app.R
import nl.totem.app.model.FocusMode
import nl.totem.app.shield.ShieldService
import nl.totem.app.store.AppStore

/**
 * Hoofdscherm: je modi als kaarten, met onderin één grote knop om je Totem te
 * activeren. Eén modus? Dan start die meteen. Meer? Dan kies je eerst, en
 * daarna gaat meteen het scanvenster open.
 *
 * Een kaart naar links vegen verwijdert de modus, na een bevestiging.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModesScreen(
    store: AppStore,
    onOpenMode: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenStats: () -> Unit,
    onAddMode: () -> Unit
) {
    val context = LocalContext.current
    val modes by store.modes.collectAsStateWithLifecycle()
    var teVerwijderen by remember { mutableStateOf<FocusMode?>(null) }
    var kiezen by remember { mutableStateOf(false) }

    // Elke keer opnieuw meten: de gebruiker kan net terug zijn uit de instellingen.
    val meting = terugkeerTeller()
    val klaar = remember(meting) { ShieldService.isReady(context) }

    val klaarVoorGebruik = modes.filter { it.isConfigured }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onOpenStats) {
                        Icon(Icons.Filled.BarChart, contentDescription = stringResource(R.string.stats_title))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                }
            )
        },
        bottomBar = {
            ActiveerBalk(
                modes = klaarVoorGebruik,
                onActiveer = {
                    if (klaarVoorGebruik.size == 1) store.startSession(klaarVoorGebruik[0])
                    else kiezen = true
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!klaar) {
                item { ToestemmingKaart(onOpen = onOpenSettings) }
            }

            items(modes, key = { it.id }) { mode ->
                VeegOmTeVerwijderen(onVeeg = { teVerwijderen = mode }) {
                    ModusKaart(mode = mode, onClick = { onOpenMode(mode.id) })
                }
            }

            item { ToevoegKaart(aantal = modes.size, onClick = onAddMode) }

            item {
                Text(
                    text = stringResource(R.string.home_swipe_hint),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 24.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }

    teVerwijderen?.let { mode ->
        AlertDialog(
            onDismissRequest = { teVerwijderen = null },
            title = { Text(stringResource(R.string.delete_title, mode.name)) },
            text = { Text(stringResource(R.string.delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    store.deleteMode(mode.id)
                    teVerwijderen = null
                }) {
                    Text(stringResource(R.string.delete_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { teVerwijderen = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (kiezen) {
        KiesModusSheet(
            modes = klaarVoorGebruik,
            onDismiss = { kiezen = false },
            onKies = { mode ->
                kiezen = false
                store.startSession(mode)
            }
        )
    }
}

/**
 * Vegen naar links laat een rode achtergrond zien. Loslaten verwijdert niets:
 * de kaart veert terug en er komt eerst een bevestiging.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VeegOmTeVerwijderen(onVeeg: () -> Unit, content: @Composable () -> Unit) {
    // De laatste versie van onVeeg gebruiken: de state wordt maar één keer
    // gemaakt, en zou anders de modus van het eerste moment onthouden (met
    // bijvoorbeeld een oude naam in de bevestiging).
    val huidigeVeeg by rememberUpdatedState(onVeeg)
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { waarde ->
            if (waarde == SwipeToDismissBoxValue.EndToStart) huidigeVeeg()
            false
        }
    )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            // Alleen rood tijdens het vegen; in rust is er niets te zien.
            if (state.dismissDirection != SwipeToDismissBoxValue.EndToStart) return@SwipeToDismissBox
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.error)
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.delete_confirm),
                    tint = Color.White
                )
            }
        }
    ) { content() }
}

@Composable
private fun ModusKaart(mode: FocusMode, onClick: () -> Unit) {
    val context = LocalContext.current
    val accent = MaterialTheme.colorScheme.primary
    val schema = scheduleSummary(mode.schedule)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            // Eerst ondoorzichtig, anders schijnt het rood van het vegen erdoorheen.
            .background(MaterialTheme.colorScheme.surface)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    (if (mode.isConfigured) accent else MaterialTheme.colorScheme.onSurfaceVariant)
                        .copy(alpha = 0.12f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = TotemIcons.vector(mode.symbol),
                contentDescription = null,
                tint = if (mode.isConfigured) accent else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(mode.name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = Texts.modeSummary(context, mode),
                fontSize = 14.sp,
                color = if (mode.isConfigured) MaterialTheme.colorScheme.onSurfaceVariant
                else Color(0xFFE08A00)
            )
            if (schema != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (mode.schedule.isLocationBased) Icons.Filled.Place else Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        schema,
                        fontSize = 13.sp,
                        color = accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }

        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

/** Een gestippelde kaart onder de modi. */
@Composable
private fun ToevoegKaart(aantal: Int, onClick: () -> Unit) {
    val kan = aantal < FocusMode.MAX_COUNT
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(
                BorderStroke(1.5.dp, accent.copy(alpha = if (kan) 0.4f else 0.15f)),
                RoundedCornerShape(18.dp)
            )
            .clickable(enabled = kan, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = if (kan) accent else Color.Gray)
        Text(
            stringResource(R.string.home_add_mode),
            color = if (kan) accent else Color.Gray,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
        )
        Text(
            "$aantal/${FocusMode.MAX_COUNT}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** De grote knop onderin. */
@Composable
private fun ActiveerBalk(modes: List<FocusMode>, onActiveer: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    val kan = modes.isNotEmpty()
    Surface(tonalElevation = 3.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(CircleShape)
                    .background(if (kan) accent else Color.Gray.copy(alpha = 0.4f))
                    .clickable(enabled = kan, onClick = onActiveer),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Nfc, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (modes.size == 1) stringResource(R.string.home_activate_mode, modes[0].name)
                    else stringResource(R.string.detail_activate),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp
                )
            }
            Text(
                text = stringResource(
                    if (kan) R.string.home_activate_hint else R.string.home_activate_hint_empty
                ),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/**
 * Bij meer dan één modus: eerst kiezen, daarna gaat meteen het scanvenster
 * open. Niet nog een keer "activeren" in de modus zelf.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KiesModusSheet(
    modes: List<FocusMode>,
    onDismiss: () -> Unit,
    onKies: (FocusMode) -> Unit
) {
    val context = LocalContext.current
    val accent = MaterialTheme.colorScheme.primary
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Text(stringResource(R.string.choose_title), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.home_activate_hint),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )
            modes.forEach { mode ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .clickable { onKies(mode) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(TotemIcons.vector(mode.symbol), contentDescription = null, tint = accent)
                    }
                    Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                        Text(mode.name, fontWeight = FontWeight.SemiBold)
                        Text(
                            Texts.modeSummary(context, mode),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Filled.Nfc, contentDescription = null, tint = accent)
                }
            }
        }
    }
}

/** Zonder toegankelijkheid en overlay kan Totem niets blokkeren. */
@Composable
private fun ToestemmingKaart(onOpen: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFFF9500).copy(alpha = 0.14f))
            .clickable(onClick = onOpen)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFE08A00))
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(stringResource(R.string.home_permission_title), fontWeight = FontWeight.Medium)
            Text(
                text = stringResource(R.string.home_permission_body),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
