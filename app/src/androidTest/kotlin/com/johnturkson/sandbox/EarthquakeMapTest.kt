package com.johnturkson.sandbox

import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.rememberCameraPositionState
import com.johnturkson.sandbox.data.EarthquakeLocation
import com.johnturkson.sandbox.ui.EarthquakeMap
import com.johnturkson.sandbox.ui.EarthquakeMapItem
import com.johnturkson.sandbox.ui.EarthquakePreviewState
import com.johnturkson.sandbox.ui.EarthquakeScreen
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

class EarthquakeMapTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun explicitLocateCentersOnceWithoutFollowingLaterFixes() {
        var state by mutableStateOf(EarthquakePreviewState())
        var isLocatingUser by mutableStateOf(true)
        composeRule.setContent {
            EarthquakeScreen(
                state = state,
                onMapItemClick = {},
                onMapClick = {},
                isLocatingUser = isLocatingUser,
                onUserLocated = { isLocatingUser = false },
            )
        }
        val map = awaitMap()
        composeRule.runOnIdle {
            state = state.copy(userLocation = EarthquakeLocation(49.2827, -123.1207, 1_000L))
        }
        composeRule.waitUntil(10_000) { !isLocatingUser }
        composeRule.runOnIdle {
            assertEquals(49.2827, map.cameraPosition.target.latitude, 0.0001)
            assertEquals(-123.1207, map.cameraPosition.target.longitude, 0.0001)
            state = state.copy(userLocation = EarthquakeLocation(40.7128, -74.0060, 2_000L))
        }
        composeRule.waitForIdle()
        composeRule.runOnIdle { assertEquals(49.2827, map.cameraPosition.target.latitude, 0.0001) }
    }

    @Test
    fun persistedLocationInitializesCameraOnceAndRestoredCameraWins() {
        val restorationTester = StateRestorationTester(composeRule)
        lateinit var camera: CameraPositionState
        var state by
            mutableStateOf(
                EarthquakePreviewState(initialMapLocation = EarthquakeLocation(49.2827, -123.1207))
            )
        restorationTester.setContent {
            camera = rememberCameraPositionState()
            EarthquakeScreen(
                state = state,
                onMapItemClick = {},
                onMapClick = {},
                cameraPositionState = camera,
            )
        }
        val map = awaitMap()
        composeRule.runOnIdle {
            assertEquals(49.2827, map.cameraPosition.target.latitude, 0.0001)
            assertEquals(-123.1207, map.cameraPosition.target.longitude, 0.0001)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(34.0522, -118.2437), 7f))
        }
        composeRule.waitUntil(10_000) {
            abs(camera.position.target.latitude - 34.0522) < 0.0001 && !camera.isMoving
        }
        composeRule.runOnIdle {
            state = state.copy(initialMapLocation = EarthquakeLocation(40.7128, -74.0060))
        }
        composeRule.waitForIdle()
        composeRule.runOnIdle { assertEquals(34.0522, map.cameraPosition.target.latitude, 0.0001) }
        restorationTester.emulateSavedInstanceStateRestore()
        val restoredMap = awaitMap()
        composeRule.waitUntil(10_000) {
            composeRule.runOnIdle {
                abs(restoredMap.cameraPosition.target.latitude - 34.0522) < 0.0001
            }
        }
        composeRule.runOnIdle {
            assertEquals(34.0522, restoredMap.cameraPosition.target.latitude, 0.0001)
            assertEquals(-118.2437, restoredMap.cameraPosition.target.longitude, 0.0001)
            assertEquals(7f, restoredMap.cameraPosition.zoom)
        }
    }

    @Test
    fun emptyMapAppliesConfigurationAndKeepsGesturesEnabled() {
        val cameraPositionState =
            CameraPositionState(CameraPosition.fromLatLngZoom(LatLng(0.0, 0.0), 4f))
        composeRule.setContent {
            EarthquakeMap(
                items = emptyList(),
                onItemClick = {},
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(mapType = MapType.NONE),
            )
        }

        val map = awaitMap()
        composeRule.runOnIdle {
            assertEquals(GoogleMap.MAP_TYPE_NONE, map.mapType)
            assertFalse(map.isMyLocationEnabled)
            assertTrue(map.uiSettings.isMyLocationButtonEnabled)
            assertFalse(map.uiSettings.isCompassEnabled)
            assertFalse(map.uiSettings.isZoomControlsEnabled)
            assertFalse(map.uiSettings.isMapToolbarEnabled)
            assertTrue(map.uiSettings.isScrollGesturesEnabled)
            assertTrue(map.uiSettings.isZoomGesturesEnabled)
            assertEquals(4f, cameraPositionState.position.zoom)
        }
    }

    @Test
    fun markerClicksUseCurrentItemsAfterReorderingAndMoving() {
        val first = EarthquakeMapItem("first", 0.0, -8.0, "First earthquake")
        val second = EarthquakeMapItem("second", 0.0, 8.0, "Second earthquake")
        var items by mutableStateOf(listOf(first, second))
        var selectedItem by mutableStateOf<EarthquakeMapItem?>(null)
        var mapClicks = 0
        val cameraPositionState =
            CameraPositionState(CameraPosition.fromLatLngZoom(LatLng(0.0, 0.0), 5f))
        composeRule.setContent {
            EarthquakeMap(
                items = items,
                onItemClick = { selectedItem = it },
                modifier = Modifier.fillMaxSize(),
                selectedItemId = selectedItem?.id,
                onMapClick = {
                    selectedItem = null
                    mapClicks++
                },
                cameraPositionState = cameraPositionState,
                properties = MapProperties(mapType = MapType.NONE),
            )
        }
        val map = awaitMap()

        tapLocation(map, LatLng(first.latitude, first.longitude))
        composeRule.waitUntil(10_000) { selectedItem == first }

        val moved = second.copy(latitude = 8.0, longitude = 0.0, title = "Moved earthquake")
        composeRule.runOnIdle { items = listOf(moved, first) }
        tapLocation(map, LatLng(moved.latitude, moved.longitude))
        composeRule.waitUntil(10_000) { selectedItem == moved }

        composeRule.runOnIdle { items = emptyList() }
        tapLocation(map, LatLng(moved.latitude, moved.longitude))
        composeRule.waitUntil(10_000) { mapClicks == 1 }
        composeRule.runOnIdle { assertEquals(null, selectedItem) }
    }

    @Test
    fun earthquakeScreenRendersCapturedStateAndEmitsSelection() {
        var state by mutableStateOf(EarthquakePreviewState())
        composeRule.setContent {
            EarthquakeScreen(
                state = state,
                onMapItemClick = { state = state.copy(selectedMapItemId = it.id) },
                onMapClick = { state = state.copy(selectedMapItemId = null) },
            )
        }

        val map = awaitMap()
        val item = state.mapItems.single { it.id == "us7000tukm" }
        composeRule.runOnIdle {
            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(item.latitude, item.longitude), 5f)
            )
        }
        tapLocation(map, LatLng(item.latitude, item.longitude))
        composeRule.waitUntil(10_000) { state.selectedMapItemId == item.id }
    }

    private fun awaitMap(): GoogleMap {
        var map: GoogleMap? = null
        composeRule.runOnIdle {
            val mapView = findMapView(composeRule.activity.window.decorView)
            assertNotNull(mapView).getMapAsync { map = it }
        }
        composeRule.waitUntil(30_000) { map != null }
        composeRule.waitForIdle()
        return assertNotNull(map)
    }

    private fun tapLocation(map: GoogleMap, location: LatLng) {
        composeRule.waitForIdle()
        var frameReady = false
        composeRule.runOnIdle { map.snapshot { frameReady = it != null } }
        composeRule.waitUntil(10_000) { frameReady }
        val point = composeRule.runOnIdle { map.projection.toScreenLocation(location) }
        composeRule.onRoot().performTouchInput {
            click(Offset(point.x.toFloat(), point.y.toFloat()))
        }
    }

    private fun findMapView(view: View): MapView? {
        if (view is MapView) return view
        if (view is ViewGroup) {
            repeat(view.childCount) { index ->
                findMapView(view.getChildAt(index))?.let {
                    return it
                }
            }
        }
        return null
    }
}
