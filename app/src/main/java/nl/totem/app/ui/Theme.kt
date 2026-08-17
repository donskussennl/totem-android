package nl.totem.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * De kleuren van Totem op één plek.
 *
 * Bewust géén dynamic color (Material You): Totem heeft één herkenbaar blauw,
 * en dat moet op elke telefoon hetzelfde zijn.
 */
object Theme {
    /** Het blauw van het merk, licht genoeg om in het donker op te vallen. */
    val accentLight = Color(0xFF2B40FF)
    val accentDark = Color(0xFF6B7BFF)

    /** De twee toestanden van het detailscherm. */
    val activeBackground = Color(0xFF0A0A0A)
    val restBackground = Color(0xFFF2F2F2)
    val activeCard = Color(0xFF1C1C1C)
    val restCard = Color(0xFFE3E3E3)
}

private val LightColors = lightColorScheme(
    primary = Theme.accentLight,
    background = Color(0xFFFAFAFA),
    surface = Color(0xFFFFFFFF)
)

private val DarkColors = darkColorScheme(
    primary = Theme.accentDark,
    background = Color(0xFF0A0A0A),
    surface = Color(0xFF151515)
)

@Composable
fun TotemTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
