package com.johnturkson.sandbox.ui.notifications

import androidx.compose.runtime.Immutable

@Immutable
data class EarthquakeNotificationState(
    val isChecking: Boolean = true,
    val permissionGranted: Boolean = false,
    val appEnabled: Boolean = false,
    val monitoringChannelEnabled: Boolean = true,
    val alertsChannelEnabled: Boolean = true,
    val hasRequestedPermission: Boolean = false,
    val requiresSettings: Boolean = false,
    val alertsEnabled: Boolean = false,
    val minimumMagnitude: Int = 6,
    val settingsError: Boolean = false,
) {
    val canPreviewAlert: Boolean
        get() = !isChecking && permissionGranted && appEnabled && alertsChannelEnabled
}
