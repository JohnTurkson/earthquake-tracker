package com.johnturkson.sandbox.ui.list

import androidx.compose.runtime.Stable
import com.johnturkson.sandbox.ui.event.EarthquakeMapItem
import com.johnturkson.sandbox.ui.monitoring.EarthquakeMonitoringState
import com.johnturkson.sandbox.ui.navigation.EarthquakeSelectionResult

@Stable
interface EarthquakeListState {
    val mapItems: List<EarthquakeMapItem>
    val selectedMapItemId: String?
    val monitoring: EarthquakeMonitoringState
    val scrollRequest: EarthquakeSelectionResult?
}
