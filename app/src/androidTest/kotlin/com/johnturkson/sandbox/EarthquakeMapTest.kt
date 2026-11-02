package com.johnturkson.sandbox

import android.view.InputDevice
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.GeneralClickAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Tap
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
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
import kotlin.time.Instant
import org.junit.Rule
import org.junit.Test

class EarthquakeMapTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun scaffoldKeepsNativeMapFullSizeWithLocationFabAndSelection() {
        var state by mutableStateOf(EarthquakePreviewState())
        var isListPanelVisible by mutableStateOf(false)
        composeRule.setContent {
            EarthquakeScreen(
                state = state,
                onMapItemClick = {},
                onMapClick = {},
                isListPanelVisible = isListPanelVisible,
                modifier = Modifier.testTag("explorer"),
            )
        }
        val map = awaitMap()
        fun assertFullSize() {
            val bounds = composeRule.onNodeWithTag("explorer").fetchSemanticsNode().boundsInWindow
            composeRule.runOnIdle {
                val mapView = assertNotNull(findMapView(composeRule.activity.window.decorView))
                val origin = IntArray(2)
                mapView.getLocationInWindow(origin)
                assertEquals(bounds.left, origin[0].toFloat(), 1f)
                assertEquals(bounds.top, origin[1].toFloat(), 1f)
                assertEquals(bounds.width, mapView.width.toFloat(), 1f)
                assertEquals(bounds.height, mapView.height.toFloat(), 1f)
                assertTrue(map.uiSettings.isScrollGesturesEnabled)
                assertTrue(map.uiSettings.isZoomGesturesEnabled)
                assertFalse(map.uiSettings.isMyLocationButtonEnabled)
            }
        }
        composeRule.onNodeWithContentDescription("Use my location").assertIsDisplayed()
        assertFullSize()
        composeRule.runOnIdle { state = state.copy(selectedMapItemId = state.mapItems.first().id) }
        composeRule.onNodeWithContentDescription("Clear earthquake selection").assertIsDisplayed()
        assertFullSize()
        composeRule.runOnIdle { isListPanelVisible = true }
        assertFullSize()
    }

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
            state =
                state.copy(
                    userLocation =
                        EarthquakeLocation(
                            49.2827,
                            -123.1207,
                            Instant.fromEpochMilliseconds(1_000L),
                        )
                )
        }
        composeRule.waitUntil(10_000) { !isLocatingUser }
        composeRule.runOnIdle {
            assertEquals(49.2827, map.cameraPosition.target.latitude, 0.0001)
            assertEquals(-123.1207, map.cameraPosition.target.longitude, 0.0001)
            state =
                state.copy(
                    userLocation =
                        EarthquakeLocation(40.7128, -74.0060, Instant.fromEpochMilliseconds(2_000L))
                )
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
        val first =
            EarthquakeMapItem(
                "first",
                0.0,
                -8.0,
                title = null,
                magnitude = 4.5,
                place = "First earthquake",
            )
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

    @Test
    fun coincidentPairOpensClusterWithCurrentMembersEvenAtMaximumZoom() {
        val first = EarthquakeMapItem("first", 0.0, 0.0, "First")
        val second = first.copy(id = "second", title = "Second")
        var items by mutableStateOf(listOf(first, second))
        var clicked: List<EarthquakeMapItem>? = null
        val camera = CameraPositionState(CameraPosition.fromLatLngZoom(LatLng(0.0, 0.0), 21f))
        composeRule.setContent {
            EarthquakeMap(
                items = items,
                onItemClick = {},
                onClusterClick = { clicked = it },
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = camera,
                properties = MapProperties(mapType = MapType.NONE),
            )
        }
        val map = awaitMap()
        tapLocation(map, LatLng(0.0, 0.0), verticalOffsetDp = -40f)
        composeRule.waitUntil(10_000) { clicked != null }
        composeRule.runOnIdle {
            assertEquals(setOf("first", "second"), clicked!!.map { it.id }.toSet())
            items = listOf(second.copy(title = "Updated second"), first)
            clicked = null
        }
        tapLocation(map, LatLng(0.0, 0.0), verticalOffsetDp = -40f)
        composeRule.waitUntil(10_000) { clicked?.any { it.title == "Updated second" } == true }
    }

    @Test
    fun zoomingSeparatesNearbyEventsAndSelectedEventRemainsClickable() {
        val first = EarthquakeMapItem("first", 0.0, 0.0, "First")
        val second = first.copy(id = "second", longitude = 0.02, title = "Second")
        var selected by mutableStateOf<String?>(null)
        var cluster: List<EarthquakeMapItem>? = null
        var itemClicks = 0
        val camera = CameraPositionState(CameraPosition.fromLatLngZoom(LatLng(0.0, 0.0), 10f))
        composeRule.setContent {
            EarthquakeMap(
                items = listOf(first, second),
                onItemClick = {
                    selected = it.id
                    itemClicks++
                },
                onClusterClick = { cluster = it },
                selectedItemId = selected,
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = camera,
                properties = MapProperties(mapType = MapType.NONE),
            )
        }
        val map = awaitMap()
        tapLocation(map, LatLng(0.0, 0.0), verticalOffsetDp = -40f)
        composeRule.waitUntil(10_000) { cluster?.size == 2 }
        composeRule.runOnIdle {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(0.0, 0.0), 18f))
        }
        tapLocation(map, LatLng(0.0, 0.0))
        composeRule.waitUntil(10_000) { selected == "first" }
        composeRule.runOnIdle {
            assertEquals(1, itemClicks)
            selected = "second"
            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(second.latitude, second.longitude), 18f)
            )
        }
        tapLocation(map, LatLng(second.latitude, second.longitude))
        composeRule.waitUntil(10_000) { itemClicks == 2 }
        composeRule.runOnIdle { assertEquals("second", selected) }
    }

    @Test
    fun openingClusterCentersAMemberAboveTheMeasuredSheet() {
        val first = EarthquakeMapItem("vancouver", 49.2827, -123.1207, "Vancouver first")
        val items = listOf(first, first.copy(id = "second", title = "Vancouver second"))
        composeRule.setContent {
            EarthquakeScreen(
                state =
                    EarthquakePreviewState(
                        mapItems = items,
                        selectedClusterItemIds = items.map { it.id },
                    ),
                onMapItemClick = {},
                onMapClick = {},
            )
        }
        val map = awaitMap()
        composeRule.onNodeWithText("Map").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Use my location").assertDoesNotExist()
        val sheet =
            composeRule
                .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.PaneTitle))
                .fetchSemanticsNode()
                .boundsInWindow
        composeRule.runOnIdle {
            assertEquals(first.latitude, map.cameraPosition.target.latitude, 0.0001)
            assertEquals(first.longitude, map.cameraPosition.target.longitude, 0.0001)
            val point = map.projection.toScreenLocation(LatLng(first.latitude, first.longitude))
            val origin = IntArray(2)
            assertNotNull(findMapView(composeRule.activity.window.decorView))
                .getLocationInWindow(origin)
            assertTrue(point.y > 0)
            assertTrue(origin[1] + point.y < sheet.top)
        }
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

    private fun tapLocation(map: GoogleMap, location: LatLng, verticalOffsetDp: Float = 0f) {
        composeRule.waitForIdle()
        var frameReady = false
        composeRule.runOnIdle { map.snapshot { frameReady = it != null } }
        composeRule.waitUntil(10_000) { frameReady }
        val point = composeRule.runOnIdle { map.projection.toScreenLocation(location) }
        val verticalOffset =
            verticalOffsetDp * composeRule.activity.resources.displayMetrics.density
        onView(isAssignableFrom(MapView::class.java))
            .perform(
                GeneralClickAction(
                    Tap.SINGLE,
                    { view ->
                        val origin = IntArray(2)
                        view.getLocationOnScreen(origin)
                        floatArrayOf(
                            origin[0] + point.x.toFloat(),
                            origin[1] + point.y + verticalOffset,
                        )
                    },
                    Press.FINGER,
                    InputDevice.SOURCE_TOUCHSCREEN,
                    0,
                )
            )
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
