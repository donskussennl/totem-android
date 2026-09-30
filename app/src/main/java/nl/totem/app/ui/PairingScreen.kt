package nl.totem.app.ui

import androidx.compose.ui.res.stringResource
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.Button
import nl.totem.app.model.PairedTotem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.totem.app.R
import nl.totem.app.store.AppStore

/**
 * Het eerste scherm: koppel je Totem.
 *
 * Anders dan op iOS controleren we hier ook of NFC überhaupt aan staat. Op
 * Android kan de gebruiker NFC uitzetten terwijl het toestel het wel heeft, en
 * dan is een duidelijke uitleg beter dan een knop die niets doet.
 */
@Composable
fun PairingScreen(store: AppStore) {
    val context = LocalContext.current
    var naam by remember { mutableStateOf(store.pairingName) }

    val adapter = remember { android.nfc.NfcAdapter.getDefaultAdapter(context) }
    val heeftNfc = adapter != null
    val nfcAan = adapter?.isEnabled == true

    val overgang = rememberInfiniteTransition(label = "adem")
    val schaal by overgang.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "schaal"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(Modifier.weight(1f))

        Image(
            painter = painterResource(R.drawable.totem_light),
            contentDescription = null,
            modifier = Modifier
                .height(220.dp)
                .scale(schaal)
        )

        Text(
            text = "Totem",
            fontSize = 34.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 8.dp)
        )

        Text(
            text = "Tap back to reality.",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(Modifier.weight(1f))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.pair_title),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.pair_body),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )

            OutlinedTextField(
                value = naam,
                onValueChange = {
                    naam = it
                    // Het model leest dit uit zodra je tikt; er is geen knop
                    // nodig, de lezer staat al aan.
                    store.pairingName = it
                },
                label = { Text(stringResource(R.string.set_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
            )
        }

        Button(
            onClick = { store.pairTotem(naam.ifBlank { context.getString(R.string.my_totem) }) },
            // Met het codewoord kan er zonder NFC gekoppeld worden, zodat
            // een reviewer op een toestel zonder NFC toch verder komt.
            enabled = (heeftNfc && nfcAan) ||
                naam.trim().uppercase() == PairedTotem.DEMO_CODE,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 20.dp)
        ) {
            Icon(Icons.Filled.Nfc, contentDescription = null)
            Text(stringResource(R.string.pair_scan), modifier = Modifier.padding(start = 8.dp))
        }

        if (!heeftNfc) {
            Text(
                text = stringResource(R.string.pair_no_nfc),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 10.dp, start = 24.dp, end = 24.dp)
            )
        } else if (!nfcAan) {
            Text(
                text = stringResource(R.string.err_nfc_off),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 10.dp, start = 24.dp, end = 24.dp)
            )
        }

        Spacer(Modifier.height(40.dp))
    }
}
