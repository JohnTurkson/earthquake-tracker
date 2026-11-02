package com.johnturkson.sandbox.ui

import androidx.compose.runtime.Stable

@Stable
interface MainState {
    val navKey: Destination.MainRoute
    val mapItems: List<EarthquakeMapItem>
    val selectedMapItemId: String?
}
