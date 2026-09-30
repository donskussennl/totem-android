package nl.totem.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import nl.totem.app.R
import nl.totem.app.store.AppStore
import nl.totem.app.store.AppStore.UnlockPromo

/** Hoe lang een promotiescherm blijft staan, in milliseconden. */
private const val DUUR = 12_000

/**
 * Kaart die na een geslaagde ontgrendeling even verschijnt: een vraag, een
 * weetje of een uitnodiging. Verdwijnt na twaalf seconden, bij een tik ernaast
 * of zodra de app naar de achtergrond gaat.
 */
@Composable
fun UnlockPromoOverlay(store: AppStore, promo: UnlockPromo) {
    val voortgang = remember(promo) { Animatable(1f) }
    LaunchedEffect(promo) {
        voortgang.animateTo(0f, tween(DUUR, easing = LinearEasing))
        store.dismissPromo()
    }

    // App dicht of op de achtergrond: promotiescherm weg.
    val eigenaar = LocalLifecycleOwner.current
    DisposableEffect(eigenaar) {
        val kijker = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_STOP) store.dismissPromo() }
        eigenaar.lifecycle.addObserver(kijker)
        onDispose { eigenaar.lifecycle.removeObserver(kijker) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                store.dismissPromo()
            },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                // Tikken op de kaart zelf sluit hem niet.
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
        ) {
            Box {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val accent = MaterialTheme.colorScheme.primary
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icoon(promo), contentDescription = null, tint = accent, modifier = Modifier.size(32.dp))
                    }

                    if (promo is UnlockPromo.Tip) {
                        Text(
                            stringResource(R.string.promo_tip_label).uppercase(),
                            color = accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        )
                    }

                    Text(
                        text = when (promo) {
                            is UnlockPromo.Feedback -> stringResource(R.string.promo_feedback_title, promo.modeName)
                            is UnlockPromo.Tip -> stringResource(R.string.promo_tip_title)
                            is UnlockPromo.Schedule -> stringResource(R.string.promo_schedule_title)
                            is UnlockPromo.Location -> stringResource(R.string.promo_location_title)
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )

                    val uitleg = when (promo) {
                        is UnlockPromo.Feedback -> null
                        is UnlockPromo.Tip -> stringResource(promo.textRes)
                        is UnlockPromo.Schedule -> stringResource(R.string.promo_schedule_body)
                        is UnlockPromo.Location -> stringResource(R.string.promo_location_body)
                    }
                    if (uitleg != null) {
                        Text(
                            uitleg,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    when (promo) {
                        is UnlockPromo.Feedback -> {
                            Knop(stringResource(R.string.promo_feedback_good), primair = true) { store.dismissPromo() }
                            Knop(stringResource(R.string.promo_feedback_better), primair = false) { store.promoAction(promo) }
                        }
                        is UnlockPromo.Tip -> Knop(stringResource(R.string.promo_tip_ok), primair = false) { store.dismissPromo() }
                        is UnlockPromo.Schedule -> Knop(stringResource(R.string.promo_schedule_button), primair = true) { store.promoAction(promo) }
                        is UnlockPromo.Location -> Knop(stringResource(R.string.promo_location_button), primair = true) { store.promoAction(promo) }
                    }

                    // Loopt in twaalf seconden leeg; dan sluit de kaart.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(voortgang.value)
                                .height(3.dp)
                                .clip(CircleShape)
                                .background(accent.copy(alpha = 0.4f))
                        )
                    }
                }
                IconButton(onClick = { store.dismissPromo() }, modifier = Modifier.align(Alignment.TopEnd)) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun icoon(promo: UnlockPromo): ImageVector = when (promo) {
    is UnlockPromo.Feedback -> Icons.Filled.SentimentSatisfied
    is UnlockPromo.Tip -> Icons.Filled.Lightbulb
    is UnlockPromo.Schedule -> Icons.Filled.CalendarMonth
    is UnlockPromo.Location -> Icons.Filled.MyLocation
}

@Composable
private fun Knop(tekst: String, primair: Boolean, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(CircleShape)
            .then(
                if (primair) Modifier.background(accent)
                else Modifier.border(BorderStroke(1.5.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)), CircleShape)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            tekst,
            color = if (primair) Color.White else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (primair) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}
