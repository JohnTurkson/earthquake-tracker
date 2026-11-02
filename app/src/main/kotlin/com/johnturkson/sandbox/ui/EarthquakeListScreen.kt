package com.johnturkson.sandbox.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun EarthquakeListScreen(
    state: EarthquakeListState,
    onItemClick: (EarthquakeMapItem) -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    onMinimize: () -> Unit = {},
    onViewSelected: (EarthquakeView) -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onScrollHandled: (EarthquakeSelectionResult) -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
) {
    val request = state.scrollRequest
    LaunchedEffect(request, state.mapItems) {
        if (request != null) {
            val index = state.mapItems.indexOfFirst { it.id == request.earthquakeId }
            if (index >= 0) {
                val listIndex = index + 2
                val layout = listState.layoutInfo
                val visible = layout.visibleItemsInfo.find { it.index == listIndex }
                val fullyVisible =
                    visible != null &&
                        visible.offset >= layout.viewportStartOffset &&
                        visible.offset + visible.size <= layout.viewportEndOffset
                if (!fullyVisible) listState.animateScrollToItem(listIndex)
                onScrollHandled(request)
            }
        }
    }
    val safePadding = WindowInsets.safeDrawing.asPaddingValues()
    if (expanded) {
        EarthquakeListPanel(
            items = state.mapItems,
            selectedItemId = state.selectedMapItemId,
            onItemClick = onItemClick,
            onMinimize = onMinimize,
            listState = listState,
            modifier = modifier.fillMaxSize().padding(safePadding).padding(16.dp),
            monitoring = state.monitoring,
            onRefresh = onRefresh,
        )
    } else {
        Box(
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(safePadding)
        ) {
            EarthquakeList(
                items = state.mapItems,
                selectedItemId = state.selectedMapItemId,
                onItemClick = onItemClick,
                modifier = Modifier.padding(top = 84.dp).fillMaxSize(),
                listState = listState,
                monitoring = state.monitoring,
                onRefresh = onRefresh,
            )
            EarthquakeExplorerControls(
                items = state.mapItems,
                selectedItemId = state.selectedMapItemId,
                selectedView = EarthquakeView.List,
                expanded = false,
                onItemClick = onItemClick,
                onClearSelection = {},
                onViewSelected = onViewSelected,
                renderListContent = false,
                onNotificationsClick = onNotificationsClick,
            )
        }
    }
}
