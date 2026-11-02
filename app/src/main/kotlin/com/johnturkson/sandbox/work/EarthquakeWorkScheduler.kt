package com.johnturkson.sandbox.work

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.map

@Singleton
class EarthquakeWorkScheduler @Inject constructor(private val manager: WorkManager) {
    val refreshWork = manager.getWorkInfosForUniqueWorkFlow(RefreshWork).map { it.singleOrNull() }
    val monitoringWork =
        manager.getWorkInfosForUniqueWorkFlow(MonitoringWork).map { it.singleOrNull() }

    fun refresh() =
        manager.enqueueUniqueWork(
            RefreshWork,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<EarthquakeRefreshWorker>().build(),
        )

    fun startMonitoring() =
        manager.enqueueUniqueWork(
            MonitoringWork,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<EarthquakeMonitoringWorker>().build(),
        )

    fun stopMonitoring() = manager.cancelUniqueWork(MonitoringWork)

    companion object {
        const val RefreshWork = "earthquake_refresh"
        const val MonitoringWork = "earthquake_monitoring"
    }
}
