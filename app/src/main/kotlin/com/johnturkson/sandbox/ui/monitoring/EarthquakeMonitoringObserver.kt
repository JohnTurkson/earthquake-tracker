package com.johnturkson.sandbox.ui.monitoring

import androidx.work.WorkInfo
import com.johnturkson.sandbox.data.repository.EarthquakeRepository
import com.johnturkson.sandbox.work.EarthquakeMonitor
import javax.inject.Inject
import kotlinx.coroutines.flow.combine

class EarthquakeMonitoringObserver
@Inject
constructor(
    repository: EarthquakeRepository,
    monitor: EarthquakeMonitor,
) {
    private val work =
        combine(monitor.monitoringWork, monitor.refreshWork) { monitoring, refresh ->
            val monitoringStatus =
                when (monitoring?.state) {
                    WorkInfo.State.ENQUEUED,
                    WorkInfo.State.BLOCKED -> EarthquakeMonitoringState.Status.Starting
                    WorkInfo.State.RUNNING -> EarthquakeMonitoringState.Status.Running
                    WorkInfo.State.FAILED -> EarthquakeMonitoringState.Status.Failed
                    else -> EarthquakeMonitoringState.Status.Stopped
                }
            val refreshing =
                when (refresh?.state) {
                    WorkInfo.State.RUNNING -> EarthquakeMonitoringState.Refresh.Running
                    WorkInfo.State.ENQUEUED,
                    WorkInfo.State.BLOCKED -> EarthquakeMonitoringState.Refresh.Queued
                    WorkInfo.State.FAILED -> EarthquakeMonitoringState.Refresh.Failed
                    WorkInfo.State.SUCCEEDED -> EarthquakeMonitoringState.Refresh.Succeeded
                    else -> EarthquakeMonitoringState.Refresh.Idle
                }
            EarthquakeMonitoringState(status = monitoringStatus, refresh = refreshing)
        }

    val states =
        combine(work, repository.getEarthquakesFlow(), repository.getRefreshFlow()) {
            monitoring,
            items,
            metadata ->
            monitoring.copy(
                hasStoredData = items.isNotEmpty(),
                lastUpdated = metadata?.updatedAt,
            )
        }
}
