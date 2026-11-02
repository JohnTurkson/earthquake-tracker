package com.johnturkson.sandbox.ui

import androidx.compose.runtime.Immutable
import kotlin.time.Instant

@Immutable
data class EarthquakeMonitoringState(
    val status: Status = Status.Stopped,
    val refresh: Refresh = Refresh.Idle,
    val lastUpdated: Instant? = null,
    val hasStoredData: Boolean = false,
) {
    enum class Status {
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
        Failed,
    }

    val isRefreshing: Boolean
        get() = refresh == Refresh.Queued || refresh == Refresh.Running

    val isMonitoring: Boolean
        get() = status == Status.Starting || status == Status.Running || status == Status.Offline
}
