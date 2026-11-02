package com.johnturkson.sandbox.work

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.map

@Singleton
class EarthquakeWorkScheduler @Inject constructor(private val manager: WorkManager) {
    val refreshWork = manager.getWorkInfosForUniqueWorkFlow(RefreshWork).map(::latest)
    val monitoringWork = manager.getWorkInfosForUniqueWorkFlow(MonitoringWork).map(::latest)

    fun refresh() {
        manager.enqueueUniqueWork(
            RefreshWork,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<EarthquakeRefreshWorker>()
                .addTag("requested:${System.currentTimeMillis()}")
                .build(),
        )
    }

    fun startMonitoring() {
        manager.enqueueUniqueWork(
            MonitoringWork,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<EarthquakeMonitoringWorker>()
                .addTag("requested:${System.currentTimeMillis()}")
                .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build(),
        )
    }

    fun stopMonitoring() {
        manager.cancelUniqueWork(MonitoringWork)
    }

    private fun latest(items: List<WorkInfo>): WorkInfo? = items.maxByOrNull { info ->
        info.tags.firstOrNull { it.startsWith("requested:") }?.substringAfter(':')?.toLongOrNull()
            ?: 0L
    }

    companion object {
        const val RefreshWork = "earthquake_refresh"
        const val MonitoringWork = "earthquake_monitoring"
        const val ResultKey = "earthquake_result"
    }
}
