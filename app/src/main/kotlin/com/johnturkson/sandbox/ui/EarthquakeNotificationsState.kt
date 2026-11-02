package com.johnturkson.sandbox.ui

import androidx.compose.runtime.Stable

@Stable
interface EarthquakeNotificationsState {
    val notifications: EarthquakeNotificationState
    val monitoring: EarthquakeMonitoringState
}
