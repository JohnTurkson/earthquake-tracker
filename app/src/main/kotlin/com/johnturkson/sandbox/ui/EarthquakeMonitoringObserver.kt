package com.johnturkson.sandbox.ui

import androidx.work.WorkInfo
import com.johnturkson.sandbox.data.EarthquakeConnection
import com.johnturkson.sandbox.data.EarthquakeRepository
import com.johnturkson.sandbox.work.EarthquakeWorkScheduler
import javax.inject.Inject
import kotlinx.coroutines.flow.combine

class EarthquakeMonitoringObserver
@Inject
constructor(
    repository: EarthquakeRepository,
    workScheduler: EarthquakeWorkScheduler,
    connectivity: EarthquakeConnection,
) {
    private val work =
        combine(
            workScheduler.monitoringWork,
            workScheduler.refreshWork,
            connectivity.online,
        ) { monitor, refresh, online ->
            val monitoringStatus =
                when (monitor?.state) {
                    WorkInfo.State.ENQUEUED,
                    WorkInfo.State.BLOCKED -> EarthquakeMonitoringState.Status.Starting
                    WorkInfo.State.RUNNING ->
                        if (online) EarthquakeMonitoringState.Status.Running
                        else EarthquakeMonitoringState.Status.Offline
                    WorkInfo.State.FAILED -> EarthquakeMonitoringState.Status.Failed
                    else -> EarthquakeMonitoringState.Status.Stopped
                }
            val refreshing =
                when (refresh?.state) {
                    WorkInfo.State.RUNNING -> EarthquakeMonitoringState.Refresh.Running
                    WorkInfo.State.ENQUEUED,
                    WorkInfo.State.BLOCKED -> EarthquakeMonitoringState.Refresh.Queued
                    WorkInfo.State.FAILED -> EarthquakeMonitoringState.Refresh.Failed
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
