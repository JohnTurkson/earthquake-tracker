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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Notifications
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
    monitoring: EarthquakeMonitoringState? = null,
    onRefresh: () -> Unit = {},
    notificationsVisible: Boolean = false,
    onNotificationsClick: () -> Unit = {},
    onNotificationsClose: () -> Unit = {},
    notificationContent: @Composable (Modifier) -> Unit = {},
    onMonitoringCardSizeChanged: (Int) -> Unit = {},
) {
    val selectedItem = items.find { it.id == selectedItemId }
    val showListPanel = expanded && isListPanelVisible && !notificationsVisible
    val showMapCard =
        if (expanded) !showListPanel
        else selectedView == EarthquakeView.Map && !notificationsVisible

    Box(modifier.fillMaxSize()) {
        if (
            showLocationButton &&
                (expanded || selectedView == EarthquakeView.Map) &&
                !notificationsVisible
        ) {
            EarthquakeLocationButton(
                onClick = onLocationClick,
                requiresSettings = requiresLocationSettings,
                modifier =
                    Modifier.align(Alignment.TopEnd)
                        .padding(contentPadding)
                        .padding(
                            top = if (expanded) 16.dp else 84.dp,
                            end = if (expanded) 80.dp else 16.dp,
                        ),
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
                        monitoring = monitoring,
                        onRefresh = onRefresh,
                    )
                }
            }
        } else if (!expanded && selectedView == EarthquakeView.List && !notificationsVisible) {
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
                monitoring = monitoring,
                onRefresh = onRefresh,
            )
        }
        if (showMapCard && selectedItem != null) {
            EarthquakeSelectionCard(
                item = selectedItem,
                onDismiss = onClearSelection,
                modifier =
                    Modifier.align(Alignment.BottomCenter)
                        .padding(contentPadding)
                        .padding(
                            start = 16.dp,
                            end =
                                if (expanded && notificationsVisible)
                                    earthquakeListPaneWidth + 32.dp
                                else 16.dp,
                            top = 16.dp,
                            bottom = 16.dp,
                        )
                        .widthIn(max = 480.dp)
                        .fillMaxWidth()
                        .onSizeChanged { onSelectionCardSizeChanged(it.height) },
            )
        }
        if (!expanded && !notificationsVisible) {
            EarthquakeViewSwitcher(
                selectedView,
                onViewSelected,
                Modifier.align(Alignment.TopCenter)
                    .padding(contentPadding)
                    .padding(top = 12.dp, start = 80.dp, end = 80.dp)
                    .widthIn(max = 296.dp)
                    .fillMaxWidth(),
            )
        } else if (expanded && !showListPanel) {
            Surface(
                modifier =
                    Modifier.align(Alignment.TopStart).padding(contentPadding).padding(16.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp,
            ) {
                TextButton(
                    onClick = {
                        onNotificationsClose()
                        onListPanelVisibilityChanged(true)
                    },
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    Icon(Icons.AutoMirrored.Outlined.List, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.show_earthquake_list))
                }
            }
        }
        if (expanded || !notificationsVisible) {
            EarthquakeNotificationButton(
                onNotificationsClick,
                Modifier.align(Alignment.TopEnd)
                    .padding(contentPadding)
                    .padding(top = if (expanded) 16.dp else 12.dp, end = 16.dp),
            )
        }
        if (
            monitoring != null &&
                !showListPanel &&
                (expanded || selectedView == EarthquakeView.Map && !notificationsVisible)
        ) {
            EarthquakeMonitoringCard(
                monitoring,
                onRefresh,
                Modifier.align(Alignment.TopCenter)
                    .padding(contentPadding)
                    .padding(
                        start = 16.dp,
                        end =
                            if (expanded && notificationsVisible) earthquakeListPaneWidth + 32.dp
                            else 16.dp,
                        top = if (expanded) 84.dp else if (showLocationButton) 144.dp else 84.dp,
                    )
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .onSizeChanged { onMonitoringCardSizeChanged(it.height) },
            )
        }
        if (notificationsVisible) {
            notificationContent(
                Modifier.align(if (expanded) Alignment.CenterEnd else Alignment.Center)
                    .padding(contentPadding)
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = if (expanded) 84.dp else 12.dp,
                        bottom = 16.dp,
                    )
                    .then(
                        if (expanded) Modifier.width(earthquakeListPaneWidth)
                        else Modifier.fillMaxWidth()
                    )
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
fun EarthquakeNotificationButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
    ) {
        IconButton(onClick, Modifier.size(52.dp)) {
            Icon(Icons.Outlined.Notifications, stringResource(R.string.earthquake_alerts))
        }
    }
}
