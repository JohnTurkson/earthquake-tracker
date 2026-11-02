package com.johnturkson.sandbox.data

import androidx.compose.runtime.Immutable

@Immutable
data class EarthquakeLocation(
    val latitude: Double,
    val longitude: Double,
    val timestampMillis: Long? = null,
) {
    companion object {
        val Default = EarthquakeLocation(latitude = 37.7749, longitude = -122.4194)
    }
}
