package nl.totem.app.shield

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import nl.totem.app.data.SharedStore
import nl.totem.app.model.FocusMode
import java.util.concurrent.atomic.AtomicReference

/**
 * Zet de daadwerkelijke blokkade aan en uit.
 *
 * Dit is het onderdeel dat het meest verschilt van iOS. Daar deed
 * `ManagedSettingsStore` het werk: je gaf Apple een lijst met apps en het
 * systeem liet ze niet meer openen. Android heeft geen enkele API die dat voor
 * een gewone app kan.
 *
 * Wat wél kan: een toegankelijkheidsdienst mag zien welke app naar de
 * voorgrond komt. Staat die in de lijst, dan zetten we er meteen een
 * Totem-scherm overheen en sturen we de gebruiker terug naar het startscherm.
 * Het resultaat voelt hetzelfde, maar het is een andere afspraak met het
 * systeem — en de gebruiker moet er twee keer toestemming voor geven:
 *
 * 1. **Toegankelijkheid** — om te zien welke app opent.
 * 2. **Over andere apps tekenen** — om het blokkadescherm te mogen openen
 *    terwijl een andere app op de voorgrond staat.
 *
 * Alles wat de dienst nodig heeft staat hier in het geheugen, niet in de
 * opslag: de dienst wordt bij elke schermwissel aangeroepen en mag daar geen
 * bestand voor openen.
 */
object ShieldService {

    /** Alles wat de waakhond moet weten, in één onveranderlijk blokje. */
    data class Stand(
        val packages: Set<String> = emptySet(),
        val modeName: String = "Totem",
        val startedAt: Long = 0L
    )

    private val stand = AtomicReference(Stand())

    /** Snel te lezen vanuit de dienst, bij elke schermwissel. */
    val huidigeStand: Stand get() = stand.get()

    val isBlocking: Boolean get() = stand.get().packages.isNotEmpty()

    /**
     * Herstelt de blokkade uit de opslag. Aangeroepen bij het starten van de
     * app en van de toegankelijkheidsdienst — dus juist níét op de hete route.
     */
    fun restore(context: Context) {
        SharedStore.init(context)
        val session = SharedStore.session
        val mode = session?.let { SharedStore.mode(it.modeID) }
        stand.set(
            // Sessie eerst: is die er niet, dan is `mode` per definitie ook null.
            // Even ontdooid: dan hoort er juist niets geblokkeerd te zijn.
            if (session == null || mode == null || session.isPaused()) Stand()
            else Stand(
                packages = mode.blockedPackages - Onaantastbaar.voor(context),
                modeName = mode.name,
                startedAt = session.startedAt
            )
        )
    }

    /**
     * Blokkeert alles wat in de modus geselecteerd is.
     *
     * De onaantastbare apps gaan er hier alvast af: zo kan een oude modus met
     * een pakket dat inmiddels je launcher is nooit alsnog roet in het eten
     * gooien.
     *
     * @return false als de toestemmingen niet compleet zijn; dan kunnen we
     *         niets blokkeren en hoort de gebruiker een uitleg te zien.
     */
    fun startBlocking(context: Context, mode: FocusMode, startedAt: Long): Boolean {
        if (!isReady(context)) return false
        stand.set(
            Stand(
                packages = mode.blockedPackages - Onaantastbaar.voor(context),
                modeName = mode.name,
                startedAt = startedAt
            )
        )
        return true
    }

    /** Heft alle blokkades op. */
    fun stopBlocking(context: Context) {
        stand.set(Stand())
        BlockActivity.close(context)
    }

    // MARK: - Toestemmingen

    /** Staat onze toegankelijkheidsdienst aan? */
    fun isAccessibilityEnabled(context: Context): Boolean {
        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE)
            as? AccessibilityManager ?: return false
        val ours = ComponentName(context, TotemAccessibilityService::class.java)
        return manager
            .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any {
                val info = it.resolveInfo.serviceInfo
                ComponentName(info.packageName, info.name) == ours
            }
    }

    /** Mogen we over andere apps heen tekenen? */
    fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)

    /** Mogen we wekkers zetten die op de minuut afgaan? */
    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager
        return alarms?.canScheduleExactAlarms() ?: false
    }

    /** Alles wat nodig is om te kunnen blokkeren. */
    fun isReady(context: Context): Boolean =
        isAccessibilityEnabled(context) && canDrawOverlays(context)

    /**
     * Opent de lijst met toegankelijkheidsdiensten, met Totem uitgelicht.
     *
     * Aanzetten kan een app niet zelf: daarvoor is WRITE_SECURE_SETTINGS
     * nodig, en dat recht krijgt alleen een systeem-app. Het enige wat we
     * kunnen doen is de gebruiker zo dicht mogelijk bij de schakelaar
     * afleveren.
     *
     * De twee extra's hieronder zijn de sleutels waarmee de Instellingen-app
     * intern naar een rij springt. Ze staan niet in de documentatie, dus ze
     * kunnen bij een Android-versie zomaar verdwijnen; op Pixel en Samsung
     * lichten ze de rij 'Totem' op, elders worden ze genegeerd. Dat is
     * ongevaarlijk: dan opent gewoon de normale lijst, precies zoals eerst.
     */
    fun openAccessibilitySettings(context: Context) {
        val dienst = ComponentName(context, TotemAccessibilityService::class.java)
            .flattenToString()
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .putExtra(":settings:fragment_args_key", dienst)
            .putExtra(
                ":settings:show_fragment_args",
                Bundle().apply { putString(":settings:fragment_args_key", dienst) }
            )
        open(context, intent)
    }

    fun openOverlaySettings(context: Context) {
        open(
            context,
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
        )
    }

    /** Het scherm waar je exacte wekkers toestaat (Android 12 en nieuwer). */
    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        open(
            context,
            Intent(
                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                Uri.parse("package:${context.packageName}")
            )
        )
    }

    /**
     * Sommige fabrikanten (Xiaomi, Oppo, Samsung) doden achtergronddiensten om
     * batterij te sparen. Zonder uitzondering kan de blokkade stilvallen.
     */
    fun openBatterySettings(context: Context) {
        open(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }

    private fun open(context: Context, intent: Intent) {
        runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
