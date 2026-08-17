package nl.totem.app.shield

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * De waakhond. Ziet welke app naar voren komt en grijpt in als die geblokkeerd
 * is.
 *
 * Dit is het Android-antwoord op wat op iOS de `ManagedSettingsStore` deed.
 * Twee dingen gebeuren er bij een geblokkeerde app, in deze volgorde:
 *
 * 1. Terug naar het startscherm, zodat de geblokkeerde app niet achter ons
 *    scherm blijft staan en meteen weer verschijnt als je op "terug" drukt.
 * 2. Het blokkadescherm openen — dat is wat de gebruiker ziet.
 *
 * Deze methode wordt bij élke schermwissel aangeroepen en heeft honderd
 * milliseconden. Daarom leest ze niets van schijf: alles staat klaar in
 * [ShieldService.huidigeStand].
 */
class TotemAccessibilityService : AccessibilityService() {

    /** Zo lang na een blokkade negeren we hetzelfde pakket, tegen geflikker. */
    private val cooldownMillis = 800L
    private var lastBlockedAt = 0L
    private var lastBlockedPackage: String? = null

    private val handler by lazy { Handler(mainLooper) }

    /**
     * De apps die we nooit tegenhouden. Eén keer bepaald bij het verbinden;
     * de lijst kost een paar milliseconden en mag niet in de hete route staan.
     */
    private var onaantastbaar: Set<String> = emptySet()

    override fun onServiceConnected() {
        super.onServiceConnected()
        onaantastbaar = Onaantastbaar.voor(this)
        ShieldService.restore(this)
        Log.d("Totem", "Toegankelijkheidsdienst verbonden")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val pakket = event.packageName?.toString() ?: return

        // Nooit onszelf, de launcher, de instellingen, de telefoon-app of het
        // toetsenbord. Zie Onaantastbaar voor waarom dat zo belangrijk is.
        if (pakket == packageName) return
        if (pakket in onaantastbaar) return

        val stand = ShieldService.huidigeStand
        if (pakket !in stand.packages) return

        val now = SystemClock.elapsedRealtime()
        if (pakket == lastBlockedPackage && now - lastBlockedAt < cooldownMillis) return
        lastBlockedAt = now
        lastBlockedPackage = pakket

        Log.i(LOG, "BLOKKEERT $pakket (eigen pakket=$packageName)")
        blokkeer(stand)
    }

    private fun blokkeer(stand: ShieldService.Stand) {
        // Eerst de geblokkeerde app wegduwen. Doen we dit ná het openen van ons
        // eigen scherm, dan zou de startknop óns scherm wegduwen.
        Log.i(LOG, "drukt op HOME")
        runCatching { performGlobalAction(GLOBAL_ACTION_HOME) }

        val intent = Intent(this, BlockActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
            )
            putExtra(BlockActivity.EXTRA_MODE_NAME, stand.modeName)
            putExtra(BlockActivity.EXTRA_STARTED_AT, stand.startedAt)
        }

        // Een korte tel wachten zodat het startscherm eerst klaar is; anders
        // schuift de animatie van "home" ons scherm er weer af.
        handler.postDelayed({
            runCatching { startActivity(intent) }
                .onFailure { Log.w("Totem", "Blokkadescherm openen mislukt", it) }
        }, 250)
    }

    override fun onInterrupt() { /* niets bij te onderbreken */ }

    companion object {
        /** `adb logcat -s TotemShield` laat zien wat de waakhond doet. */
        const val LOG = "TotemShield"
    }
}
