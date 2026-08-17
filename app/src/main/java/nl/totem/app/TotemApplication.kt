package nl.totem.app

import android.app.Application
import nl.totem.app.data.PinService
import nl.totem.app.data.SharedStore
import nl.totem.app.notify.NotificationService
import nl.totem.app.schedule.ActivityScheduler
import nl.totem.app.schedule.SessionEngine
import nl.totem.app.shield.ShieldService

/**
 * Alles wat één keer moet gebeuren zodra het proces start — ook als het niet
 * de gebruiker is die de app opent, maar een wekker of de blokkadedienst.
 */
class TotemApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        SharedStore.init(this)
        PinService.init(this)
        NotificationService.createChannels(this)

        // De blokkade weer oppakken waar hij was; het proces kan tussendoor
        // zijn opgeruimd zonder dat de sessie voorbij is.
        ShieldService.restore(this)
        SessionEngine.syncWithSchedule(this)
        ActivityScheduler.refresh(this)
    }
}
