package nl.totem.app.ui

import androidx.compose.runtime.Composable
import nl.totem.app.shield.BlockScreen
import nl.totem.app.store.AppStore

/**
 * Losse schermen met voorbeelddata, alleen voor screenshots en om ontwerpen
 * na te lopen zonder echte Totem. MainActivity opent dit alleen in een
 * debug-build, met `--es shot <naam>`.
 */
@Composable
fun DebugShots(kind: String, store: AppStore) {
    when (kind) {
        "block" -> BlockScreen(appName = "Instagram")
        else -> RootScreen(store)
    }
}
