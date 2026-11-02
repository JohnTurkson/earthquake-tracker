package com.johnturkson.sandbox.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.AdaptStrategy
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldDefaults
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.SupportingPaneSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberSupportingPaneSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.result.rememberResultEventBusNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.johnturkson.sandbox.theme.EarthquakeTheme
import com.johnturkson.sandbox.ui.Destination.ListRoute
import com.johnturkson.sandbox.ui.Destination.MapRoute
import com.johnturkson.sandbox.ui.Destination.NotificationsRoute

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun EarthquakeApp(
    modifier: Modifier = Modifier,
    notificationEventId: String? = null,
    onNotificationEventConsumed: () -> Unit = {},
    destinationContent: @Composable (Destination, EarthquakeNavigationActions) -> Unit =
        { route, actions ->
            EarthquakeDestination(route, actions, notificationEventId, onNotificationEventConsumed)
        },
) {
    EarthquakeTheme {
        val activity = LocalActivity.current
        val backStack = rememberNavBackStack(ListRoute, MapRoute)
        var compactView by rememberSaveable { mutableStateOf(EarthquakeView.Map) }
        var panelVisible by rememberSaveable { mutableStateOf(true) }
        var notificationsVisible by rememberSaveable { mutableStateOf(false) }
        var clusterVisible by rememberSaveable { mutableStateOf(false) }
        var revision by rememberSaveable { mutableStateOf(0L) }
        var pendingMapId by rememberSaveable { mutableStateOf<String?>(null) }
        var pendingMapRevision by rememberSaveable { mutableStateOf<Long?>(null) }
        var pendingListId by rememberSaveable { mutableStateOf<String?>(null) }
        var pendingListRevision by rememberSaveable { mutableStateOf<Long?>(null) }
        fun publishSelection(id: String?, fromMap: Boolean): EarthquakeSelectionResult {
            revision++
            if (fromMap) {
                pendingMapRevision = null
                pendingListId = id
                pendingListRevision = revision
            } else {
                pendingListRevision = null
                pendingMapId = id
                pendingMapRevision = revision
            }
            return EarthquakeSelectionResult(id, revision)
        }
        LaunchedEffect(notificationEventId) {
            if (notificationEventId != null) {
                notificationsVisible = false
                clusterVisible = false
                compactView = EarthquakeView.Map
            }
        }
        BoxWithConstraints(modifier.fillMaxSize()) {
            val expanded = maxWidth >= 840.dp && maxHeight >= 480.dp
            val listVisible = expanded && panelVisible && !notificationsVisible && !clusterVisible
            val showList = if (expanded) listVisible else compactView == EarthquakeView.List
            LaunchedEffect(showList, notificationsVisible) {
                val entries =
                    if (showList) listOf(MapRoute, ListRoute) else listOf(ListRoute, MapRoute)
                backStack.clear()
                backStack.addAll(entries)
                if (notificationsVisible) backStack.add(NotificationsRoute)
            }
            val directive =
                calculatePaneScaffoldDirective(currentWindowAdaptiveInfoV2())
                    .copy(maxHorizontalPartitions = 1, maxVerticalPartitions = 1)
            val strategy =
                rememberSupportingPaneSceneStrategy<NavKey>(
                    shouldHandleSinglePaneLayout = true,
                    directive = directive,
                    adaptStrategies =
                        SupportingPaneScaffoldDefaults.adaptStrategies(
                            supportingPaneAdaptStrategy =
                                if (expanded) AdaptStrategy.Levitate(Alignment.CenterStart)
                                else AdaptStrategy.Hide,
                            extraPaneAdaptStrategy =
                                if (expanded) AdaptStrategy.Levitate(Alignment.CenterEnd)
                                else AdaptStrategy.Hide,
                        ),
                )
            val actions =
                EarthquakeNavigationActions(
                    expanded = expanded,
                    isListPanelVisible = listVisible,
                    notificationsVisible = notificationsVisible,
                    onViewSelected = { compactView = it },
                    onListPanelVisibilityChanged = {
                        panelVisible = it
                        notificationsVisible = false
                    },
                    onNotificationsClick = { notificationsVisible = true },
                    onNotificationsClose = { notificationsVisible = false },
                    onClusterVisibilityChanged = { clusterVisible = it },
                    onMapSelection = { publishSelection(it, fromMap = true) },
                    onListSelection = { publishSelection(it, fromMap = false) },
                    pendingMapSelection =
                        pendingMapRevision?.let { EarthquakeSelectionResult(pendingMapId, it) },
                    pendingListSelection =
                        pendingListRevision?.let { EarthquakeSelectionResult(pendingListId, it) },
                    onMapSelectionConsumed = {
                        if (pendingMapRevision == it.revision) pendingMapRevision = null
                    },
                    onListSelectionConsumed = {
                        if (pendingListRevision == it.revision) pendingListRevision = null
                    },
                )
            NavDisplay(
                backStack = backStack,
                modifier = Modifier.fillMaxSize(),
                onBack = {
                    when {
                        notificationsVisible -> notificationsVisible = false
                        expanded && listVisible -> panelVisible = false
                        !expanded && compactView == EarthquakeView.List ->
                            compactView = EarthquakeView.Map
                        else -> activity?.finish()
                    }
                },
                sceneStrategies = listOf(strategy),
                entryDecorators =
                    listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                        rememberResultEventBusNavEntryDecorator(),
                    ),
                transitionSpec = { ContentTransform(EnterTransition.None, ExitTransition.None) },
                popTransitionSpec = { ContentTransform(EnterTransition.None, ExitTransition.None) },
                predictivePopTransitionSpec = {
                    ContentTransform(EnterTransition.None, ExitTransition.None)
                },
                entryProvider =
                    entryProvider {
                        entry<MapRoute>(metadata = SupportingPaneSceneStrategy.mainPane()) { navKey
                            ->
                            destinationContent(navKey, actions)
                        }
                        entry<ListRoute>(
                            metadata =
                                SupportingPaneSceneStrategy.supportingPane() +
                                    SupportingPaneSceneStrategy.preferredPaneSize(
                                        width = earthquakeListPaneWidth + 32.dp
                                    ) +
                                    SupportingPaneSceneStrategy.preferredPaneSize(height = 1f)
                        ) { navKey ->
                            destinationContent(navKey, actions)
                        }
                        entry<NotificationsRoute>(
                            metadata =
                                SupportingPaneSceneStrategy.extraPane() +
                                    SupportingPaneSceneStrategy.preferredPaneSize(
                                        width = earthquakeListPaneWidth + 32.dp
                                    ) +
                                    SupportingPaneSceneStrategy.preferredPaneSize(height = 1f)
                        ) { navKey ->
                            destinationContent(navKey, actions)
                        }
                    },
            )
        }
    }
}

@Composable
private fun EarthquakeDestination(
    navKey: Destination,
    actions: EarthquakeNavigationActions,
    notificationEventId: String?,
    onNotificationEventConsumed: () -> Unit,
) {
    when (navKey) {
        MapRoute -> {
            val viewModel =
                hiltViewModel<EarthquakeViewModel, EarthquakeViewModel.Factory>(
                    creationCallback = { it.create(MapRoute) }
                )
            EarthquakeSelectionEffect(
                resultKey = "map_selection",
                pending = actions.pendingMapSelection,
                onResult = { viewModel.applySelection(it) },
                onConsumed = actions.onMapSelectionConsumed,
            )
            EarthquakeScreen(
                navKey = MapRoute,
                viewModel = viewModel,
                notificationEventId = notificationEventId,
                onNotificationEventConsumed = onNotificationEventConsumed,
                onSelectionChanged = actions.onMapSelection,
                onViewSelected = actions.onViewSelected,
                onListPanelVisibilityChanged = actions.onListPanelVisibilityChanged,
                onNotificationsClick = actions.onNotificationsClick,
                onClusterVisibilityChanged = actions.onClusterVisibilityChanged,
                isListPanelVisible = actions.isListPanelVisible,
                notificationsVisible = actions.notificationsVisible,
            )
        }
        ListRoute -> {
            val viewModel =
                hiltViewModel<EarthquakeListViewModel, EarthquakeListViewModel.Factory>(
                    creationCallback = { it.create(ListRoute) }
                )
            EarthquakeSelectionEffect(
                resultKey = "list_selection",
                pending = actions.pendingListSelection,
                onResult = { viewModel.applySelection(it) },
                onConsumed = actions.onListSelectionConsumed,
            )
            EarthquakeListScreen(
                state = viewModel,
                onItemClick = {
                    viewModel.applySelection(actions.onListSelection(it.id), scroll = false)
                    if (!actions.expanded) actions.onViewSelected(EarthquakeView.Map)
                },
                expanded = actions.expanded,
                onMinimize = { actions.onListPanelVisibilityChanged(false) },
                onViewSelected = actions.onViewSelected,
                onNotificationsClick = actions.onNotificationsClick,
                onRefresh = viewModel::refresh,
                onScrollHandled = viewModel::consumeScrollRequest,
            )
        }
        NotificationsRoute -> {
            EarthquakeNotificationsScreen(
                navKey = NotificationsRoute,
                onClose = actions.onNotificationsClose,
                modifier =
                    Modifier.fillMaxSize()
                        .padding(WindowInsets.safeDrawing.asPaddingValues())
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = if (actions.expanded) 84.dp else 12.dp,
                            bottom = 16.dp,
                        ),
            )
        }
    }
}
