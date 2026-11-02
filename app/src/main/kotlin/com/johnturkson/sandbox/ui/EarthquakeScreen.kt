package com.johnturkson.sandbox.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
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
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.rememberCameraPositionState
import com.johnturkson.sandbox.data.EarthquakeLocation

@Composable
fun EarthquakeScreen(
    navKey: Destination.EarthquakeRoute,
    modifier: Modifier = Modifier,
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
    val requiresSettings = hasRequestedLocation && !shouldShowRationale
    EarthquakeScreen(
        state = viewModel,
        onMapItemClick = viewModel::selectMapItem,
        onMapClick = viewModel::clearMapSelection,
        onViewSelected = viewModel::selectView,
        onListPanelVisibilityChanged = viewModel::updateListPanelVisibility,
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
) {
    val cameraPositionState = rememberCameraPositionState {
        val location = EarthquakeLocation.Default
        position = CameraPosition.fromLatLngZoom(LatLng(location.latitude, location.longitude), 10f)
    }
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
    val listState = rememberLazyListState()
    val safePadding = WindowInsets.safeDrawing.asPaddingValues()
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    var selectedCardHeight by remember { mutableStateOf(0.dp) }
    val selectedItem = state.mapItems.find { it.id == state.selectedMapItemId }

    BoxWithConstraints(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        val expanded = maxWidth >= 840.dp && maxHeight >= 480.dp
        val showListPanel = expanded && state.isListPanelVisible
        if (expanded || state.selectedView == EarthquakeView.Map) {
            EarthquakeMap(
                items = state.mapItems,
                onItemClick = onMapItemClick,
                modifier = Modifier.fillMaxSize(),
                selectedItemId = state.selectedMapItemId,
                onMapClick = onMapClick,
                cameraPositionState = cameraPositionState,
                properties =
                    MapProperties(minZoomPreference = 1f, isMyLocationEnabled = isLocationGranted),
                contentPadding =
                    PaddingValues(
                        start =
                            safePadding.calculateStartPadding(layoutDirection) +
                                if (showListPanel) earthquakeListPaneWidth + 32.dp else 0.dp,
                        end = safePadding.calculateEndPadding(layoutDirection),
                        top =
                            safePadding.calculateTopPadding() + if (showListPanel) 16.dp else 84.dp,
                        bottom =
                            safePadding.calculateBottomPadding() +
                                if (showListPanel) 16.dp
                                else if (selectedItem != null) selectedCardHeight + 32.dp else 0.dp,
                    ),
            )
        }
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
        )
    }
}
