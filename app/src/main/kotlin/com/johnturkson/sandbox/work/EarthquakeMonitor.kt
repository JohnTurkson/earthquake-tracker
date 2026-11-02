package com.johnturkson.sandbox.work

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.map

@Singleton
class EarthquakeMonitor @Inject constructor(private val manager: WorkManager) {
    val refreshWork = manager.getWorkInfosForUniqueWorkFlow(REFRESH_WORK).map { it.singleOrNull() }
    val monitoringWork =
        manager.getWorkInfosForUniqueWorkFlow(MONITORING_WORK).map { it.singleOrNull() }

    fun refresh() =
        manager.enqueueUniqueWork(
            REFRESH_WORK,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<EarthquakeRefreshWorker>().build(),
        )

    fun start() =
        manager.enqueueUniqueWork(
            MONITORING_WORK,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<EarthquakeMonitoringWorker>().build(),
        )

    fun stop() = manager.cancelUniqueWork(MONITORING_WORK)

    companion object {
        const val REFRESH_WORK = "earthquake_refresh"
        const val MONITORING_WORK = "earthquake_monitoring"
    }
}
