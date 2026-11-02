package com.johnturkson.sandbox.ui

import androidx.compose.runtime.Stable

@Stable
interface EarthquakeState {
    val navKey: Destination.EarthquakeRoute
    val mapItems: List<EarthquakeMapItem>
    val selectedMapItemId: String?
}
