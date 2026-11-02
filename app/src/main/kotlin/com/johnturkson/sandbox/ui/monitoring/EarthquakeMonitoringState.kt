package com.johnturkson.sandbox.ui.monitoring

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
        Failed,
    }

    enum class Refresh {
        Idle,
        Queued,
        Running,
        Succeeded,
        Failed,
        Cancelled,
    }

    val isRefreshing: Boolean
        get() = refresh == Refresh.Queued || refresh == Refresh.Running

    val isMonitoring: Boolean
        get() = status == Status.Starting || status == Status.Running
}
