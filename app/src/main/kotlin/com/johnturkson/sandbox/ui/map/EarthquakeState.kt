package com.johnturkson.sandbox.ui.map

import androidx.compose.runtime.Stable
import com.johnturkson.sandbox.data.location.EarthquakeLocation
import com.johnturkson.sandbox.ui.event.EarthquakeMapItem
import com.johnturkson.sandbox.ui.monitoring.EarthquakeMonitoringState
import com.johnturkson.sandbox.ui.navigation.Destination

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
