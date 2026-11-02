package com.johnturkson.sandbox.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.window.core.layout.WindowSizeClass.Companion.HEIGHT_DP_MEDIUM_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import com.johnturkson.sandbox.R
import com.johnturkson.sandbox.data.EarthquakeLocation

@Composable
fun EarthquakeScreen(
    navKey: Destination.EarthquakeMapRoute,
    onSelectionChanged: (String?) -> EarthquakeSelectionResult,
    onViewSelected: (EarthquakeView) -> Unit,
    onListPanelVisibilityChanged: (Boolean) -> Unit,
    onNotificationsClick: () -> Unit,
    onClusterVisibilityChanged: (Boolean) -> Unit,
    isListPanelVisible: Boolean,
    notificationsVisible: Boolean,
    modifier: Modifier = Modifier,
    notificationEventId: String? = null,
    onNotificationEventConsumed: () -> Unit = {},
    viewModel: EarthquakeViewModel =
        hiltViewModel<EarthquakeViewModel, EarthquakeViewModel.Factory>(
            creationCallback = { factory -> factory.create(navKey) }
        ),
    onMoreDetails: (String) -> Unit = {},
    detailsVisible: Boolean = false,
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    var isLocationGranted by remember { mutableStateOf(hasPermission()) }
    var hasRequestedLocation by rememberSaveable { mutableStateOf(false) }
    var shouldShowRationale by remember { mutableStateOf(false) }
    var showLocationRationale by rememberSaveable { mutableStateOf(false) }
    var isLocatingUser by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            isLocationGranted = granted
            isLocatingUser = granted
            shouldShowRationale =
                activity != null &&
                    ActivityCompat.shouldShowRequestPermissionRationale(
                        activity,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    )
        }
    LifecycleResumeEffect(Unit) {
        isLocationGranted = hasPermission()
        shouldShowRationale =
            activity != null &&
                ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                )
        onPauseOrDispose {}
    }
    LifecycleStartEffect(isLocationGranted) {
        if (isLocationGranted) {
            viewModel.startLocationUpdates()
        }
        onStopOrDispose { viewModel.stopLocationUpdates() }
    }
    LaunchedEffect(notificationEventId) {
        if (notificationEventId != null) {
            viewModel.openNotificationEvent(notificationEventId) { id ->
                viewModel.applySelection(onSelectionChanged(id))
            }
            onNotificationEventConsumed()
        }
    }
    val snackbarHost = remember { SnackbarHostState() }
    val unavailableMessage = stringResource(R.string.event_unavailable)
    LaunchedEffect(viewModel.unavailableNotificationEvent) {
        if (viewModel.unavailableNotificationEvent) {
            snackbarHost.showSnackbar(unavailableMessage)
            viewModel.dismissUnavailableEvent()
        }
    }
    LaunchedEffect(viewModel.selectedClusterItemIds) {
        onClusterVisibilityChanged(viewModel.selectedClusterItemIds.isNotEmpty())
    }
    val requiresSettings = hasRequestedLocation && !shouldShowRationale
    EarthquakeScreen(
        state = viewModel,
        onMapItemClick = { item ->
            viewModel.applySelection(onSelectionChanged(item.id), focus = false)
        },
        onMapClick = { viewModel.applySelection(onSelectionChanged(null), focus = false) },
        onClusterClick = viewModel::selectMapCluster,
        onClusterDismiss = viewModel::dismissMapCluster,
        onViewSelected = onViewSelected,
        onListPanelVisibilityChanged = onListPanelVisibilityChanged,
        onNotificationsClick = onNotificationsClick,
        isListPanelVisible = isListPanelVisible,
        notificationsVisible = notificationsVisible,
        detailsVisible = detailsVisible,
        onMoreDetails = onMoreDetails,
        renderListContent = false,
        onRefresh = viewModel::refresh,
        onMapFocusHandled = viewModel::consumeMapFocus,
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHost, snackbar = { EarthquakeEventSnackbar(it) }) },
        isLocationGranted = isLocationGranted,
        requiresLocationSettings = !isLocationGranted && requiresSettings,
        isLocatingUser = isLocatingUser,
        onUserLocated = { isLocatingUser = false },
        onLocationClick = {
            isLocatingUser = true
            when {
                hasPermission() -> isLocationGranted = true
                shouldShowRationale -> showLocationRationale = true
                requiresSettings ->
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            "package:${context.packageName}".toUri(),
                        )
                    )
                else -> {
                    hasRequestedLocation = true
                    permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
            }
        },
    )
    if (showLocationRationale && !isLocationGranted) {
        EarthquakeLocationRationale(
            onContinue = {
                showLocationRationale = false
                hasRequestedLocation = true
                permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            },
            onDismiss = {
                showLocationRationale = false
                isLocatingUser = false
            },
        )
    }
}

@Composable
fun EarthquakeScreen(
    state: EarthquakeState,
    onMapItemClick: (EarthquakeMapItem) -> Unit,
    onMapClick: () -> Unit,
    modifier: Modifier = Modifier,
    onViewSelected: (EarthquakeView) -> Unit = {},
    onListPanelVisibilityChanged: (Boolean) -> Unit = {},
    isLocationGranted: Boolean = false,
    requiresLocationSettings: Boolean = false,
    onLocationClick: () -> Unit = {},
    isLocatingUser: Boolean = false,
    onUserLocated: () -> Unit = {},
    onClusterClick: (List<EarthquakeMapItem>) -> Unit = {},
    onClusterDismiss: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onNotificationsClose: () -> Unit = {},
    onMapFocusHandled: () -> Unit = {},
    selectedView: EarthquakeView = EarthquakeView.Map,
    isListPanelVisible: Boolean = true,
    notificationsVisible: Boolean = false,
    renderListContent: Boolean = true,
    notificationContent: @Composable (Modifier) -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    cameraPositionState: CameraPositionState = rememberCameraPositionState {
        val location = EarthquakeLocation.Default
        position = CameraPosition.fromLatLngZoom(LatLng(location.latitude, location.longitude), 10f)
    },
    onMoreDetails: ((String) -> Unit)? = null,
    detailsVisible: Boolean = false,
) {
    var hasInitializedCamera by rememberSaveable { mutableStateOf(false) }
    val initialMapLocation = state.initialMapLocation
    LaunchedEffect(initialMapLocation) {
        if (initialMapLocation != null && !hasInitializedCamera) {
            if (cameraPositionState.cameraMoveStartedReason != CameraMoveStartedReason.GESTURE) {
                cameraPositionState.position =
                    CameraPosition.fromLatLngZoom(
                        LatLng(initialMapLocation.latitude, initialMapLocation.longitude),
                        10f,
                    )
            }
            hasInitializedCamera = true
        }
    }
    val userLocation = state.userLocation
    LaunchedEffect(isLocatingUser, userLocation) {
        if (isLocatingUser && userLocation != null) {
            cameraPositionState.position =
                CameraPosition.fromLatLngZoom(
                    LatLng(userLocation.latitude, userLocation.longitude),
                    maxOf(10f, cameraPositionState.position.zoom),
                )
            hasInitializedCamera = true
            onUserLocated()
        }
    }
    val requestedFocus = state.mapItems.find { it.id == state.requestedMapFocusId }
    LaunchedEffect(requestedFocus) {
        if (requestedFocus != null) {
            cameraPositionState.position =
                CameraPosition.fromLatLngZoom(
                    LatLng(requestedFocus.latitude, requestedFocus.longitude),
                    maxOf(5f, cameraPositionState.position.zoom),
                )
            hasInitializedCamera = true
            onMapFocusHandled()
        }
    }
    val listState = rememberLazyListState()
    val safePadding = WindowInsets.safeDrawing.asPaddingValues()
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    var monitoringCardHeight by remember { mutableStateOf(0.dp) }
    var mapBottomControlsHeight by remember { mutableStateOf(0.dp) }
    var clusterSheetHeight by remember { mutableStateOf(0.dp) }
    val selectedItem = state.mapItems.find { it.id == state.selectedMapItemId }
    val showCluster = state.selectedClusterItemIds.isNotEmpty()
    val clusterItems = state.mapItems.filter { it.id in state.selectedClusterItemIds }
    LaunchedEffect(state.selectedClusterItemIds, clusterSheetHeight) {
        if (!showCluster) {
            clusterSheetHeight = 0.dp
        } else if (clusterSheetHeight > 0.dp) {
            withFrameNanos {}
            val item =
                clusterItems.find { it.id == state.selectedMapItemId } ?: clusterItems.firstOrNull()
            if (item != null) {
                hasInitializedCamera = true
                cameraPositionState.position =
                    CameraPosition.fromLatLngZoom(
                        LatLng(item.latitude, item.longitude),
                        cameraPositionState.position.zoom,
                    )
            }
        }
    }

    val expanded =
        currentWindowAdaptiveInfoV2()
            .windowSizeClass
            .isAtLeastBreakpoint(WIDTH_DP_EXPANDED_LOWER_BOUND, HEIGHT_DP_MEDIUM_LOWER_BOUND)
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        val showListPanel =
            expanded &&
                isListPanelVisible &&
                !notificationsVisible &&
                !showCluster &&
                !detailsVisible
        EarthquakeExplorerControls(
            items = state.mapItems,
            selectedItemId = state.selectedMapItemId,
            selectedView = selectedView,
            expanded = expanded,
            isListPanelVisible = isListPanelVisible,
            onListPanelVisibilityChanged = onListPanelVisibilityChanged,
            onItemClick = { item ->
                onMapItemClick(item)
                hasInitializedCamera = true
                cameraPositionState.position =
                    CameraPosition.fromLatLngZoom(
                        LatLng(item.latitude, item.longitude),
                        maxOf(5f, cameraPositionState.position.zoom),
                    )
                if (!expanded) {
                    onViewSelected(EarthquakeView.Map)
                }
            },
            onClearSelection = onMapClick,
            onViewSelected = onViewSelected,
            contentPadding = safePadding,
            listState = listState,
            renderListContent = renderListContent,
            onMapBottomControlsSizeChanged = {
                mapBottomControlsHeight = with(density) { it.toDp() }
            },
            showLocationButton = true,
            requiresLocationSettings = requiresLocationSettings,
            onLocationClick = onLocationClick,
            monitoring = state.monitoring,
            onRefresh = onRefresh,
            notificationsVisible = notificationsVisible,
            detailsVisible = detailsVisible,
            onMoreDetails = onMoreDetails,
            onNotificationsClick = onNotificationsClick,
            onNotificationsClose = onNotificationsClose,
            notificationContent = notificationContent,
            snackbarHost = snackbarHost,
            showControls = !showCluster,
            content = {
                if (
                    showCluster ||
                        expanded ||
                        selectedView == EarthquakeView.Map && !notificationsVisible
                ) {
                    EarthquakeMap(
                        items = state.mapItems,
                        onItemClick = onMapItemClick,
                        modifier = Modifier.fillMaxSize(),
                        selectedItemId = state.selectedMapItemId,
                        onMapClick = onMapClick,
                        onClusterClick = onClusterClick,
                        cameraPositionState = cameraPositionState,
                        properties =
                            MapProperties(
                                minZoomPreference = 1f,
                                isMyLocationEnabled = isLocationGranted,
                            ),
                        uiSettings =
                            MapUiSettings(
                                compassEnabled = false,
                                indoorLevelPickerEnabled = false,
                                mapToolbarEnabled = false,
                                myLocationButtonEnabled = false,
                                zoomControlsEnabled = false,
                            ),
                        contentPadding =
                            PaddingValues(
                                start =
                                    safePadding.calculateStartPadding(layoutDirection) +
                                        if (
                                            showListPanel ||
                                                expanded && detailsVisible && !showCluster
                                        ) {
                                            earthquakeListPaneWidth + 32.dp
                                        } else {
                                            0.dp
                                        },
                                end =
                                    safePadding.calculateEndPadding(layoutDirection) +
                                        if (expanded && notificationsVisible && !showCluster) {
                                            earthquakeListPaneWidth + 32.dp
                                        } else {
                                            0.dp
                                        },
                                top =
                                    safePadding.calculateTopPadding() +
                                        when {
                                            showCluster -> 16.dp
                                            expanded -> 80.dp
                                            else -> 100.dp + monitoringCardHeight
                                        },
                                bottom =
                                    safePadding.calculateBottomPadding() +
                                        when {
                                            showCluster -> clusterSheetHeight + 48.dp
                                            selectedItem != null || !notificationsVisible ->
                                                mapBottomControlsHeight + 32.dp
                                            else -> 0.dp
                                        },
                            ),
                    )
                }
            },
            onMonitoringCardSizeChanged = { monitoringCardHeight = with(density) { it.toDp() } },
        )
        if (showCluster) {
            EarthquakeClusterSheet(
                items = clusterItems,
                selectedItemId = state.selectedMapItemId,
                onItemClick = { item ->
                    onMapItemClick(item)
                    hasInitializedCamera = true
                    cameraPositionState.position =
                        CameraPosition.fromLatLngZoom(
                            LatLng(item.latitude, item.longitude),
                            cameraPositionState.position.zoom,
                        )
                },
                onDismiss = onClusterDismiss,
                modifier =
                    Modifier.onSizeChanged {
                        clusterSheetHeight = with(density) { it.height.toDp() }
                    },
            )
        }
    }
}
