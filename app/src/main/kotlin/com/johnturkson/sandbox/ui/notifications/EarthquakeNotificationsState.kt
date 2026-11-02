package com.johnturkson.sandbox.ui.notifications

import androidx.compose.runtime.Stable
import com.johnturkson.sandbox.ui.monitoring.EarthquakeMonitoringState

@Stable
interface EarthquakeNotificationsState {
    val notifications: EarthquakeNotificationState
    val monitoring: EarthquakeMonitoringState
}
