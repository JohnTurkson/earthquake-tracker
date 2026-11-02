package com.johnturkson.sandbox.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
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
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
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
    renderListContent: Boolean = true,
    onMapBottomControlsSizeChanged: (Int) -> Unit = {},
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
    onMoreDetails: ((String) -> Unit)? = null,
    detailsVisible: Boolean = false,
    snackbarHost: @Composable () -> Unit = {},
    showControls: Boolean = true,
    content: @Composable () -> Unit = {},
) {
    val selectedItem = items.find { it.id == selectedItemId }
    val showListPanel =
        showControls && expanded && isListPanelVisible && !notificationsVisible && !detailsVisible
    val showMapCard =
        showControls &&
            !detailsVisible &&
            (if (expanded) !showListPanel
            else selectedView == EarthquakeView.Map && !notificationsVisible)
    val showLocation =
        showControls &&
            showLocationButton &&
            (expanded || selectedView == EarthquakeView.Map) &&
            !notificationsVisible

    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current
    val showSelection = showMapCard && selectedItem != null
    var selectionHeight by remember { mutableIntStateOf(0) }
    var fabHeight by remember { mutableIntStateOf(0) }
    var snackbarHeight by remember { mutableIntStateOf(0) }
    LaunchedEffect(showLocation, showSelection, selectionHeight, fabHeight, snackbarHeight) {
        val bottomHeight = if (showSelection) selectionHeight else 0
        val locationHeight =
            if (showLocation) fabHeight + with(density) { 16.dp.roundToPx() } else 0
        onMapBottomControlsSizeChanged(bottomHeight + locationHeight + snackbarHeight)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets =
            WindowInsets(
                left = contentPadding.calculateLeftPadding(layoutDirection),
                top = contentPadding.calculateTopPadding(),
                right = contentPadding.calculateRightPadding(layoutDirection),
                bottom = contentPadding.calculateBottomPadding(),
            ),
        topBar = {
            if (expanded && showControls) {
                EarthquakeExplorerTopAppBar(
                    monitoring = monitoring,
                    isListPanelVisible = showListPanel,
                    onListPanelVisibilityChanged = {
                        onNotificationsClose()
                        onListPanelVisibilityChanged(it)
                    },
                    onRefresh = onRefresh,
                    onNotificationsClick = onNotificationsClick,
                    modifier =
                        Modifier.padding(
                            start = contentPadding.calculateStartPadding(layoutDirection),
                            end = contentPadding.calculateEndPadding(layoutDirection),
                            top = contentPadding.calculateTopPadding(),
                        ),
                )
            }
        },
        floatingActionButton = {
            if (showLocation) {
                EarthquakeLocationButton(
                    onClick = onLocationClick,
                    requiresSettings = requiresLocationSettings,
                    modifier = Modifier.onSizeChanged { fabHeight = it.height },
                )
            }
        },
        bottomBar = {
            if (showSelection) {
                Box(
                    Modifier.fillMaxWidth()
                        .padding(
                            start = contentPadding.calculateStartPadding(layoutDirection) + 16.dp,
                            end =
                                contentPadding.calculateEndPadding(layoutDirection) +
                                    if (expanded && notificationsVisible)
                                        earthquakeListPaneWidth + 32.dp
                                    else 16.dp,
                            top = 0.dp,
                            bottom = contentPadding.calculateBottomPadding() + 16.dp,
                        )
                        .onSizeChanged { selectionHeight = it.height }
                ) {
                    EarthquakeSelectionCard(
                        item = selectedItem,
                        onDismiss = onClearSelection,
                        onMoreDetails =
                            onMoreDetails?.let { callback -> { callback(selectedItem.id) } },
                        modifier =
                            Modifier.align(Alignment.Center).widthIn(max = 480.dp).fillMaxWidth(),
                    )
                }
            }
        },
        snackbarHost = {
            Box(
                Modifier.padding(
                        start =
                            if (showListPanel || expanded && detailsVisible)
                                earthquakeListPaneWidth + 32.dp
                            else 0.dp,
                        end =
                            if (expanded && notificationsVisible) earthquakeListPaneWidth + 32.dp
                            else 0.dp,
                    )
                    .onSizeChanged { snackbarHeight = it.height }
            ) {
                snackbarHost()
            }
        },
    ) { scaffoldPadding ->
        Box(Modifier.fillMaxSize().consumeWindowInsets(scaffoldPadding)) {
            content()
            if (showControls) {
                if (showListPanel && renderListContent) {
                    EarthquakeListPanel(
                        items,
                        selectedItemId,
                        onItemClick,
                        { onListPanelVisibilityChanged(false) },
                        listState,
                        Modifier.align(Alignment.CenterStart)
                            .padding(contentPadding)
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 80.dp)
                            .width(earthquakeListPaneWidth)
                            .fillMaxHeight(),
                        showMinimizeAction = false,
                        onMoreDetails = onMoreDetails?.let { callback -> { callback(it.id) } },
                    )
                } else if (
                    renderListContent &&
                        !expanded &&
                        selectedView == EarthquakeView.List &&
                        !notificationsVisible
                ) {
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
                        onMoreDetails = onMoreDetails?.let { callback -> { callback(it.id) } },
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
                }
                if (!expanded && !notificationsVisible) {
                    EarthquakeNotificationButton(
                        onNotificationsClick,
                        Modifier.align(Alignment.TopEnd)
                            .padding(contentPadding)
                            .padding(top = 12.dp, end = 16.dp),
                    )
                }
                if (
                    monitoring != null &&
                        !expanded &&
                        selectedView == EarthquakeView.Map &&
                        !notificationsVisible
                ) {
                    EarthquakeMonitoringCard(
                        monitoring,
                        onRefresh,
                        Modifier.align(Alignment.TopCenter)
                            .padding(contentPadding)
                            .padding(
                                start = 16.dp,
                                end = 16.dp,
                                top = 84.dp,
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
