package com.johnturkson.sandbox.ui

import androidx.compose.runtime.Stable
import com.johnturkson.sandbox.data.EarthquakeLocation

@Stable
interface EarthquakeState {
    val monitoring: EarthquakeMonitoringState
    val notifications: EarthquakeNotificationState
    val notificationsVisible: Boolean
    val requestedMapFocusId: String?
    val unavailableNotificationEvent: Boolean
    val navKey: Destination.EarthquakeRoute
    val mapItems: List<EarthquakeMapItem>
    val selectedMapItemId: String?
    val isListPanelVisible: Boolean
    val selectedView: EarthquakeView
    val initialMapLocation: EarthquakeLocation?
    val userLocation: EarthquakeLocation?
}
