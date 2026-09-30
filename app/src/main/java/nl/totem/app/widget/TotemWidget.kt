package nl.totem.app.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.action.actionStartActivity
import nl.totem.app.MainActivity
import nl.totem.app.data.SharedStore
import nl.totem.app.ui.formatElapsed

/**
 * De widget op het startscherm.
 *
 * De iOS-versie had hier een Live Activity en een widget; op Android doet de
 * doorlopende melding het eerste, en dit het tweede. Bewust sober: één blik
 * moet genoeg zijn om te zien of er iets loopt.
 *
 * Een widget wordt niet elke seconde bijgewerkt — Android staat dat niet toe.
 * De teller springt dus per update; voor de seconde-teller open je de app of
 * kijk je naar de melding.
 */
class TotemWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        SharedStore.init(context)

        provideContent {
            val session = SharedStore.session
            val mode = session?.let { SharedStore.mode(it.modeID) }

            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(if (session != null) Color(0xFF141414) else Color(0xFFF2F2F2))
                        .padding(14.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally
                ) {
                    if (session != null && mode != null) {
                        Text(
                            text = mode.name,
                            style = TextStyle(
                                color = androidx.glance.unit.ColorProvider(Color.White),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Text(
                            text = formatElapsed(context, System.currentTimeMillis() - session.startedAt),
                            style = TextStyle(
                                color = androidx.glance.unit.ColorProvider(Color(0xFF9E9E9E)),
                                fontSize = 22.sp
                            ),
                            modifier = GlanceModifier.padding(top = 4.dp)
                        )
                        Text(
                            text = "Tik je Totem aan",
                            style = TextStyle(
                                color = androidx.glance.unit.ColorProvider(Color(0xFF6E6E6E)),
                                fontSize = 11.sp
                            ),
                            modifier = GlanceModifier.padding(top = 6.dp)
                        )
                    } else {
                        Text(
                            text = "Totem",
                            style = TextStyle(
                                color = androidx.glance.unit.ColorProvider(Color(0xFF1A1A1A)),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Text(
                            text = "Geen blokkade",
                            style = TextStyle(
                                color = androidx.glance.unit.ColorProvider(Color(0xFF6B6B6B)),
                                fontSize = 12.sp
                            ),
                            modifier = GlanceModifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

class TotemWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TotemWidget()
}
