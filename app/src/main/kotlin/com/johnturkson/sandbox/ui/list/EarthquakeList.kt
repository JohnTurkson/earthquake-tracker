package com.johnturkson.sandbox.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.johnturkson.sandbox.R
import com.johnturkson.sandbox.ui.event.EarthquakeListRow
import com.johnturkson.sandbox.ui.event.EarthquakeMapItem
import com.johnturkson.sandbox.ui.monitoring.EarthquakeMonitoringCard
import com.johnturkson.sandbox.ui.monitoring.EarthquakeMonitoringState

@Composable
fun EarthquakeList(
    items: List<EarthquakeMapItem>,
    selectedItemId: String?,
    onItemClick: (EarthquakeMapItem) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    isSupportingPane: Boolean = false,
    showTitle: Boolean = true,
    monitoring: EarthquakeMonitoringState? = null,
    onRefresh: () -> Unit = {},
    onMoreDetails: ((EarthquakeMapItem) -> Unit)? = null,
) {
    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding =
            PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top =
                    if (showTitle) {
                        16.dp
                    } else {
                        0.dp
                    },
                bottom = 16.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (monitoring != null) {
            item(key = "monitoring") {
                EarthquakeMonitoringCard(
                    monitoring,
                    onRefresh,
                    Modifier.fillMaxWidth().padding(bottom = 12.dp),
                )
            }
        }
        item(key = "heading") {
            Column(
                Modifier.padding(
                    horizontal = 12.dp,
                    vertical =
                        if (showTitle) {
                            12.dp
                        } else {
                            0.dp
                        },
                )
            ) {
                if (showTitle) {
                    Text(
                        stringResource(R.string.recent_earthquakes),
                        style =
                            if (isSupportingPane) {
                                MaterialTheme.typography.titleLarge
                            } else {
                                MaterialTheme.typography.headlineMedium
                            },
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    pluralStringResource(
                        R.plurals.earthquake_snapshot_count,
                        items.size,
                        items.size,
                    ),
                    modifier =
                        Modifier.padding(
                            top =
                                if (showTitle) {
                                    8.dp
                                } else {
                                    0.dp
                                }
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(
                        if (isSupportingPane) {
                            R.string.local_event_times
                        } else {
                            R.string.list_explanation
                        }
                    ),
                    modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider()
            }
        }
        if (items.isEmpty()) {
            item(key = "empty") {
                Text(
                    stringResource(R.string.no_earthquakes),
                    Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        items(items, key = { it.id }) { item ->
            EarthquakeListRow(
                item = item,
                isSelected = item.id == selectedItemId,
                onClick = { onItemClick(item) },
                showMapAction = !isSupportingPane,
                onMoreDetails = onMoreDetails?.let { { it(item) } },
            )
        }
    }
}
