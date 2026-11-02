package com.johnturkson.sandbox.ui

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.johnturkson.sandbox.R

@Composable
fun EarthquakeSelectionCard(
    item: EarthquakeMapItem,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
    ) {
        Row(Modifier.padding(start = 20.dp, top = 12.dp, bottom = 20.dp, end = 8.dp)) {
            Column(
                Modifier.weight(1f).padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(earthquakeTitle(item), style = MaterialTheme.typography.titleMedium)
                earthquakeEventTime(item)?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    stringResource(R.string.earthquake_coordinates, item.latitude, item.longitude),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.clear_selection),
                )
            }
        }
    }
}
