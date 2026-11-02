package com.johnturkson.sandbox.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource
import com.johnturkson.sandbox.R
import com.johnturkson.sandbox.data.EarthquakeDatabaseModel
import kotlin.time.Instant

@Immutable
data class EarthquakeMapItem(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val title: String?,
    val snippet: String? = null,
    val eventTime: Instant? = null,
    val magnitude: Double? = null,
    val place: String? = null,
)

internal fun EarthquakeDatabaseModel.toMapItem(): EarthquakeMapItem =
    EarthquakeMapItem(
        id = id,
        latitude = latitude,
        longitude = longitude,
        title = title?.replaceFirst(Regex("^M\\s+"), "M"),
        eventTime = time,
        magnitude = magnitude,
        place = place,
    )

@Composable
internal fun earthquakeTitle(item: EarthquakeMapItem): String =
    item.title
        ?: stringResource(
            R.string.earthquake_fallback_title,
            item.magnitude?.let { stringResource(R.string.earthquake_magnitude, it) }
                ?: stringResource(R.string.unknown_magnitude),
            item.place ?: stringResource(R.string.unknown_location),
        )
