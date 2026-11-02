package com.johnturkson.sandbox.ui

import androidx.compose.runtime.Immutable

@Immutable
data class EarthquakeMapItem(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val title: String,
    val snippet: String? = null,
)
