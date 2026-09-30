package nl.totem.app.ui

import nl.totem.app.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.totem.app.store.AppStore

/**
 * Verschijnt wanneer iemand de Totem aantikt terwijl er geen blokkade loopt.
 * De tik is al gebeurd, dus kiezen is genoeg — er hoeft niet nog eens gescand.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeChoiceSheet(store: AppStore, onDismiss: () -> Unit) {
    val modes by store.modes.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            Text(
                text = stringResource(R.string.choice_tapped),
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 20.dp)
            )
            Text(
                text = stringResource(R.string.choice_body),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, top = 2.dp, bottom = 10.dp)
            )

            modes.forEach { mode ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (mode.isConfigured) 1f else 0.4f)
                        .clickable(enabled = mode.isConfigured) { store.startAfterTap(mode) }
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
                        Icon(TotemIcons.vector(mode.symbol), contentDescription = null)
                    }
                    Column(modifier = Modifier.padding(start = 14.dp)) {
                        Text(mode.name)
                        Text(
                            text = Texts.modeSummary(androidx.compose.ui.platform.LocalContext.current, mode),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.padding(start = 12.dp, top = 8.dp)
            ) { Text(stringResource(R.string.later)) }
        }
    }
}
