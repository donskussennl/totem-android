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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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


/**
 * Een teller die omhoog gaat elke keer dat dit scherm weer op de voorgrond komt.
 *
 * Nodig voor alles wat buiten de app om kan veranderen: de toestemmingen staan
 * in de systeeminstellingen, en Compose heeft geen enkele reden om die opnieuw
 * te lezen als de gebruiker terugkomt. Zet de uitkomst als sleutel op een
 * `remember`, dan wordt er wél opnieuw gemeten:
 *
 *     val meting = terugkeerTeller()
 *     val aan = remember(meting) { ShieldService.isAccessibilityEnabled(context) }
 */
@Composable
fun terugkeerTeller(): Int {
    var meting by remember { mutableIntStateOf(0) }
    val eigenaar = LocalLifecycleOwner.current
    DisposableEffect(eigenaar) {
        val kijker = LifecycleEventObserver { _, gebeurtenis ->
            if (gebeurtenis == Lifecycle.Event.ON_RESUME) meting++
        }
        eigenaar.lifecycle.addObserver(kijker)
        onDispose { eigenaar.lifecycle.removeObserver(kijker) }
    }
    return meting
}
