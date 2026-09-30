package nl.totem.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Rondjes voor de dagen van de week. Gekozen dagen zijn gevuld, de rest grijs.
 *
 * De nummering is die van [java.util.Calendar]: 1 = zondag … 7 = zaterdag,
 * precies zoals op iOS. Maandag staat vooraan, zoals we in Nederland gewend
 * zijn.
 */
@Composable
fun WeekdayPicker(
    selection: Set<Int>,
    onChange: (Set<Int>) -> Unit,
    modifier: Modifier = Modifier
) {
    // Maandag eerst; de letters komen van Android, dus in de taal van de
    // telefoon (M D W D V Z Z, of M T W T F S S).
    val context = androidx.compose.ui.platform.LocalContext.current
    val letters = androidx.compose.runtime.remember(context) { Texts.dayLetters(context) }
    val dagen = listOf(2, 3, 4, 5, 6, 7, 1).map { it to (letters[it] ?: "") }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        dagen.forEach { (nummer, letter) ->
            val aan = selection.contains(nummer)
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        if (aan) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable {
                        onChange(
                            if (aan) selection - nummer else selection + nummer
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = letter,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (aan) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
