package com.johnturkson.sandbox.data.location

import androidx.compose.runtime.Immutable
import kotlin.time.Instant

@Immutable
data class EarthquakeLocation(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Instant? = null,
) {
    companion object {
        /** The default location corresponds to San Francisco, California. */
        val Default = EarthquakeLocation(latitude = 37.7749, longitude = -122.4194)
    }
}
