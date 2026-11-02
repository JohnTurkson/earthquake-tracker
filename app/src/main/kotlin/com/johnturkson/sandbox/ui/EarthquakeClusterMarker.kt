package com.johnturkson.sandbox.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.johnturkson.sandbox.R

@Composable
fun EarthquakeClusterMarker(
    count: Int,
    maximumMagnitude: Double?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.padding(bottom = 16.dp).defaultMinSize(48.dp, 48.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFB91C1C),
        contentColor = Color.White,
        border = BorderStroke(2.dp, Color.White),
    ) {
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(count.toString(), style = MaterialTheme.typography.titleMedium)
            maximumMagnitude?.let {
                Text(
                    stringResource(R.string.cluster_maximum_magnitude, it),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

internal fun Iterable<EarthquakeMapItem>.maximumMagnitude(): Double? = mapNotNull {
    it.magnitude?.takeIf(Double::isFinite)
}
    .maxOrNull()
