package com.johnturkson.sandbox.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp

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
) {
    val selectedItem = items.find { it.id == selectedItemId }

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
        if (expanded) {
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
                    EarthquakeList(
                        items = items,
                        selectedItemId = selectedItemId,
                        onItemClick = onItemClick,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        listState = listState,
                        isSupportingPane = true,
                    )
                    if (selectedItem != null) {
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                        EarthquakeSelectionCard(
                            item = selectedItem,
                            onDismiss = onClearSelection,
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        )
                    }
                }
            }
        } else {
            if (selectedView == EarthquakeView.List) {
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
            } else if (selectedItem != null) {
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
        }
    }
}
