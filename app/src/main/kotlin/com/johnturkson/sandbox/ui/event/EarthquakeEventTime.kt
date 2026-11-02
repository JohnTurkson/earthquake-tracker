package com.johnturkson.sandbox.ui.event

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.johnturkson.sandbox.R
import java.util.Date

@Composable
internal fun earthquakeEventTime(item: EarthquakeMapItem): String? {
    val context = LocalContext.current
    LocalConfiguration.current
    return item.eventTime?.let { time ->
        val date = Date(time.toEpochMilliseconds())
        stringResource(
            R.string.earthquake_event_time,
            DateFormat.getMediumDateFormat(context).format(date),
            DateFormat.getTimeFormat(context).format(date),
        )
    } ?: item.snippet
}
