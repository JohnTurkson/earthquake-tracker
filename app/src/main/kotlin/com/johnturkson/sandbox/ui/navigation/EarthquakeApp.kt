package com.johnturkson.sandbox.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.snap
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.result.rememberResultEventBusNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowSizeClass.Companion.HEIGHT_DP_MEDIUM_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import com.johnturkson.sandbox.R
import com.johnturkson.sandbox.theme.EarthquakeTheme
import com.johnturkson.sandbox.ui.details.EarthquakeDetailsScreen
import com.johnturkson.sandbox.ui.layout.EarthquakePaneOverrides
import com.johnturkson.sandbox.ui.layout.EarthquakeView
import com.johnturkson.sandbox.ui.layout.earthquakeListPaneWidth
import com.johnturkson.sandbox.ui.layout.earthquakeTopAppBarHeight
import com.johnturkson.sandbox.ui.list.EarthquakeListScreen
import com.johnturkson.sandbox.ui.list.EarthquakeListViewModel
import com.johnturkson.sandbox.ui.map.EarthquakeScreen
import com.johnturkson.sandbox.ui.map.EarthquakeViewModel
import com.johnturkson.sandbox.ui.navigation.Destination.EarthquakeDetailsRoute
import com.johnturkson.sandbox.ui.navigation.Destination.EarthquakeListRoute
import com.johnturkson.sandbox.ui.navigation.Destination.EarthquakeMapRoute
import com.johnturkson.sandbox.ui.navigation.Destination.EarthquakeNotificationsRoute
import com.johnturkson.sandbox.ui.notifications.EarthquakeNotificationsScreen

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
        val adaptiveInfo = currentWindowAdaptiveInfoV2()
        val backStack = rememberNavBackStack(EarthquakeListRoute, EarthquakeMapRoute)
        var compactView by rememberSaveable { mutableStateOf(EarthquakeView.Map) }
        var panelVisible by rememberSaveable { mutableStateOf(true) }
        var notificationsVisible by rememberSaveable { mutableStateOf(false) }
        var animateExplorerPanes by remember(adaptiveInfo) { mutableStateOf(true) }
        var clusterVisible by rememberSaveable { mutableStateOf(false) }
        var detailEarthquakeId by rememberSaveable { mutableStateOf<String?>(null) }
        var revision by rememberSaveable { mutableStateOf(0L) }
        var pendingMapId by rememberSaveable { mutableStateOf<String?>(null) }
        var pendingMapRevision by rememberSaveable { mutableStateOf<Long?>(null) }
        var pendingListId by rememberSaveable { mutableStateOf<String?>(null) }
        var pendingListRevision by rememberSaveable { mutableStateOf<Long?>(null) }
        fun publishSelection(id: String?, fromMap: Boolean): EarthquakeSelectionResult {
            revision++
            if (detailEarthquakeId != null) {
                detailEarthquakeId = id
            }
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
                animateExplorerPanes = false
                notificationsVisible = false
                detailEarthquakeId = null
                clusterVisible = false
                compactView = EarthquakeView.Map
            }
        }
        val expanded =
            adaptiveInfo.windowSizeClass.isAtLeastBreakpoint(
                WIDTH_DP_EXPANDED_LOWER_BOUND,
                HEIGHT_DP_MEDIUM_LOWER_BOUND,
            )
        BoxWithConstraints(modifier.fillMaxSize()) {
            val paneHeight =
                if (expanded) {
                    (maxHeight -
                            WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding() -
                            earthquakeTopAppBarHeight)
                        .coerceAtLeast(0.dp)
                } else {
                    maxHeight
                }
            val immediatePaneAnimation =
                SupportingPaneSceneStrategy.paneAnimation(
                    enterTransition = EnterTransition.None,
                    exitTransition = ExitTransition.None,
                    boundsAnimationSpec = snap(),
                )
            val explorerPaneAnimation =
                if (animateExplorerPanes && !notificationsVisible) {
                    emptyMap()
                } else {
                    immediatePaneAnimation
                }
            val listVisible =
                expanded &&
                    panelVisible &&
                    !notificationsVisible &&
                    !clusterVisible &&
                    detailEarthquakeId == null
            val detailsVisible =
                detailEarthquakeId != null && !notificationsVisible && !clusterVisible
            val showList =
                if (expanded) {
                    panelVisible && !clusterVisible
                } else {
                    compactView == EarthquakeView.List
                }
            LaunchedEffect(showList, notificationsVisible, detailEarthquakeId, clusterVisible) {
                val entries =
                    if (showList) {
                        listOf(EarthquakeMapRoute, EarthquakeListRoute)
                    } else {
                        listOf(EarthquakeListRoute, EarthquakeMapRoute)
                    }
                backStack.clear()
                backStack.addAll(entries)
                if (!clusterVisible) {
                    detailEarthquakeId?.let { backStack.add(EarthquakeDetailsRoute(it)) }
                }
                if (notificationsVisible) {
                    backStack.add(EarthquakeNotificationsRoute)
                }
            }
            val directive =
                calculatePaneScaffoldDirective(adaptiveInfo)
                    .copy(
                        maxHorizontalPartitions = 1,
                        maxVerticalPartitions = 1,
                        defaultPanePreferredHeight = paneHeight,
                    )
            val strategy =
                rememberSupportingPaneSceneStrategy<NavKey>(
                    shouldHandleSinglePaneLayout = true,
                    directive = directive,
                    adaptStrategies =
                        SupportingPaneScaffoldDefaults.adaptStrategies(
                            supportingPaneAdaptStrategy =
                                if (expanded) {
                                    AdaptStrategy.Levitate(Alignment.BottomStart)
                                } else {
                                    AdaptStrategy.Hide
                                },
                            extraPaneAdaptStrategy =
                                if (expanded) {
                                    AdaptStrategy.Levitate(Alignment.BottomEnd)
                                } else {
                                    AdaptStrategy.Hide
                                },
                        ),
                )
            val actions =
                EarthquakeNavigationActions(
                    expanded = expanded,
                    isListPanelVisible = listVisible,
                    notificationsVisible = notificationsVisible,
                    detailsVisible = detailsVisible,
                    onMoreDetails = {
                        animateExplorerPanes = !notificationsVisible
                        detailEarthquakeId = it
                        compactView = EarthquakeView.List
                        panelVisible = true
                        notificationsVisible = false
                        clusterVisible = false
                    },
                    onDetailsClose = {
                        animateExplorerPanes = true
                        detailEarthquakeId = null
                    },
                    onViewSelected = {
                        animateExplorerPanes = !notificationsVisible
                        compactView = it
                    },
                    onListPanelVisibilityChanged = {
                        animateExplorerPanes = !notificationsVisible
                        if (it) {
                            detailEarthquakeId = null
                        }
                        panelVisible = it
                        notificationsVisible = false
                    },
                    onNotificationsClick = {
                        animateExplorerPanes = false
                        notificationsVisible = true
                    },
                    onNotificationsClose = {
                        animateExplorerPanes = false
                        notificationsVisible = false
                    },
                    onClusterVisibilityChanged = {
                        if (clusterVisible != it) {
                            animateExplorerPanes = !notificationsVisible
                        }
                        clusterVisible = it
                    },
                    onMapSelection = { publishSelection(it, fromMap = true) },
                    onListSelection = { publishSelection(it, fromMap = false) },
                    pendingMapSelection =
                        pendingMapRevision?.let { EarthquakeSelectionResult(pendingMapId, it) },
                    pendingListSelection =
                        pendingListRevision?.let { EarthquakeSelectionResult(pendingListId, it) },
                    onMapSelectionConsumed = {
                        if (pendingMapRevision == it.revision) {
                            pendingMapRevision = null
                        }
                    },
                    onListSelectionConsumed = {
                        if (pendingListRevision == it.revision) {
                            pendingListRevision = null
                        }
                    },
                )
            val supportingPaneTitle =
                stringResource(
                    if (detailsVisible) {
                        R.string.earthquake_details
                    } else {
                        R.string.recent_earthquakes
                    }
                )
            EarthquakePaneOverrides(supportingPaneTitle = supportingPaneTitle) {
                NavDisplay(
                    backStack = backStack,
                    modifier = Modifier.fillMaxSize(),
                    onBack = {
                        when {
                            notificationsVisible -> {
                                animateExplorerPanes = false
                                notificationsVisible = false
                            }
                            detailEarthquakeId != null -> {
                                animateExplorerPanes = true
                                detailEarthquakeId = null
                            }
                            expanded && listVisible -> {
                                animateExplorerPanes = true
                                panelVisible = false
                            }
                            !expanded && compactView == EarthquakeView.List -> {
                                animateExplorerPanes = true
                                compactView = EarthquakeView.Map
                            }
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
                    transitionSpec = {
                        ContentTransform(EnterTransition.None, ExitTransition.None)
                    },
                    popTransitionSpec = {
                        ContentTransform(EnterTransition.None, ExitTransition.None)
                    },
                    predictivePopTransitionSpec = {
                        ContentTransform(EnterTransition.None, ExitTransition.None)
                    },
                    entryProvider =
                        entryProvider {
                            entry<EarthquakeMapRoute>(
                                metadata =
                                    SupportingPaneSceneStrategy.mainPane() + explorerPaneAnimation
                            ) { navKey ->
                                destinationContent(navKey, actions)
                            }
                            entry<EarthquakeListRoute>(
                                metadata =
                                    SupportingPaneSceneStrategy.supportingPane() +
                                        SupportingPaneSceneStrategy.preferredPaneSize(
                                            width = earthquakeListPaneWidth + 32.dp
                                        ) +
                                        explorerPaneAnimation
                            ) { navKey ->
                                destinationContent(navKey, actions)
                            }
                            entry<EarthquakeDetailsRoute>(
                                metadata =
                                    SupportingPaneSceneStrategy.supportingPane() +
                                        SupportingPaneSceneStrategy.preferredPaneSize(
                                            width = earthquakeListPaneWidth + 32.dp
                                        ) +
                                        explorerPaneAnimation
                            ) { navKey ->
                                destinationContent(navKey, actions)
                            }
                            entry<EarthquakeNotificationsRoute>(
                                metadata =
                                    SupportingPaneSceneStrategy.extraPane() +
                                        SupportingPaneSceneStrategy.preferredPaneSize(
                                            width = earthquakeListPaneWidth + 32.dp
                                        ) +
                                        immediatePaneAnimation
                            ) { navKey ->
                                destinationContent(navKey, actions)
                            }
                        },
                )
            }
            BackHandler(
                enabled =
                    !notificationsVisible &&
                        !clusterVisible &&
                        (detailsVisible ||
                            if (expanded) {
                                !listVisible
                            } else {
                                compactView == EarthquakeView.Map
                            })
            ) {
                if (detailsVisible) {
                    actions.onDetailsClose()
                } else {
                    activity?.finish()
                }
            }
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
        EarthquakeMapRoute -> {
            val viewModel =
                hiltViewModel<EarthquakeViewModel, EarthquakeViewModel.Factory>(
                    creationCallback = { it.create(EarthquakeMapRoute) }
                )
            EarthquakeSelectionEffect(
                resultKey = "map_selection",
                pending = actions.pendingMapSelection,
                onResult = { viewModel.applySelection(it) },
                onConsumed = actions.onMapSelectionConsumed,
            )
            EarthquakeScreen(
                navKey = EarthquakeMapRoute,
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
                detailsVisible = actions.detailsVisible,
                onMoreDetails = actions.onMoreDetails,
            )
        }
        EarthquakeListRoute -> {
            val viewModel =
                hiltViewModel<EarthquakeListViewModel, EarthquakeListViewModel.Factory>(
                    creationCallback = { it.create(EarthquakeListRoute) }
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
                    if (!actions.expanded) {
                        actions.onViewSelected(EarthquakeView.Map)
                    }
                },
                expanded = actions.expanded,
                onMinimize = { actions.onListPanelVisibilityChanged(false) },
                onViewSelected = actions.onViewSelected,
                onNotificationsClick = actions.onNotificationsClick,
                onRefresh = viewModel::refresh,
                onScrollHandled = viewModel::consumeScrollRequest,
                onMoreDetails = {
                    viewModel.applySelection(actions.onListSelection(it.id), scroll = false)
                    actions.onMoreDetails(it.id)
                },
            )
        }
        is EarthquakeDetailsRoute -> {
            EarthquakeDetailsScreen(
                navKey = navKey,
                onClose = actions.onDetailsClose,
                expanded = actions.expanded,
            )
        }
        EarthquakeNotificationsRoute -> {
            EarthquakeNotificationsScreen(
                navKey = EarthquakeNotificationsRoute,
                onClose = actions.onNotificationsClose,
                expanded = actions.expanded,
            )
        }
    }
}
