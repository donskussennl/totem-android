package nl.totem.app.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.totem.app.store.AppStore

/**
 * Het scanvenster.
 *
 * Dit bestaat op iOS niet als eigen scherm: daar tekent het systeem zelf een
 * venster met de tekst die je meegeeft, en verdwijnt het weer zodra er een tag
 * gelezen is. Android leest gewoon door zolang de lezer aan staat, dus moet de
 * app zelf laten zien dát er gescand wordt — en de gebruiker de kans geven om
 * te stoppen.
 *
 * De ringen pulseren zachtjes, zodat duidelijk is dat de telefoon wacht.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanSheet(store: AppStore) {
    val request by store.scanRequest.collectAsStateWithLifecycle()
    val huidige = request ?: return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { store.cancelScan() },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PulserendeRingen()

            Text(
                text = "Klaar om te lezen",
                fontSize = 21.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 24.dp)
            )

            Text(
                text = huidige.prompt,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            Text(
                text = "Houd de bovenkant van je telefoon tegen de Totem.",
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )

            TextButton(
                onClick = { store.cancelScan() },
                modifier = Modifier.padding(top = 20.dp)
            ) {
                Text("Annuleer")
            }
        }
    }
}

@Composable
private fun PulserendeRingen() {
    val overgang = rememberInfiniteTransition(label = "puls")
    val factor by overgang.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "schaal"
    )

    Box(modifier = Modifier.padding(top = 28.dp).scale(factor)) {
        TotemRings(size = 120.dp, color = MaterialTheme.colorScheme.primary)
    }
}
