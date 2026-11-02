package com.johnturkson.sandbox.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun EarthquakeScreen(
    navKey: Destination.EarthquakeRoute,
    modifier: Modifier = Modifier,
    viewModel: EarthquakeViewModel =
        hiltViewModel<EarthquakeViewModel, EarthquakeViewModel.Factory>(
            creationCallback = { factory -> factory.create(navKey) }
        ),
) {
    EarthquakeScreen(
        state = viewModel,
        onMapItemClick = viewModel::selectMapItem,
        onMapClick = viewModel::clearMapSelection,
        onViewSelected = viewModel::selectView,
        modifier = modifier,
    )
}

@Composable
fun EarthquakeScreen(
    state: EarthquakeState,
    onMapItemClick: (EarthquakeMapItem) -> Unit,
    onMapClick: () -> Unit,
    modifier: Modifier = Modifier,
    onViewSelected: (EarthquakeView) -> Unit = {},
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(20.0, 0.0), 1f)
    }
    val listState = rememberLazyListState()
    val safePadding = WindowInsets.safeDrawing.asPaddingValues()
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    var selectedCardHeight by remember { mutableStateOf(0.dp) }
    val selectedItem = state.mapItems.find { it.id == state.selectedMapItemId }

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        when (state.selectedView) {
            EarthquakeView.Map -> {
                EarthquakeMap(
                    items = state.mapItems,
                    onItemClick = onMapItemClick,
                    modifier = Modifier.fillMaxSize(),
                    selectedItemId = state.selectedMapItemId,
                    onMapClick = onMapClick,
                    cameraPositionState = cameraPositionState,
                    contentPadding =
                        PaddingValues(
                            start = safePadding.calculateStartPadding(layoutDirection),
                            end = safePadding.calculateEndPadding(layoutDirection),
                            top = safePadding.calculateTopPadding() + 84.dp,
                            bottom =
                                safePadding.calculateBottomPadding() +
                                    if (selectedItem != null) selectedCardHeight + 32.dp else 0.dp,
                        ),
                )
                if (selectedItem != null) {
                    EarthquakeSelectionCard(
                        item = selectedItem,
                        onDismiss = onMapClick,
                        modifier =
                            Modifier.align(Alignment.BottomCenter)
                                .padding(safePadding)
                                .padding(horizontal = 16.dp, vertical = 16.dp)
                                .widthIn(max = 480.dp)
                                .fillMaxWidth()
                                .onSizeChanged {
                                    selectedCardHeight = with(density) { it.height.toDp() }
                                },
                    )
                }
            }
            EarthquakeView.List -> {
                EarthquakeList(
                    items = state.mapItems,
                    selectedItemId = state.selectedMapItemId,
                    onItemClick = { item ->
                        onMapItemClick(item)
                        cameraPositionState.position =
                            CameraPosition.fromLatLngZoom(
                                LatLng(item.latitude, item.longitude),
                                maxOf(5f, cameraPositionState.position.zoom),
                            )
                        onViewSelected(EarthquakeView.Map)
                    },
                    modifier =
                        Modifier.align(Alignment.TopCenter)
                            .padding(safePadding)
                            .padding(top = 84.dp)
                            .widthIn(max = 840.dp)
                            .fillMaxSize(),
                    listState = listState,
                )
            }
        }
        EarthquakeViewSwitcher(
            selectedView = state.selectedView,
            onViewSelected = onViewSelected,
            modifier =
                Modifier.align(Alignment.TopCenter)
                    .padding(safePadding)
                    .padding(top = 12.dp)
                    .widthIn(max = 280.dp)
                    .fillMaxWidth(),
        )
    }
}
