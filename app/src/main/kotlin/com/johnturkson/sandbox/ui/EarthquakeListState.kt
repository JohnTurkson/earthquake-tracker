package com.johnturkson.sandbox.ui

import androidx.compose.runtime.Stable

@Stable
interface EarthquakeListState {
    val mapItems: List<EarthquakeMapItem>
    val selectedMapItemId: String?
    val monitoring: EarthquakeMonitoringState
    val scrollRequest: EarthquakeSelectionResult?
}
