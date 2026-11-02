package com.johnturkson.sandbox.ui.details

import androidx.compose.runtime.Stable

@Stable
interface EarthquakeDetailsState {
    val isLoading: Boolean
    val item: EarthquakeDetailsItem?
}
