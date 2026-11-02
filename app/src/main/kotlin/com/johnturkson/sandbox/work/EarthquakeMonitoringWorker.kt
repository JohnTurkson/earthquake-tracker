package com.johnturkson.sandbox.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.await
import com.johnturkson.sandbox.data.EarthquakeConnection
import com.johnturkson.sandbox.data.EarthquakeNotifications
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

@HiltWorker
class EarthquakeMonitoringWorker
@AssistedInject
constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val scheduler: EarthquakeWorkScheduler,
    private val connectivity: EarthquakeConnection,
    private val notifications: EarthquakeNotifications,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        try {
            setForeground(notifications.foreground(id))
            EarthquakePollingLoop()
                .run(
                    online = connectivity.online,
                    onPaused = { setForeground(notifications.foreground(id, offline = true)) },
                    onTick = {
                        setForeground(notifications.foreground(id))
                        scheduler.refresh().await()
                    },
                )
            return Result.success()
        } catch (error: CancellationException) {
            throw error
        } catch (_: IllegalStateException) {
            return Result.failure()
        } catch (_: SecurityException) {
            return Result.failure()
        }
    }
}
