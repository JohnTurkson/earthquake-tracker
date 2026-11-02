package com.johnturkson.sandbox.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.await
import com.johnturkson.sandbox.data.notifications.EarthquakeNotifications
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@HiltWorker
class EarthquakeMonitoringWorker
@AssistedInject
constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val monitor: EarthquakeMonitor,
    private val notifications: EarthquakeNotifications,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        setForeground(notifications.foreground(id))
        while (currentCoroutineContext().isActive) {
            monitor.refresh().await()
            delay(1.minutes)
        }
        return Result.success()
    }
}
