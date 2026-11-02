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
internal fun earthquakeTitle(item: EarthquakeMapItem): String {
    val title = item.title
    if (title != null) {
        return title
    }

    val magnitude = item.magnitude
    val magnitudeText =
        if (magnitude != null) {
            stringResource(R.string.earthquake_magnitude, magnitude)
        } else {
            stringResource(R.string.unknown_magnitude)
        }
    val placeText = item.place ?: stringResource(R.string.unknown_location)

    return stringResource(R.string.earthquake_fallback_title, magnitudeText, placeText)
}
