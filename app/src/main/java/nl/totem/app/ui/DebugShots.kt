package nl.totem.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import nl.totem.app.R
import nl.totem.app.data.SharedStore
import nl.totem.app.model.ActiveSession
import nl.totem.app.model.FocusMode
import nl.totem.app.model.PairedTotem
import nl.totem.app.model.Schedule
import nl.totem.app.model.ScheduleEnd
import nl.totem.app.shield.BlockScreen
import nl.totem.app.store.AppStore

/**
 * Losse schermen met voorbeelddata, alleen voor screenshots en om ontwerpen
 * na te lopen zonder echte Totem. MainActivity opent dit alleen in een
 * debug-build, met `--es shot <naam>`.
 *
 * Let op: de voorbeelddata worden in de opslag gezet. Dat is precies de
 * bedoeling op een emulator, maar gebruik het niet op je eigen telefoon.
 */
@Composable
fun DebugShots(kind: String, store: AppStore) {
    val werk = stringResource(R.string.mode_work)
    val context = LocalContext.current

    when (kind) {
        "block" -> BlockScreen(appName = "Instagram")

        "detail", "active", "paused" -> {
            val mode = voorbeeldModus(werk)
            LaunchedEffect(kind) {
                SharedStore.init(context)
                SharedStore.totem = PairedTotem("DEMO", "Demo Totem", 0L, isDemo = true)
                SharedStore.modes = listOf(mode)
                val nu = System.currentTimeMillis()
                SharedStore.session = when (kind) {
                    "active" -> ActiveSession(mode.id, nu - (8 * 60 + 15) * 1000L)
                    "paused" -> ActiveSession(
                        modeID = mode.id,
                        startedAt = nu - 40 * 60_000L,
                        startedBySchedule = true,
                        pausedUntil = nu + 4 * 60_000L + 32_000L,
                        pauseStartedAt = nu - 28_000L
                    )
                    else -> null
                }
                store.load()
            }
            ModeDetailScreen(store = store, modeID = mode.id, onBack = {})
        }

        else -> RootScreen(store)
    }
}

/** Een modus met 13 apps en een schema, met een vaste id. */
private fun voorbeeldModus(naam: String) = FocusMode(
    id = "debug-werk",
    name = naam,
    symbol = "laptop",
    blockedPackages = (1..13).map { "com.voorbeeld.app$it" }.toSet(),
    schedule = Schedule(isOn = true, end = ScheduleEnd.Time(17, 0))
)
