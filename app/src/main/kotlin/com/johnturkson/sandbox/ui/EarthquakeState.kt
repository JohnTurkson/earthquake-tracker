package com.johnturkson.sandbox.ui

import androidx.compose.runtime.Stable
import com.johnturkson.sandbox.data.EarthquakeLocation

@Stable
interface EarthquakeState {
    val navKey: Destination.EarthquakeRoute
    val mapItems: List<EarthquakeMapItem>
    val selectedMapItemId: String?
    val selectedView: EarthquakeView
    val initialMapLocation: EarthquakeLocation?
    val userLocation: EarthquakeLocation?
}
