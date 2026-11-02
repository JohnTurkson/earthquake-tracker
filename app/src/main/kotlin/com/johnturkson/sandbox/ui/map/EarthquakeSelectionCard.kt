package com.johnturkson.sandbox.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.johnturkson.sandbox.R
import com.johnturkson.sandbox.ui.event.EarthquakeMapItem
import com.johnturkson.sandbox.ui.event.earthquakeEventTime
import com.johnturkson.sandbox.ui.event.earthquakeTitle

@Composable
fun EarthquakeSelectionCard(
    item: EarthquakeMapItem,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onMoreDetails: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
    ) {
        EarthquakeSelectionHeader(item, onDismiss, onMoreDetails = onMoreDetails)
    }
}

@Composable
internal fun EarthquakeSelectionHeader(
    item: EarthquakeMapItem,
    onDismiss: () -> Unit,
    onMoreDetails: (() -> Unit)? = null,
) {
    Row(Modifier.padding(start = 20.dp, top = 12.dp, bottom = 20.dp, end = 8.dp)) {
        Column(
            Modifier.weight(1f).padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(earthquakeTitle(item), style = MaterialTheme.typography.titleMedium)
            earthquakeEventTime(item)?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            Text(
                stringResource(R.string.earthquake_coordinates, item.latitude, item.longitude),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (onMoreDetails != null) {
                TextButton(onClick = onMoreDetails) {
                    Text(stringResource(R.string.more_earthquake_details))
                }
            }
        }
        IconButton(onClick = onDismiss) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = stringResource(R.string.clear_selection),
            )
        }
    }
}
