package com.johnturkson.sandbox.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
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
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.rememberCameraPositionState
import com.johnturkson.sandbox.R
import com.johnturkson.sandbox.data.EarthquakeLocation
import com.johnturkson.sandbox.data.EarthquakeNotifications

@Composable
fun EarthquakeScreen(
    navKey: Destination.EarthquakeRoute,
    modifier: Modifier = Modifier,
    notificationEventId: String? = null,
    onNotificationEventConsumed: () -> Unit = {},
    viewModel: EarthquakeViewModel =
        hiltViewModel<EarthquakeViewModel, EarthquakeViewModel.Factory>(
            creationCallback = { factory -> factory.create(navKey) }
        ),
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
        if (isLocationGranted) viewModel.startLocationUpdates()
        onStopOrDispose { viewModel.stopLocationUpdates() }
    }
    var notificationRationale by rememberSaveable { mutableStateOf(false) }
    var shouldExplainNotifications by remember { mutableStateOf(false) }
    fun updateNotificationPermission() {
        viewModel.refreshNotificationCapabilities()
        shouldExplainNotifications =
            Build.VERSION.SDK_INT >= 33 &&
                activity != null &&
                ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS,
                )
    }
    val notificationLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            updateNotificationPermission()
        }
    LifecycleResumeEffect(Unit) {
        updateNotificationPermission()
        onPauseOrDispose {}
    }
    val notificationState = viewModel.notifications
    val notificationSettingsRequired =
        notificationState.permissionGranted && !notificationState.appEnabled ||
            notificationState.hasRequestedPermission && !shouldExplainNotifications
    fun launchNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            viewModel.recordNotificationPermissionRequest()
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    fun enableNotifications() {
        when {
            notificationSettingsRequired || Build.VERSION.SDK_INT < 33 ->
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                )
            shouldExplainNotifications -> notificationRationale = true
            else -> launchNotificationPermission()
        }
    }
    BackHandler(viewModel.notificationsVisible) { viewModel.showNotifications(false) }
    LaunchedEffect(notificationEventId) {
        if (notificationEventId != null) {
            viewModel.openNotificationEvent(notificationEventId)
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
    val requiresSettings = hasRequestedLocation && !shouldShowRationale
    EarthquakeScreen(
        state = viewModel,
        onMapItemClick = viewModel::selectMapItem,
        onMapClick = viewModel::clearMapSelection,
        onClusterClick = viewModel::selectMapCluster,
        onClusterDismiss = viewModel::dismissMapCluster,
        onViewSelected = viewModel::selectView,
        onListPanelVisibilityChanged = viewModel::updateListPanelVisibility,
        onRefresh = viewModel::refresh,
        onNotificationsClick = { viewModel.showNotifications(true) },
        onNotificationsClose = { viewModel.showNotifications(false) },
        onMapFocusHandled = viewModel::consumeMapFocus,
        notificationContent = { pageModifier ->
            EarthquakeNotificationPage(
                state = notificationState.copy(requiresSettings = notificationSettingsRequired),
                monitoring = viewModel.monitoring,
                onClose = { viewModel.showNotifications(false) },
                onEnableNotifications = ::enableNotifications,
                onAlertsEnabled = { enabled ->
                    viewModel.setAlertsEnabled(enabled)
                    if (
                        enabled &&
                            (!notificationState.permissionGranted || !notificationState.appEnabled)
                    )
                        enableNotifications()
                },
                onMagnitudeSelected = viewModel::setMinimumMagnitude,
                onMonitoringEnabled = viewModel::setMonitoring,
                onChannelSettings = { alerts ->
                    context.startActivity(
                        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            .putExtra(
                                Settings.EXTRA_CHANNEL_ID,
                                if (alerts) EarthquakeNotifications.AlertsChannel
                                else EarthquakeNotifications.MonitoringChannel,
                            )
                    )
                },
                onPreviewAlert = viewModel::previewNotification,
                modifier = pageModifier,
            )
        },
        modifier = modifier,
        isLocationGranted = isLocationGranted,
        requiresLocationSettings = requiresSettings,
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
    Box(Modifier.fillMaxSize()) {
        SnackbarHost(
            snackbarHost,
            Modifier.align(Alignment.BottomCenter)
                .padding(WindowInsets.safeDrawing.asPaddingValues()),
        )
    }
    if (notificationRationale) {
        EarthquakeNotificationRationale(
            onContinue = {
                notificationRationale = false
                launchNotificationPermission()
            },
            onDismiss = { notificationRationale = false },
        )
    }
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
    notificationContent: @Composable (Modifier) -> Unit = { pageModifier ->
        EarthquakeNotificationPage(
            state.notifications,
            state.monitoring,
            onNotificationsClose,
            {},
            {},
            {},
            {},
            {},
            {},
            pageModifier,
        )
    },
    cameraPositionState: CameraPositionState = rememberCameraPositionState {
        val location = EarthquakeLocation.Default
        position = CameraPosition.fromLatLngZoom(LatLng(location.latitude, location.longitude), 10f)
    },
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
    var selectedCardHeight by remember { mutableStateOf(0.dp) }
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

    BoxWithConstraints(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        val expanded = maxWidth >= 840.dp && maxHeight >= 480.dp
        val showListPanel =
            expanded && state.isListPanelVisible && !state.notificationsVisible && !showCluster
        if (showCluster || expanded || state.selectedView == EarthquakeView.Map && !state.notificationsVisible) {
            EarthquakeMap(
                items = state.mapItems,
                onItemClick = onMapItemClick,
                modifier = Modifier.fillMaxSize(),
                selectedItemId = state.selectedMapItemId,
                onMapClick = onMapClick,
                onClusterClick = onClusterClick,
                cameraPositionState = cameraPositionState,
                properties =
                    MapProperties(minZoomPreference = 1f, isMyLocationEnabled = isLocationGranted),
                contentPadding =
                    PaddingValues(
                        start =
                            safePadding.calculateStartPadding(layoutDirection) +
                                if (showListPanel) earthquakeListPaneWidth + 32.dp else 0.dp,
                        end =
                            safePadding.calculateEndPadding(layoutDirection) +
                                if (expanded && state.notificationsVisible)
                                    earthquakeListPaneWidth + 32.dp
                                else 0.dp,
                        top =
                            safePadding.calculateTopPadding() +
                                if (showCluster) 16.dp
                                else if (showListPanel) 80.dp
                                else
                                    (if (!expanded && !isLocationGranted) 160.dp else 100.dp) +
                                        monitoringCardHeight,
                        bottom =
                            safePadding.calculateBottomPadding() +
                                if (showCluster) clusterSheetHeight + 48.dp
                                else if (showListPanel) 16.dp
                                else if (selectedItem != null) selectedCardHeight + 32.dp else 0.dp,
                    ),
            )
        }
        if (!showCluster) {
            EarthquakeExplorerControls(
                items = state.mapItems,
                selectedItemId = state.selectedMapItemId,
                selectedView = state.selectedView,
                expanded = expanded,
                isListPanelVisible = state.isListPanelVisible,
                onListPanelVisibilityChanged = onListPanelVisibilityChanged,
                onItemClick = { item ->
                    onMapItemClick(item)
                    hasInitializedCamera = true
                    cameraPositionState.position =
                        CameraPosition.fromLatLngZoom(
                            LatLng(item.latitude, item.longitude),
                            maxOf(5f, cameraPositionState.position.zoom),
                        )
                    if (!expanded) onViewSelected(EarthquakeView.Map)
                },
                onClearSelection = onMapClick,
                onViewSelected = onViewSelected,
                contentPadding = safePadding,
                listState = listState,
                onSelectionCardSizeChanged = { selectedCardHeight = with(density) { it.toDp() } },
                showLocationButton = !isLocationGranted,
                requiresLocationSettings = requiresLocationSettings,
                onLocationClick = onLocationClick,
                monitoring = state.monitoring,
                onRefresh = onRefresh,
                notificationsVisible = state.notificationsVisible,
                onNotificationsClick = onNotificationsClick,
                onNotificationsClose = onNotificationsClose,
                notificationContent = notificationContent,
                onMonitoringCardSizeChanged = { monitoringCardHeight = with(density) { it.toDp() } },
            )
        }
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
