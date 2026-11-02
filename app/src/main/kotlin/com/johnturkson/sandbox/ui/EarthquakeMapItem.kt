package com.johnturkson.sandbox.ui

import androidx.compose.runtime.Immutable
import kotlin.time.Instant

@Immutable
data class EarthquakeMapItem(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val title: String,
    val snippet: String? = null,
    val eventTime: Instant? = null,
    val magnitude: Double? = null,
)
