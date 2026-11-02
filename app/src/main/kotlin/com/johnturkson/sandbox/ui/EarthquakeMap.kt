package com.johnturkson.sandbox.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.clustering.Clustering
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.johnturkson.sandbox.R
import com.johnturkson.sandbox.data.EarthquakeLocation

@OptIn(MapsComposeExperimentalApi::class)
@Composable
fun EarthquakeMap(
    items: List<EarthquakeMapItem>,
    onItemClick: (EarthquakeMapItem) -> Unit,
    modifier: Modifier = Modifier,
    selectedItemId: String? = null,
    onMapClick: () -> Unit = {},
    onClusterClick: (List<EarthquakeMapItem>) -> Unit = {},
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
    val clusterItems =
        remember(items, selectedItemId) {
            items.filter { it.id != selectedItemId }.map(::EarthquakeClusterItem)
        }
    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        contentDescription = stringResource(R.string.earthquake_map),
        properties = properties,
        uiSettings = uiSettings,
        contentPadding = contentPadding,
        onMapClick = { onMapClick() },
    ) {
        Clustering(
            items = clusterItems,
            onClusterClick = { cluster ->
                onClusterClick(cluster.items.map { it.item })
                true
            },
            onClusterItemClick = {
                onItemClick(it.item)
                true
            },
            clusterContent = { cluster ->
                EarthquakeClusterMarker(
                    count = cluster.size,
                    maximumMagnitude = cluster.items.map { it.item }.maximumMagnitude(),
                )
            },
            clusterItemContent = { EarthquakeMapMarker(isSelected = false) },
            clusterContentAnchor = Offset(0.5f, 1f),
            clusterItemContentAnchor = Offset(0.5f, 0.5f),
            onClusterManager = {
                (it.renderer as? DefaultClusterRenderer<EarthquakeClusterItem>)?.minClusterSize = 2
            },
        )
        items
            .find { it.id == selectedItemId }
            ?.let { item ->
                key(item.id) {
                    MarkerComposable(
                        item.id,
                        true,
                        state = rememberUpdatedMarkerState(LatLng(item.latitude, item.longitude)),
                        contentDescription = item.title,
                        title = item.title,
                        snippet = item.snippet,
                        anchor = Offset(0.5f, 0.5f),
                        zIndex = 2f,
                        onClick = {
                            onItemClick(item)
                            true
                        },
                    ) {
                        EarthquakeMapMarker(isSelected = true)
                    }
                }
            }
    }
}
