package nl.totem.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.totem.app.data.PinService

/** Waarom er om een pincode wordt gevraagd. */
sealed interface PinPurpose {
    /** Nieuwe pincode instellen; twee keer invoeren. */
    data object Create : PinPurpose

    /** Controleren, met uitleg waarom. */
    data class Verify(val reason: String) : PinPurpose
}

/**
 * Invoerscherm voor de pincode van de strikte modus.
 *
 * Net als op iOS: vier bolletjes, een onzichtbaar veld dat het cijfertoetsen-
 * bord opent, en bij het instellen twee keer invoeren.
 */
@Composable
fun PinDialog(
    purpose: PinPurpose,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var eerste by remember { mutableStateOf("") }
    var tweede by remember { mutableStateOf("") }
    var fout by remember { mutableStateOf<String?>(null) }
    val focus = remember { FocusRequester() }

    val bevestigen = purpose is PinPurpose.Create && eerste.length == 4
    val invoer = if (bevestigen) tweede else eerste

    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    fun afhandelen(cijfers: String) {
        when (purpose) {
            is PinPurpose.Create -> {
                if (!bevestigen) {
                    fout = null                 // eerste invoer bewaard
                } else if (tweede == eerste) {
                    PinService.set(eerste)
                    onSuccess()
                } else {
                    fout = "De pincodes zijn niet gelijk. Probeer opnieuw."
                    eerste = ""
                    tweede = ""
                }
            }
            is PinPurpose.Verify -> {
                if (PinService.verify(cijfers)) {
                    onSuccess()
                } else {
                    fout = "Onjuiste pincode."
                    eerste = ""
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (purpose) {
                    is PinPurpose.Create -> "Pincode instellen"
                    is PinPurpose.Verify -> "Pincode invoeren"
                }
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when (purpose) {
                        is PinPurpose.Create ->
                            if (bevestigen) "Voer dezelfde pincode nog een keer in."
                            else "Kies een pincode van vier cijfers. Je hebt hem nodig " +
                                "om de strikte modus uit te zetten."
                        is PinPurpose.Verify -> purpose.reason
                    },
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.padding(top = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    repeat(4) { i ->
                        Box(
                            modifier = Modifier
                                .size(17.dp)
                                .clip(CircleShape)
                                .border(
                                    1.5.dp,
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                    CircleShape
                                )
                                .background(
                                    if (i < invoer.length) MaterialTheme.colorScheme.onSurface
                                    else Color.Transparent,
                                    CircleShape
                                )
                        )
                    }
                }

                fout?.let {
                    Text(
                        text = it,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                // Onzichtbaar veld dat het cijfertoetsenbord opent.
                TextField(
                    value = invoer,
                    onValueChange = { waarde ->
                        val cijfers = waarde.filter { it.isDigit() }.take(4)
                        if (bevestigen) tweede = cijfers else eerste = cijfers
                        if (cijfers.length == 4) afhandelen(cijfers)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(0.01f)
                        .focusRequester(focus)
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuleer") }
        }
    )
}
