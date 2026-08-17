package nl.totem.app.schedule

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Het vangnet onder de wekkers.
 *
 * Sommige toestellen — vooral van Xiaomi, Oppo en Huawei — laten exacte
 * wekkers weg om batterij te sparen. Deze controle draait elk kwartier en zet
 * recht wat er mist. Vijftien minuten is het minimum dat WorkManager toestaat.
 */
class ScheduleWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        SessionEngine.syncWithSchedule(applicationContext)
        return Result.success()
    }

    companion object {
        private const val NAME = "totem.schedule.check"

        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<ScheduleWorker>(15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
