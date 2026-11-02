package com.johnturkson.sandbox.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.johnturkson.sandbox.R

internal val earthquakeListPaneWidth = 360.dp

@Composable
fun EarthquakeExplorerControls(
    items: List<EarthquakeMapItem>,
    selectedItemId: String?,
    selectedView: EarthquakeView,
    expanded: Boolean,
    onItemClick: (EarthquakeMapItem) -> Unit,
    onClearSelection: () -> Unit,
    onViewSelected: (EarthquakeView) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    listState: LazyListState = rememberLazyListState(),
    onSelectionCardSizeChanged: (Int) -> Unit = {},
    showLocationButton: Boolean = false,
    requiresLocationSettings: Boolean = false,
    onLocationClick: () -> Unit = {},
    isListPanelVisible: Boolean = true,
    onListPanelVisibilityChanged: (Boolean) -> Unit = {},
) {
    val selectedItem = items.find { it.id == selectedItemId }
    val showListPanel = expanded && isListPanelVisible
    val showMapCard = if (expanded) !isListPanelVisible else selectedView == EarthquakeView.Map

    Box(modifier.fillMaxSize()) {
        if (showLocationButton && (expanded || selectedView == EarthquakeView.Map)) {
            EarthquakeLocationButton(
                onClick = onLocationClick,
                requiresSettings = requiresLocationSettings,
                modifier =
                    Modifier.align(Alignment.TopEnd)
                        .padding(contentPadding)
                        .padding(top = if (expanded) 16.dp else 84.dp, end = 16.dp),
            )
        }
        if (showListPanel) {
            Surface(
                modifier =
                    Modifier.align(Alignment.CenterStart)
                        .padding(contentPadding)
                        .padding(16.dp)
                        .width(earthquakeListPaneWidth)
                        .fillMaxHeight(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp,
            ) {
                Column {
                    Row(
                        Modifier.padding(start = 28.dp, end = 12.dp, top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.recent_earthquakes),
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        IconButton(onClick = { onListPanelVisibilityChanged(false) }) {
                            Icon(
                                Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                                contentDescription =
                                    stringResource(R.string.minimize_earthquake_list),
                            )
                        }
                    }
                    EarthquakeList(
                        items = items,
                        selectedItemId = selectedItemId,
                        onItemClick = onItemClick,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        listState = listState,
                        isSupportingPane = true,
                        showTitle = false,
                    )
                }
            }
        } else if (!expanded && selectedView == EarthquakeView.List) {
            EarthquakeList(
                items = items,
                selectedItemId = selectedItemId,
                onItemClick = onItemClick,
                modifier =
                    Modifier.align(Alignment.TopCenter)
                        .padding(contentPadding)
                        .padding(top = 84.dp)
                        .fillMaxSize(),
                listState = listState,
            )
        }
        if (showMapCard && selectedItem != null) {
            EarthquakeSelectionCard(
                item = selectedItem,
                onDismiss = onClearSelection,
                modifier =
                    Modifier.align(Alignment.BottomCenter)
                        .padding(contentPadding)
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .widthIn(max = 480.dp)
                        .fillMaxWidth()
                        .onSizeChanged { onSelectionCardSizeChanged(it.height) },
            )
        }
        if (!expanded) {
            EarthquakeViewSwitcher(
                selectedView = selectedView,
                onViewSelected = onViewSelected,
                modifier =
                    Modifier.align(Alignment.TopCenter)
                        .padding(contentPadding)
                        .padding(top = 12.dp)
                        .widthIn(max = 280.dp)
                        .fillMaxWidth(),
            )
        } else if (!isListPanelVisible) {
            Surface(
                modifier =
                    Modifier.align(Alignment.TopStart).padding(contentPadding).padding(16.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp,
            ) {
                TextButton(
                    onClick = { onListPanelVisibilityChanged(true) },
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    Icon(Icons.AutoMirrored.Outlined.List, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.show_earthquake_list))
                }
            }
        }
    }
}
