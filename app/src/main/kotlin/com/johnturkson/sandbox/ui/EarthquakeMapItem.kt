package com.johnturkson.sandbox.ui

import androidx.compose.runtime.Immutable
import com.johnturkson.sandbox.data.EarthquakeDatabaseModel
import java.util.Locale
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

internal fun EarthquakeDatabaseModel.toMapItem(): EarthquakeMapItem {
    val label = magnitude?.let { String.format(Locale.ROOT, "%.1f", it) } ?: "?"
    return EarthquakeMapItem(
        id = id,
        latitude = latitude,
        longitude = longitude,
        title =
            title?.replaceFirst(Regex("^M\\s+"), "M") ?: "M$label – ${place ?: "Unknown location"}",
        eventTime = time,
        magnitude = magnitude,
    )
}
