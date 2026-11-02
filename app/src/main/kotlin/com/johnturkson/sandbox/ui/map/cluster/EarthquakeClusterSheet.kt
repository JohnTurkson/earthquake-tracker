package com.johnturkson.sandbox.ui.map.cluster

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass.Companion.HEIGHT_DP_MEDIUM_LOWER_BOUND
import com.johnturkson.sandbox.R
import com.johnturkson.sandbox.ui.event.EarthquakeListRow
import com.johnturkson.sandbox.ui.event.EarthquakeMapItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EarthquakeClusterSheet(
    items: List<EarthquakeMapItem>,
    selectedItemId: String?,
    onItemClick: (EarthquakeMapItem) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val windowHeight =
        with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val maximumHeight =
        windowHeight *
            if (windowSizeClass.isHeightAtLeastBreakpoint(HEIGHT_DP_MEDIUM_LOWER_BOUND)) {
                0.6f
            } else {
                0.45f
            }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        EarthquakeClusterSheetContent(
            items = items,
            selectedItemId = selectedItemId,
            onItemClick = onItemClick,
            onDismiss = onDismiss,
            modifier = modifier.heightIn(max = maximumHeight),
        )
    }
}

@Composable
fun EarthquakeClusterSheetContent(
    items: List<EarthquakeMapItem>,
    selectedItemId: String?,
    onItemClick: (EarthquakeMapItem) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                pluralStringResource(R.plurals.cluster_earthquake_count, items.size, items.size),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall,
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Outlined.Close, stringResource(R.string.close_cluster))
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f, fill = false),
            state = listState,
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (items.isNotEmpty()) {
                item(key = "summary") {
                    Column(
                        Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            stringResource(R.string.cluster_explanation),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        items.maximumMagnitude()?.let {
                            Text(
                                stringResource(R.string.cluster_maximum_magnitude, it),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }
            if (items.isEmpty()) {
                item(key = "empty") {
                    Text(
                        stringResource(R.string.cluster_no_longer_available),
                        Modifier.padding(12.dp),
                    )
                }
            }
            items(items, key = { it.id }) { item ->
                EarthquakeListRow(
                    item = item,
                    isSelected = item.id == selectedItemId,
                    onClick = { onItemClick(item) },
                    showMapAction = false,
                )
            }
        }
    }
}
