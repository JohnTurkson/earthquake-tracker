package com.johnturkson.sandbox.ui

import androidx.compose.runtime.Immutable

@Immutable
data class EarthquakeMonitoringState(
    val phase: Phase = Phase.Stopped,
    val refresh: Refresh = Refresh.Idle,
    val lastUpdatedMillis: Long? = null,
    val hasStoredData: Boolean = false,
) {
    enum class Phase {
        Checking,
        Stopped,
        Starting,
        Running,
        Offline,
        Failed,
    }

    enum class Refresh {
        Idle,
        Queued,
        Running,
        Offline,
        NetworkError,
        DataError,
        StorageError,
    }

    val isRefreshing: Boolean
        get() = refresh == Refresh.Queued || refresh == Refresh.Running

    val isMonitoring: Boolean
        get() = phase == Phase.Starting || phase == Phase.Running || phase == Phase.Offline
}
