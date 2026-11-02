package com.johnturkson.sandbox.ui.layout

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.WindowSize
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.johnturkson.sandbox.theme.EarthquakeTheme
import com.johnturkson.sandbox.ui.event.EarthquakeMapItem
import com.johnturkson.sandbox.ui.map.EarthquakePreviewState
import com.johnturkson.sandbox.ui.map.EarthquakeScreen
import com.johnturkson.sandbox.ui.map.location.EarthquakeLocationButton
import com.johnturkson.sandbox.ui.monitoring.EarthquakeMonitoringState
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

class EarthquakeViewsTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun locationFabKeepsIconSizeAndAccessibleActionsAtLargeFontScale() {
        var requiresSettings by mutableStateOf(false)
        var clicks = 0
        composeRule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 2f)) {
                EarthquakeTheme {
                    EarthquakeLocationButton(
                        onClick = { clicks++ },
                        requiresSettings = requiresSettings,
                    )
                }
            }
        }

        composeRule.onNodeWithText("Use my location").assertDoesNotExist()
        composeRule
            .onNodeWithContentDescription("Use my location")
            .assertWidthIsEqualTo(56.dp)
            .assertHeightIsEqualTo(56.dp)
            .performClick()
        composeRule.runOnIdle { requiresSettings = true }
        composeRule.onNodeWithContentDescription("Use my location").assertDoesNotExist()
        composeRule.onNodeWithText("Location settings").assertDoesNotExist()
        composeRule
            .onNodeWithContentDescription("Location settings")
            .assertWidthIsEqualTo(56.dp)
            .assertHeightIsEqualTo(56.dp)
            .performClick()
        composeRule.runOnIdle { assertEquals(2, clicks) }
    }

    @Test
    fun locationFabFollowsMapVisibilityAcrossPresentations() {
        var selectedView by mutableStateOf(EarthquakeView.Map)
        var expanded by mutableStateOf(false)
        var notificationsVisible by mutableStateOf(false)
        composeRule.setContent {
            EarthquakeTheme {
                EarthquakeControls(
                    items = emptyList(),
                    selectedItemId = null,
                    selectedView = selectedView,
                    expanded = expanded,
                    onItemClick = {},
                    onClearSelection = {},
                    onViewSelected = {},
                    renderListContent = false,
                    showLocationButton = true,
                    notificationsVisible = notificationsVisible,
                )
            }
        }

        composeRule.onNodeWithContentDescription("Use my location").assertIsDisplayed()
        composeRule.runOnIdle { selectedView = EarthquakeView.List }
        composeRule.onNodeWithContentDescription("Use my location").assertDoesNotExist()
        composeRule.runOnIdle { expanded = true }
        composeRule.onNodeWithContentDescription("Use my location").assertIsDisplayed()
        composeRule.runOnIdle { notificationsVisible = true }
        composeRule.onNodeWithContentDescription("Use my location").assertDoesNotExist()
        composeRule.runOnIdle { expanded = false }
        composeRule.onNodeWithContentDescription("Use my location").assertDoesNotExist()
    }

    @Test
    fun locationFabRisesAboveSelectionWithoutMovingMonitoring() {
        val item = EarthquakeMapItem("fixture", 37.0, -122.0, "M4.5 - California")
        var showLocation by mutableStateOf(false)
        var selectedId by mutableStateOf<String?>(null)
        var locationClicks = 0
        var detailsId: String? = null
        composeRule.setContent {
            EarthquakeTheme {
                EarthquakeControls(
                    items = listOf(item),
                    selectedItemId = selectedId,
                    selectedView = EarthquakeView.Map,
                    expanded = false,
                    onItemClick = {},
                    onClearSelection = {},
                    onViewSelected = {},
                    showLocationButton = showLocation,
                    onLocationClick = { locationClicks++ },
                    onMoreDetails = { detailsId = it },
                    monitoring =
                        EarthquakeMonitoringState(
                            status = EarthquakeMonitoringState.Status.Running
                        ),
                )
            }
        }

        val monitoringBounds =
            composeRule
                .onNodeWithContentDescription("Live monitoring", substring = true)
                .fetchSemanticsNode()
                .boundsInRoot
        composeRule.runOnIdle { showLocation = true }
        val rootBounds = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        val locationBounds =
            composeRule
                .onNodeWithContentDescription("Use my location")
                .fetchSemanticsNode()
                .boundsInRoot
        assertEquals(
            monitoringBounds,
            composeRule
                .onNodeWithContentDescription("Live monitoring", substring = true)
                .fetchSemanticsNode()
                .boundsInRoot,
        )
        assertTrue(locationBounds.center.x > rootBounds.center.x)
        assertTrue(locationBounds.center.y > rootBounds.center.y)
        composeRule.runOnIdle { selectedId = item.id }
        val selectedLocationBounds =
            composeRule
                .onNodeWithContentDescription("Use my location")
                .fetchSemanticsNode()
                .boundsInRoot
        assertEquals(locationBounds.size, selectedLocationBounds.size)
        assertTrue(selectedLocationBounds.top < locationBounds.top)
        val selectionBounds =
            composeRule
                .onNodeWithContentDescription("Clear earthquake selection")
                .fetchSemanticsNode()
                .boundsInRoot
        assertTrue(selectionBounds.top > selectedLocationBounds.bottom)
        composeRule.onNodeWithContentDescription("Use my location").performClick()
        composeRule.onNodeWithText("More details").performClick()
        composeRule.runOnIdle {
            assertEquals(1, locationClicks)
            assertEquals(item.id, detailsId)
        }
    }

    @Test
    fun expandedPanelKeepsSelectionWithoutViewSwitching() {
        val title = "M6.0 - Shima, Japan"
        val item = EarthquakeMapItem("shima", 33.822, 136.9854, title)
        var selectedId by mutableStateOf<String?>(null)
        var isListPanelVisible by mutableStateOf(true)
        var viewSwitches = 0
        composeRule.setContent {
            EarthquakeTheme {
                EarthquakeControls(
                    items = listOf(item),
                    selectedItemId = selectedId,
                    selectedView = EarthquakeView.List,
                    expanded = true,
                    isListPanelVisible = isListPanelVisible,
                    monitoring = EarthquakeMonitoringState(),
                    onListPanelVisibilityChanged = { isListPanelVisible = it },
                    onItemClick = { selectedId = it.id },
                    onClearSelection = { selectedId = null },
                    onViewSelected = { viewSwitches++ },
                )
            }
        }

        composeRule.onNodeWithText("Map").assertDoesNotExist()
        composeRule.onNodeWithText("List").assertDoesNotExist()
        composeRule.onNodeWithText(title).performClick()
        composeRule.onAllNodesWithText(title).assertCountEquals(1)
        composeRule.onNodeWithText(title).assertIsSelected()
        composeRule.onNodeWithText("Selected earthquake").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Clear earthquake selection").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Minimize earthquake list").performClick()
        composeRule.onNodeWithText("Recent earthquakes").assertDoesNotExist()
        composeRule.onNodeWithText("Show list").assertIsDisplayed()
        composeRule.onAllNodesWithText(title).assertCountEquals(1)
        composeRule.onNodeWithContentDescription("Clear earthquake selection").assertIsDisplayed()
        composeRule.onNodeWithText("Show list").performClick()
        composeRule.onNodeWithText(title).assertIsSelected()
        composeRule.onNodeWithContentDescription("Clear earthquake selection").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Minimize earthquake list").performClick()
        composeRule.runOnIdle {
            assertEquals(item.id, selectedId)
            assertEquals(0, viewSwitches)
        }
        composeRule.onNodeWithContentDescription("Clear earthquake selection").performClick()
        composeRule.onNodeWithText(title).assertDoesNotExist()
        composeRule.onNodeWithText("Show list").performClick()
        composeRule.onAllNodesWithText(title).assertCountEquals(1)
        composeRule.runOnIdle { assertEquals(null, selectedId) }
    }

    @Test
    fun listSelectionAndViewSwitchingKeepTheSameEarthquake() {
        val title = "M6.0 - Shima, Japan"
        val item = EarthquakeMapItem("shima", 33.822, 136.9854, title)
        var state by mutableStateOf(EarthquakePreviewState(mapItems = listOf(item)))
        var selectedView by mutableStateOf(EarthquakeView.List)
        composeRule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.WindowSize(DpSize(400.dp, 600.dp))
            ) {
                EarthquakeTheme {
                    EarthquakeScreen(
                        state = state,
                        onMapItemClick = { state = state.copy(selectedMapItemId = it.id) },
                        onMapClick = { state = state.copy(selectedMapItemId = null) },
                        selectedView = selectedView,
                        onViewSelected = { selectedView = it },
                    )
                }
            }
        }

        composeRule.onNodeWithText(title).performClick()
        composeRule.onNodeWithText("Map").assertIsSelected()
        composeRule.onNodeWithText("Selected earthquake").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Clear earthquake selection").assertIsDisplayed()
        composeRule.onNodeWithText(title).assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(item.id, state.selectedMapItemId) }

        composeRule.onNodeWithText("List").performClick()
        composeRule.onNodeWithText("List").assertIsSelected()
        composeRule.onNodeWithText(title).assertIsSelected()
        composeRule.onNodeWithText("Selected earthquake").assertDoesNotExist()

        composeRule.onNodeWithText("Map").performClick()
        composeRule.onNodeWithContentDescription("Clear earthquake selection").performClick()
        composeRule.onNodeWithText("List").performClick()
        composeRule.runOnIdle { assertEquals(null, state.selectedMapItemId) }
        composeRule.onNodeWithText(title).assertIsDisplayed()
    }

    @Test
    fun fallbackTitleStaysVisibleAcrossListSelectionAndMinimizing() {
        val item = EarthquakeMapItem("unknown", 0.0, 0.0, title = null)
        var selectedId by mutableStateOf<String?>(null)
        var isListPanelVisible by mutableStateOf(true)
        composeRule.setContent {
            EarthquakeTheme {
                EarthquakeControls(
                    items = listOf(item),
                    selectedItemId = selectedId,
                    selectedView = EarthquakeView.List,
                    expanded = true,
                    isListPanelVisible = isListPanelVisible,
                    onListPanelVisibilityChanged = { isListPanelVisible = it },
                    onItemClick = { selectedId = it.id },
                    onClearSelection = { selectedId = null },
                    onViewSelected = {},
                )
            }
        }

        composeRule.onNodeWithText("Unknown Magnitude – Unknown location").performClick()
        composeRule.onNodeWithText("Unknown Magnitude – Unknown location").assertIsSelected()
        composeRule.onNodeWithContentDescription("Minimize earthquake list").performClick()
        composeRule.onNodeWithText("Unknown Magnitude – Unknown location").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Clear earthquake selection").assertIsDisplayed()
        composeRule.onNodeWithText("Show list").performClick()
        composeRule.onNodeWithText("Unknown Magnitude – Unknown location").assertIsSelected()
        composeRule.runOnIdle { assertEquals(item.id, selectedId) }
    }
}
