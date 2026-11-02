package com.johnturkson.sandbox.ui.map.location

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.johnturkson.sandbox.R

@Composable
fun EarthquakeLocationButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    requiresSettings: Boolean = false,
) {
    FloatingActionButton(onClick = onClick, modifier = modifier) {
        Icon(
            Icons.Default.MyLocation,
            contentDescription =
                stringResource(
                    if (requiresSettings) {
                        R.string.location_settings
                    } else {
                        R.string.use_my_location
                    }
                ),
        )
    }
}
