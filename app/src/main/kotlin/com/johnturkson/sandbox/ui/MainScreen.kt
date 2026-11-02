package com.johnturkson.sandbox.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun MainScreen(
    navKey: Destination.MainRoute,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel =
        hiltViewModel<MainViewModel, MainViewModel.Factory>(
            creationCallback = { factory -> factory.create(navKey) }
        ),
) {
    MainScreen(
        state = viewModel,
        onMapItemClick = viewModel::selectMapItem,
        onMapClick = viewModel::clearMapSelection,
        modifier = modifier,
    )
}

@Composable
fun MainScreen(
    state: MainState,
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
