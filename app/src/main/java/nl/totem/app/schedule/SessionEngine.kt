package nl.totem.app.schedule

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import nl.totem.app.data.SharedStore
import nl.totem.app.model.ActiveSession
import nl.totem.app.model.FocusMode
import nl.totem.app.model.ScheduleEnd
import nl.totem.app.model.SessionLog
import nl.totem.app.notify.NotificationService
import nl.totem.app.shield.SessionService
import nl.totem.app.shield.ShieldService
import nl.totem.app.widget.TotemWidget

/**
 * Het hart van de app: hier begint en eindigt een sessie, waar de opdracht ook
 * vandaan komt — een tik op de Totem, een wekker of het vangnet.
 *
 * Op iOS was hier een `SessionBridge` voor nodig: de extensie kon de blokkade
 * wel aanzetten, maar niet bij de staat van de app, dus liet ze een briefje
 * achter dat de app later ophaalde. Op Android draait alles in hetzelfde
 * proces en bij dezelfde opslag, dus dat tussenstation is niet meer nodig.
 * Iedereen roept gewoon dit ene punt aan.
 */
object SessionEngine {

    /**
     * Gaat omhoog bij elke wijziging. De UI luistert hier en herleest dan de
     * opslag; zo blijft er precies één bron van waarheid.
     */
    private val _revision = MutableStateFlow(0L)
    val revision: StateFlow<Long> = _revision

    /** Voor werk dat na een wijziging nog moet gebeuren, zoals de widget. */
    private val achtergrond = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private fun changed(context: Context) {
        _revision.value = _revision.value + 1
        // De widget leest de opslag zelf, maar moet wel horen dát er iets
        // veranderd is — anders staat hij tot een half uur achter.
        achtergrond.launch {
            runCatching { TotemWidget().updateAll(context.applicationContext) }
        }
    }

    // MARK: - Starten en stoppen

    /**
     * Start een modus.
     *
     * @param bySchedule of dit door een wekker komt in plaats van door een tik.
     * @return false als er niet geblokkeerd kon worden.
     */
    fun start(context: Context, mode: FocusMode, bySchedule: Boolean): Boolean {
        SharedStore.init(context)
        if (!mode.isConfigured) return false
        if (SharedStore.session != null) return false

        val startedAt = System.currentTimeMillis()
        if (!ShieldService.startBlocking(context, mode, startedAt)) {
            // Toestemming ontbreekt. Bij een schema staat er niemand voor het
            // scherm, dus laten we het via een melding weten.
            if (bySchedule) {
                NotificationService.notifyCannotBlock(context, mode.name)
            }
            return false
        }

        val session = ActiveSession(
            modeID = mode.id,
            startedAt = startedAt,
            startedBySchedule = bySchedule
        )
        SharedStore.session = session

        SessionService.start(context, mode.name, session.startedAt)

        if (bySchedule) {
            val until = (mode.schedule.end as? ScheduleEnd.Time)
                ?.let { "%02d:%02d".format(it.hour, it.minute) }
            NotificationService.notifyStarted(context, mode.name, until)
        }

        changed(context)
        return true
    }

    /**
     * Beëindigt de lopende sessie.
     *
     * @param bySchedule of de eindtijd is bereikt in plaats van dat er getikt is
     */
    fun stop(context: Context, bySchedule: Boolean) {
        SharedStore.init(context)
        val session = SharedStore.session ?: return
        val mode = SharedStore.mode(session.modeID)

        android.util.Log.i("TotemShield", "sessie stopt (bySchedule=$bySchedule)")
        ShieldService.stopBlocking(context)
        SessionService.stop(context)

        // In de geschiedenis zetten, zodat de statistieken kloppen.
        val duration = (System.currentTimeMillis() - session.startedAt) / 1000
        if (duration > 0) {
            SharedStore.history = SharedStore.history + SessionLog(
                modeName = mode?.name ?: "Onbekend",
                startedAt = session.startedAt,
                duration = duration
            )
        }

        // Tik je een gepláánde blokkade zelf weg, dan komt hij dit venster niet
        // meer terug — anders zou hij meteen na je tik opnieuw aanslaan.
        //
        // Let op de voorwaarde `startedBySchedule`: heb je de modus zelf eerder
        // gestart dan het schema, dan hoort het schema gewoon nog te lopen als
        // je stopt. Anders zou een vroege start je hele werkdag uitzetten.
        if (!bySchedule && session.startedBySchedule && mode != null && mode.schedule.isOn) {
            SharedStore.markDismissed(mode.id, System.currentTimeMillis())
        }

        SharedStore.session = null

        if (bySchedule && mode != null) {
            NotificationService.notifyEnded(context, mode.name)
        }

        changed(context)
    }

    // MARK: - Vangnet

    /**
     * Kijkt of de werkelijkheid nog klopt met de schema's, en trekt hem recht.
     *
     * Wordt aangeroepen bij het openen van de app, bij elke wekker, na een
     * herstart van de telefoon en elk kwartier door de WorkManager.
     */
    fun syncWithSchedule(context: Context) {
        SharedStore.init(context)
        val modes = SharedStore.modes
        val session = SharedStore.session
        val nu = System.currentTimeMillis()

        // 1. Loopt er iets dat had moeten stoppen?
        if (session != null) {
            val mode = SharedStore.mode(session.modeID)
            if (mode == null) {
                stop(context, bySchedule = true)
            } else if (session.startedBySchedule && ScheduleService.shouldStop(mode, nu)) {
                stop(context, bySchedule = true)
            } else {
                // De blokkade in geheugen kan zijn weggevallen na een herstart.
                ShieldService.restore(context)
            }
            return
        }

        // 2. Hoort er iets te lopen dat nog niet loopt?
        val zou = ScheduleService.modeThatShouldRun(modes, nu, SharedStore.dismissed)
        if (zou != null) start(context, zou, bySchedule = true)
    }
}
