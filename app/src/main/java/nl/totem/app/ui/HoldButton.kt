package nl.totem.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Knop met twee betekenissen.
 *
 * Kort tikken doet het gewone. Houd je hem langer dan een seconde vast, dan
 * verschijnt een voortgangsbalk; na drie seconden in totaal volgt de tweede
 * actie. Handig voor dingen die niet per ongeluk mogen gebeuren.
 */
@Composable
fun HoldButton(
    title: String,
    holdTitle: String,
    textColor: Color,
    borderColor: Color,
    fillColor: Color,
    enabled: Boolean = true,
    onTap: () -> Unit,
    onHold: () -> Unit,
    modifier: Modifier = Modifier
) {
    /** Zo lang blijft het een gewone knop; daarna begint de balk te lopen. */
    val graceMillis = 1000L
    /** Totale tijd vanaf het aanraken tot de tweede actie. */
    val holdMillis = 3000L

    var voortgang by remember { mutableFloatStateOf(0f) }
    var toontVoortgang by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val haptiek = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(CircleShape)
            .border(1.5.dp, borderColor, CircleShape)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        val begonnen = System.currentTimeMillis()

                        val teller = scope.launch {
                            while (true) {
                                delay(20)
                                val vast = System.currentTimeMillis() - begonnen
                                if (vast < graceMillis) continue
                                toontVoortgang = true
                                voortgang =
                                    ((vast - graceMillis).toFloat() /
                                        (holdMillis - graceMillis)).coerceIn(0f, 1f)
                                if (voortgang >= 1f) {
                                    haptiek.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onHold()
                                    break
                                }
                            }
                        }

                        tryAwaitRelease()

                        teller.cancel()
                        val vast = System.currentTimeMillis() - begonnen
                        // Losgelaten binnen de eerste seconde telt als tik.
                        if (vast < graceMillis) onTap()
                        toontVoortgang = false
                        voortgang = 0f
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // De vulling die meeloopt met de voortgang.
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .layout { measurable, constraints ->
                    val breedte = (constraints.maxWidth * voortgang).toInt()
                    val placeable = measurable.measure(
                        constraints.copy(minWidth = breedte, maxWidth = breedte)
                    )
                    layout(constraints.maxWidth, placeable.height) {
                        placeable.place(0, 0)
                    }
                }
                .background(fillColor),
            content = {}
        )

        Text(
            text = if (toontVoortgang) holdTitle else title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = textColor.copy(alpha = if (enabled) 1f else 0.4f)
        )
    }
}
