package com.johnturkson.sandbox.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

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
        modifier = modifier,
    )
}

@Composable
fun EarthquakeScreen(
    state: EarthquakeState,
    onMapItemClick: (EarthquakeMapItem) -> Unit,
    onMapClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EarthquakeMap(
        items = state.mapItems,
        onItemClick = onMapItemClick,
        modifier = modifier.fillMaxSize(),
        selectedItemId = state.selectedMapItemId,
        onMapClick = onMapClick,
        contentPadding = WindowInsets.safeDrawing.asPaddingValues(),
    )
}
