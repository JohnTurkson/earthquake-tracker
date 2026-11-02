package com.johnturkson.sandbox.data

import androidx.compose.runtime.Immutable
import kotlin.time.Instant

@Immutable
data class EarthquakeLocation(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Instant? = null,
) {
    companion object {
        val Default = EarthquakeLocation(latitude = 37.7749, longitude = -122.4194)
    }
}
