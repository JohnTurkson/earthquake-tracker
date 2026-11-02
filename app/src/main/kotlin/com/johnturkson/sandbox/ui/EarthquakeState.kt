package com.johnturkson.sandbox.ui

import androidx.compose.runtime.Stable
import com.johnturkson.sandbox.data.EarthquakeLocation

@Stable
interface EarthquakeState {
    val monitoring: EarthquakeMonitoringState
    val requestedMapFocusId: String?
    val unavailableNotificationEvent: Boolean
    val navKey: Destination.EarthquakeMapRoute
    val mapItems: List<EarthquakeMapItem>
    val selectedMapItemId: String?
    val selectedClusterItemIds: List<String>
    val initialMapLocation: EarthquakeLocation?
    val userLocation: EarthquakeLocation?
}
