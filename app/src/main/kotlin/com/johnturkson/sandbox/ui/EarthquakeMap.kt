package com.johnturkson.sandbox.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.johnturkson.sandbox.R
import com.johnturkson.sandbox.data.EarthquakeLocation

@Composable
fun EarthquakeMap(
    items: List<EarthquakeMapItem>,
    onItemClick: (EarthquakeMapItem) -> Unit,
    modifier: Modifier = Modifier,
    selectedItemId: String? = null,
    onMapClick: () -> Unit = {},
    cameraPositionState: CameraPositionState = rememberCameraPositionState {
        val location = EarthquakeLocation.Default
        position = CameraPosition.fromLatLngZoom(LatLng(location.latitude, location.longitude), 10f)
    },
    properties: MapProperties = MapProperties(minZoomPreference = 1f),
    uiSettings: MapUiSettings =
        MapUiSettings(
            compassEnabled = false,
            indoorLevelPickerEnabled = false,
            mapToolbarEnabled = false,
            myLocationButtonEnabled = true,
            zoomControlsEnabled = false,
        ),
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        contentDescription = stringResource(R.string.earthquake_map),
        properties = properties,
        uiSettings = uiSettings,
        contentPadding = contentPadding,
        onMapClick = { onMapClick() },
    ) {
        items.forEach { item ->
            key(item.id) {
                MarkerComposable(
                    item.id,
                    item.id == selectedItemId,
                    state = rememberUpdatedMarkerState(LatLng(item.latitude, item.longitude)),
                    contentDescription = item.title,
                    title = item.title,
                    snippet = item.snippet,
                    anchor = Offset(0.5f, 0.5f),
                    zIndex = if (item.id == selectedItemId) 1f else 0f,
                    onClick = {
                        onItemClick(item)
                        true
                    },
                ) {
                    EarthquakeMapMarker(isSelected = item.id == selectedItemId)
                }
            }
        }
    }
}
