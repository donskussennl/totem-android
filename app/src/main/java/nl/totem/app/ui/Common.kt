package nl.totem.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * De iconen waaruit je per modus kunt kiezen.
 *
 * Op iOS waren dit SF Symbols, die alleen op Apple-toestellen bestaan. Hier
 * staan de Material-iconen die er het dichtst bij komen; de sleutel die we
 * bewaren is een eigen korte naam, zodat de opslag hetzelfde blijft als de
 * iconenset ooit verandert.
 */
object TotemIcons {

    val all: List<String> = listOf(
        "laptop", "moon", "leaf", "run", "book", "food", "people", "circle"
    )

    fun vector(symbol: String): ImageVector = when (symbol) {
        "laptop" -> Icons.Filled.LaptopMac
        "moon" -> Icons.Filled.DarkMode
        "leaf" -> Icons.Filled.LocalFlorist
        "run" -> Icons.AutoMirrored.Filled.DirectionsRun
        "book" -> Icons.Filled.Book
        "food" -> Icons.Filled.Restaurant
        "people" -> Icons.Filled.Group
        else -> Icons.Filled.Circle
    }
}

/** Het ringenmotief van de Totem, hergebruikt door het hele ontwerp. */
@Composable
fun TotemRings(
    size: Dp = 120.dp,
    lineWidth: Dp = 2.dp,
    color: Color = Color.Black,
    opacity: Float = 1f
) {
    Box(modifier = Modifier.size(size)) {
        Canvas(modifier = Modifier.size(size)) {
            val center = androidx.compose.ui.geometry.Offset(
                this.size.width / 2f,
                this.size.height / 2f
            )
            repeat(5) { i ->
                val fraction = 0.2f + i * 0.2f
                drawCircle(
                    color = color.copy(alpha = (0.35f + i * 0.13f) * opacity),
                    radius = this.size.minDimension * fraction / 2f,
                    center = center,
                    style = Stroke(width = lineWidth.toPx())
                )
            }
        }
    }
}

/**
 * "2u 15m" of, met seconden erbij, "2u 15m 03s".
 *
 * @param millis verstreken tijd in milliseconden
 */
fun formatElapsed(millis: Long, withSeconds: Boolean = false): String {
    val total = (millis / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (withSeconds) "${h}u ${m}m ${s}s" else "${h}u ${m}m"
}

/** Korte duurweergave voor de statistieken: "2u 15m" of "45m". */
fun formatDuration(seconds: Long): String {
    val minutes = seconds / 60
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "${h}u ${m}m" else "${m}m"
}
