package com.johnturkson.sandbox.ui

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.johnturkson.sandbox.R
import java.util.Date

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
                top = if (showTitle) 16.dp else 0.dp,
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
                Modifier.padding(horizontal = 12.dp, vertical = if (showTitle) 12.dp else 0.dp)
            ) {
                if (showTitle)
                    Text(
                        stringResource(R.string.recent_earthquakes),
                        style =
                            if (isSupportingPane) MaterialTheme.typography.titleLarge
                            else MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                Text(
                    pluralStringResource(
                        R.plurals.earthquake_snapshot_count,
                        items.size,
                        items.size,
                    ),
                    modifier = Modifier.padding(top = if (showTitle) 8.dp else 0.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(
                        if (isSupportingPane) R.string.local_event_times
                        else R.string.list_explanation
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

@Composable
fun EarthquakeListRow(
    item: EarthquakeMapItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showMapAction: Boolean = true,
    onMoreDetails: (() -> Unit)? = null,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().semantics { selected = isSelected },
        shape = RoundedCornerShape(20.dp),
        color =
            if (isSelected) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surface,
    ) {
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Icon(
                    Icons.Outlined.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp).size(24.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(earthquakeTitle(item), style = MaterialTheme.typography.titleMedium)
                earthquakeEventTime(item)?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (onMoreDetails != null) {
                IconButton(onClick = onMoreDetails) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription =
                            stringResource(
                                R.string.more_earthquake_details_for,
                                earthquakeTitle(item),
                            ),
                    )
                }
            } else if (showMapAction) {
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
            }
        }
    }
}

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
