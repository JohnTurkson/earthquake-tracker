package com.johnturkson.sandbox.ui

import androidx.compose.runtime.Immutable
import kotlin.time.Instant

@Immutable
data class EarthquakeDetailsItem(
    val mapItem: EarthquakeMapItem,
    val depth: Double,
    val updated: Instant,
    val place: String? = null,
    val magnitudeType: String? = null,
    val status: String? = null,
    val felt: Int? = null,
    val communityIntensity: Double? = null,
    val network: String? = null,
    val url: String? = null,
    val shakingMapUrl: String? = null,
    val feltMapUrl: String? = null,
)
