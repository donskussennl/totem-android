package nl.totem.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.totem.app.shield.InstalledApps

/**
 * De app-kiezer: onze eigen versie van Apple's `FamilyActivityPicker`.
 *
 * Anders dan op iOS mogen we hier de echte namen en iconen laten zien, en kun
 * je zoeken. Het laden gebeurt op een achtergronddraad, want het opvragen van
 * alle iconen kost op een vol toestel een halve seconde.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerSheet(
    geselecteerd: Set<String>,
    onDismiss: () -> Unit,
    onGereed: (Set<String>) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var apps by remember { mutableStateOf<List<InstalledApps.Item>?>(null) }
    var keuze by remember { mutableStateOf(geselecteerd) }
    var zoek by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) { InstalledApps.load(context) }
    }

    val zichtbaar = remember(apps, zoek) {
        val lijst = apps.orEmpty()
        if (zoek.isBlank()) lijst
        else lijst.filter { it.label.contains(zoek, ignoreCase = true) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxHeight(0.9f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Apps om te blokkeren", fontSize = 19.sp)
                    Text(
                        "${keuze.size} gekozen",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onDismiss) { Text("Annuleer") }
                Button(onClick = { onGereed(keuze) }) { Text("Gereed") }
            }

            OutlinedTextField(
                value = zoek,
                onValueChange = { zoek = it },
                label = { Text("Zoeken") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            )

            // Alles in één keer aan of uit. Werkt op wat er zichtbaar is, dus
            // met een zoekterm actief selecteer je alleen die treffers -- zo
            // kun je bijvoorbeeld in twee tikken alle Google-apps pakken.
            if (zichtbaar.isNotEmpty()) {
                val zichtbarePakketten = zichtbaar.map { it.packageName }.toSet()
                val allesAan = keuze.containsAll(zichtbarePakketten)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            keuze = if (allesAan) keuze - zichtbarePakketten
                                    else keuze + zichtbarePakketten
                        }
                    ) {
                        Text(
                            if (allesAan) "Alles uitzetten" else
                                if (zoek.isBlank()) "Alles selecteren"
                                else "Alle ${zichtbaar.size} treffers selecteren"
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    if (keuze.isNotEmpty()) {
                        TextButton(onClick = { keuze = emptySet() }) {
                            Text("Selectie wissen")
                        }
                    }
                }
            }

            if (apps == null) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(zichtbaar, key = { it.packageName }) { app ->
                        AppRij(
                            app = app,
                            gekozen = keuze.contains(app.packageName),
                            onToggle = {
                                keuze = if (keuze.contains(app.packageName)) {
                                    keuze - app.packageName
                                } else {
                                    keuze + app.packageName
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRij(
    app: InstalledApps.Item,
    gekozen: Boolean,
    onToggle: () -> Unit
) {
    val icoon = remember(app.packageName) {
        runCatching { app.icon?.toBitmap(96, 96)?.asImageBitmap() }.getOrNull()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icoon != null) {
            Image(
                painter = BitmapPainter(icoon),
                contentDescription = null,
                modifier = Modifier.size(38.dp)
            )
        } else {
            Box(modifier = Modifier.size(38.dp))
        }

        Text(
            text = app.label,
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp)
        )

        Checkbox(checked = gekozen, onCheckedChange = { onToggle() })
    }
}
