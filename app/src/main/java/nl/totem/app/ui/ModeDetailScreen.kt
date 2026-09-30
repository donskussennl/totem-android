package nl.totem.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import nl.totem.app.R
import nl.totem.app.store.AppStore

/**
 * Detailscherm van één modus. Hetzelfde scherm dient voor twee toestanden:
 * licht (nog niet actief) en donker (blokkade loopt). De overgang tussen die
 * twee is een vloeiende crossfade van kleur, tekst en de Totem-afbeelding.
 */
@Composable
fun ModeDetailScreen(
    store: AppStore,
    modeID: String,
    onBack: () -> Unit
) {
    val modes by store.modes.collectAsStateWithLifecycle()
    val session by store.session.collectAsStateWithLifecycle()
    val noodOver by store.emergencyRemaining.collectAsStateWithLifecycle()
    val mode = modes.firstOrNull { it.id == modeID } ?: return

    val isActive = session?.modeID == modeID

    var nu by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var bewerken by remember { mutableStateOf(false) }

    // Verborgen noodontgrendeling: drie keer op de Totem tikken.
    var geheimeTikken by remember { mutableIntStateOf(0) }
    var laatsteTik by remember { mutableLongStateOf(0L) }
    var bevestigNood by remember { mutableStateOf(false) }
    var pincodeVoorNood by remember { mutableStateOf(false) }

    LaunchedEffect(isActive) {
        while (true) {
            nu = System.currentTimeMillis()
            delay(1000)
        }
    }

    val achtergrond by animateColorAsState(
        targetValue = if (isActive) Theme.activeBackground else Theme.restBackground,
        animationSpec = tween(450),
        label = "achtergrond"
    )
    val tekstPrimair by animateColorAsState(
        targetValue = if (isActive) Color.White else Color(0xFF1A1A1A),
        animationSpec = tween(450),
        label = "tekst"
    )
    val tekstSecundair by animateColorAsState(
        targetValue = if (isActive) Color(0xFF9E9E9E) else Color(0xFF6B6B6B),
        animationSpec = tween(450),
        label = "tekst2"
    )
    val kaart by animateColorAsState(
        targetValue = if (isActive) Theme.activeCard else Theme.restCard,
        animationSpec = tween(450),
        label = "kaart"
    )

    // Eerst vastpakken, dan pas gebruiken. Lees je hem twee keer uit — één keer
    // om te controleren en één keer om te gebruiken — dan kan hij er de tweede
    // keer niet meer zijn, en dat is precies hoe deze app eerder omviel op de
    // tik waarmee je wilde ontgrendelen.
    val huidigeSessie = session
    val verstreken = if (isActive && huidigeSessie != null) {
        nu - huidigeSessie.startedAt
    } else 0L
    // Even ontdooid tijdens een schema: hoeveel tijd is er nog over?
    val pauzeOver = huidigeSessie?.pausedUntil
        ?.takeIf { isActive && it > nu }
        ?.let { it - nu }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(achtergrond)
    ) {
        if (!isActive) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.padding(8.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Terug",
                    tint = tekstPrimair
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // De teller: groot tijdens een blokkade, klein daarbuiten.
            if (isActive && pauzeOver != null) {
                val seconden = pauzeOver / 1000
                Text(
                    text = stringResource(R.string.detail_paused_label),
                    fontSize = 14.sp,
                    color = Color.White
                )
                Text(
                    text = "%d:%02d".format(seconden / 60, seconden % 60),
                    fontSize = 42.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else if (isActive) {
                Text(
                    text = stringResource(R.string.detail_block_time),
                    fontSize = 14.sp,
                    color = Color.White
                )
                Text(
                    text = formatElapsed(verstreken, withSeconds = true),
                    fontSize = 42.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                Text(
                    text = formatElapsed(0, withSeconds = false),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = tekstPrimair,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(kaart)
                        .padding(horizontal = 26.dp, vertical = 11.dp)
                )
            }

            // De Totem zelf. Drie keer tikken tijdens een blokkade opent de
            // noodontgrendeling.
            Box(
                modifier = Modifier
                    .padding(top = if (isActive) 28.dp else 34.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (!isActive) return@clickable
                        val moment = System.currentTimeMillis()
                        geheimeTikken =
                            if (moment - laatsteTik > 2000) 1 else geheimeTikken + 1
                        laatsteTik = moment
                        if (geheimeTikken >= 3) {
                            geheimeTikken = 0
                            if (store.emergencyRemaining.value > 0) {
                                bevestigNood = true
                            } else {
                                store.showError(
                                    "Je hebt deze maand al ${AppStore.EMERGENCY_LIMIT} " +
                                        "noodontgrendelingen gebruikt. Tik je Totem aan om te stoppen."
                                )
                            }
                        }
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                Image(
                    painter = painterResource(
                        if (isActive) R.drawable.totem_dark else R.drawable.totem_light
                    ),
                    contentDescription = null,
                    modifier = Modifier.height(236.dp)
                )
            }

            Text(
                text = if (isActive) "${mode.blockedCount} apps geblokkeerd" else mode.name,
                fontSize = if (isActive) 15.sp else 30.sp,
                fontWeight = if (isActive) FontWeight.Normal else FontWeight.SemiBold,
                color = if (isActive) tekstSecundair else tekstPrimair,
                modifier = Modifier.padding(top = 26.dp)
            )

            if (!isActive) {
                Text(
                    text = "${mode.blockedCount} apps geblokkeerd",
                    fontSize = 15.sp,
                    color = tekstSecundair,
                    modifier = Modifier.padding(top = 6.dp)
                )
                if (mode.schedule.isOn) {
                    Text(
                        text = mode.schedule.timeText,
                        fontSize = 14.sp,
                        color = tekstSecundair,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                TextButton(
                    onClick = { bewerken = true },
                    modifier = Modifier.padding(top = 14.dp)
                ) {
                    Text("Modus aanpassen  ›", color = tekstSecundair, fontSize = 15.sp)
                }
            }

            Spacer(Modifier.weight(1f))

            // De knop. Kort tikken opent de scanner; lang vasthouden is er
            // bewust niet, want stoppen hoort via de Totem te gaan.
            val ingeschakeld = isActive || mode.isConfigured
            val knopTekst = when {
                isActive && pauzeOver != null -> stringResource(R.string.detail_freeze_now)
                isActive && store.tapPauses() ->
                    stringResource(R.string.detail_unfreeze_minutes, mode.schedule.pauseMinutes)
                isActive -> stringResource(R.string.detail_deactivate)
                else -> stringResource(R.string.detail_activate)
            }
            Text(
                text = knopTekst,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = tekstPrimair.copy(alpha = if (ingeschakeld) 1f else 0.4f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 0.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, tekstSecundair.copy(alpha = 0.45f), CircleShape)
                    .clickable(enabled = ingeschakeld) {
                        when {
                            isActive && pauzeOver != null -> store.freezeNow()
                            isActive -> store.endSession()
                            else -> store.startSession(mode)
                        }
                    }
                    .padding(vertical = 17.dp)
            )

            Spacer(Modifier.height(34.dp))
        }
    }

    if (bevestigNood) {
        AlertDialog(
            onDismissRequest = { bevestigNood = false },
            title = { Text("Blokkade opheffen zonder Totem?") },
            text = {
                Text(
                    "Je hebt nog $noodOver van de " +
                        "${AppStore.EMERGENCY_LIMIT} noodontgrendelingen deze maand. " +
                        "Daarna kun je pas volgende maand weer zonder Totem ontgrendelen."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    bevestigNood = false
                    // In de strikte modus eerst de pincode; anders zou die
                    // schakelaar nergens op uitkomen.
                    if (store.emergencyNeedsPin) pincodeVoorNood = true
                    else store.useEmergencyUnlock()
                }) { Text("Gebruik noodontgrendeling") }
            },
            dismissButton = {
                TextButton(onClick = { bevestigNood = false }) { Text("Annuleer") }
            }
        )
    }

    if (pincodeVoorNood) {
        PinDialog(
            purpose = PinPurpose.Verify(
                "Voer je pincode in om zonder Totem te ontgrendelen."
            ),
            onDismiss = { pincodeVoorNood = false },
            onSuccess = {
                pincodeVoorNood = false
                store.useEmergencyUnlock()
            }
        )
    }

    if (bewerken) {
        ModeEditorSheet(
            store = store,
            mode = mode,
            onDismiss = { bewerken = false }
        )
    }
}
