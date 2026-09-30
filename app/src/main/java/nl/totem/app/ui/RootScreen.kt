package nl.totem.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.totem.app.data.SharedStore
import nl.totem.app.model.FocusMode
import nl.totem.app.store.AppStore

/**
 * Bepaalt welk scherm je ziet.
 *
 * Dezelfde drie toestanden als op iOS — nog niet gekoppeld, een blokkade die
 * loopt, of de lijst met modi — met er één voor: het toestemmingsscherm. Dat is
 * er op Android bij nodig, want zonder toegankelijkheid en schermoverlay kan de
 * app niets doen, en het systeem vraagt er zelf nooit om.
 *
 * De navigatie doen we met een eenvoudige toestandsmachine in plaats van een
 * NavHost — er zijn maar een paar schermen, en zo blijft de overgang tussen
 * licht en donker vloeiend.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootScreen(store: AppStore) {

    val totem by store.totem.collectAsStateWithLifecycle()
    val session by store.session.collectAsStateWithLifecycle()
    val modes by store.modes.collectAsStateWithLifecycle()
    val error by store.errorMessage.collectAsStateWithLifecycle()
    val awaitingChoice by store.awaitingModeChoice.collectAsStateWithLifecycle()

    /** Welk scherm er open staat bovenop de lijst. */
    var route by remember { mutableStateOf<Route>(Route.Modes) }

    /** "Modus toevoegen" is aangetikt. */
    var nieuweModus by remember { mutableStateOf(false) }

    // Het toestemmingsscherm: alleen zolang het nog nodig is, en alleen als de
    // gebruiker het niet zelf heeft weggeklikt.
    val context = LocalContext.current
    var onboardingWeg by remember { mutableStateOf(SharedStore.hasOnboarded) }
    val toonOnboarding = remember(onboardingWeg) {
        !onboardingWeg && heeftOnboardingNodig(context)
    }

    val activeMode = session?.let { s -> modes.firstOrNull { it.id == s.modeID } }

    // Zodra een blokkade begint of eindigt, terug naar het juiste scherm.
    LaunchedEffect(session?.modeID) {
        route = if (activeMode != null) Route.Detail(activeMode.id) else Route.Modes
    }

    LaunchedEffect(Unit) { store.onAppear() }

    AnimatedContent(
        targetState = when {
            toonOnboarding -> Screen.Onboarding
            totem == null -> Screen.Pairing
            activeMode != null -> Screen.Active(activeMode.id)
            else -> Screen.Normal
        },
        transitionSpec = {
            fadeIn(tween(250)) togetherWith fadeOut(tween(250))
        },
        label = "root"
    ) { screen ->
        when (screen) {
            Screen.Onboarding -> OnboardingScreen(
                onKlaar = {
                    SharedStore.hasOnboarded = true
                    onboardingWeg = true
                },
                onOverslaan = {
                    SharedStore.hasOnboarded = true
                    onboardingWeg = true
                }
            )

            Screen.Pairing -> PairingScreen(store)

            is Screen.Active -> ModeDetailScreen(
                store = store,
                modeID = screen.modeID,
                onBack = { }
            )

            Screen.Normal -> when (val r = route) {
                is Route.Modes -> ModesScreen(
                    store = store,
                    onOpenMode = { route = Route.Detail(it) },
                    onOpenSettings = { route = Route.Settings },
                    onOpenStats = { route = Route.Stats },
                    onAddMode = { nieuweModus = true }
                )

                is Route.Detail -> ModeDetailScreen(
                    store = store,
                    modeID = r.modeID,
                    onBack = { route = Route.Modes }
                )

                is Route.Settings -> SettingsScreen(
                    store = store,
                    onBack = { route = Route.Modes },
                    onOpenStats = { route = Route.Stats }
                )

                is Route.Stats -> StatsScreen(
                    store = store,
                    onBack = { route = Route.Modes }
                )
            }
        }
    }

    // Een nieuwe modus: tijdelijk nog via de editor.
    if (nieuweModus) {
        val nieuw = remember { FocusMode(name = "", symbol = "circle") }
        ModeEditorSheet(
            store = store,
            mode = nieuw,
            isNew = true,
            onDismiss = { nieuweModus = false }
        )
    }

    // Je hebt getikt terwijl er niets liep: kiezen welke modus het wordt.
    if (awaitingChoice) {
        ModeChoiceSheet(
            store = store,
            onDismiss = { store.dismissModeChoice() }
        )
    }

    // Eén plek voor alle foutmeldingen, net als de .alert op iOS.
    if (error != null) {
        AlertDialog(
            onDismissRequest = { store.clearError() },
            title = { Text("Er ging iets mis") },
            text = { Text(error ?: "") },
            confirmButton = {
                TextButton(onClick = { store.clearError() }) { Text("Oké") }
            }
        )
    }

    // Het scanvenster: Android heeft er geen systeemversie van, dus die maken
    // we zelf. Zie ScanSheet.
    ScanSheet(store)
}

/**
 * De vier toestanden van het hoofdscherm.
 *
 * `Active` draagt de modus-id mét zich mee, en dat is geen detail. AnimatedContent
 * tekent bij een overgang beide kanten nog even door: een kwart seconde lang
 * bestaat het oude scherm nog terwijl het nieuwe al invloeit. Op het moment dat
 * een blokkade stopt is de sessie dus al weg, terwijl dit scherm nog één keer
 * wordt getekend.
 *
 * Las het die id op dat moment uit de lopende sessie, dan greep het mis en viel
 * de hele app om — precies op de tik waarmee je wilde ontgrendelen. Door de id
 * in de toestand zelf te zetten, heeft het uitgaande scherm alles wat het nodig
 * heeft en kan het rustig uitfaden.
 */
private sealed interface Screen {
    data object Onboarding : Screen
    data object Pairing : Screen
    data class Active(val modeID: String) : Screen
    data object Normal : Screen
}

private sealed interface Route {
    data object Modes : Route
    data class Detail(val modeID: String) : Route
    data object Settings : Route
    data object Stats : Route
}
