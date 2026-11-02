package com.johnturkson.sandbox

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.johnturkson.sandbox.theme.EarthquakeTheme
import com.johnturkson.sandbox.ui.EarthquakeExplorerControls
import com.johnturkson.sandbox.ui.EarthquakeMapItem
import com.johnturkson.sandbox.ui.EarthquakeMonitoringState
import com.johnturkson.sandbox.ui.EarthquakePreviewState
import com.johnturkson.sandbox.ui.EarthquakeScreen
import com.johnturkson.sandbox.ui.EarthquakeView
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

class EarthquakeViewsTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun locationStaysAtBottomRightWithoutMovingMonitoringOrCoveringSelection() {
        val item = EarthquakeMapItem("fixture", 37.0, -122.0, "M 4.5 - California")
        var showLocation by mutableStateOf(false)
        var selectedId by mutableStateOf<String?>(null)
        var locationClicks = 0
        composeRule.setContent {
            EarthquakeTheme {
                EarthquakeExplorerControls(
                    items = listOf(item),
                    selectedItemId = selectedId,
                    selectedView = EarthquakeView.Map,
                    expanded = false,
                    onItemClick = {},
                    onClearSelection = {},
                    onViewSelected = {},
                    showLocationButton = showLocation,
                    onLocationClick = { locationClicks++ },
                    monitoring =
                        EarthquakeMonitoringState(
                            status = EarthquakeMonitoringState.Status.Running
                        ),
                )
            }
        }

        val monitoringBounds =
            composeRule.onNodeWithText("Live monitoring").fetchSemanticsNode().boundsInRoot
        composeRule.runOnIdle { showLocation = true }
        val rootBounds = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        val locationBounds =
            composeRule.onNodeWithText("Use my location").fetchSemanticsNode().boundsInRoot
        assertEquals(
            monitoringBounds,
            composeRule.onNodeWithText("Live monitoring").fetchSemanticsNode().boundsInRoot,
        )
        assertTrue(locationBounds.center.x > rootBounds.center.x)
        assertTrue(locationBounds.center.y > rootBounds.center.y)
        composeRule.runOnIdle { selectedId = item.id }
        assertEquals(
            locationBounds,
            composeRule.onNodeWithText("Use my location").fetchSemanticsNode().boundsInRoot,
        )
        val selectionBounds =
            composeRule
                .onNodeWithContentDescription("Clear earthquake selection")
                .fetchSemanticsNode()
                .boundsInRoot
        assertTrue(selectionBounds.bottom < locationBounds.top)
        composeRule.onNodeWithText("Use my location").performClick()
        composeRule.runOnIdle { assertEquals(1, locationClicks) }
    }

    @Test
    fun expandedPanelKeepsSelectionWithoutViewSwitching() {
        val item = EarthquakeMapItem("shima", 33.822, 136.9854, "M 6.0 - Shima, Japan")
        var selectedId by mutableStateOf<String?>(null)
        var isListPanelVisible by mutableStateOf(true)
        var viewSwitches = 0
        composeRule.setContent {
            EarthquakeTheme {
                EarthquakeExplorerControls(
                    items = listOf(item),
                    selectedItemId = selectedId,
                    selectedView = EarthquakeView.List,
                    expanded = true,
                    isListPanelVisible = isListPanelVisible,
                    onListPanelVisibilityChanged = { isListPanelVisible = it },
                    onItemClick = { selectedId = it.id },
                    onClearSelection = { selectedId = null },
                    onViewSelected = { viewSwitches++ },
                )
            }
        }

        composeRule.onNodeWithText("Map").assertDoesNotExist()
        composeRule.onNodeWithText("List").assertDoesNotExist()
        composeRule.onNodeWithText(item.title).performClick()
        composeRule.onAllNodesWithText(item.title).assertCountEquals(1)
        composeRule.onNodeWithText(item.title).assertIsSelected()
        composeRule.onNodeWithContentDescription("Clear earthquake selection").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Minimize earthquake list").performClick()
        composeRule.onNodeWithText("Recent earthquakes").assertDoesNotExist()
        composeRule.onNodeWithText("Show list").assertIsDisplayed()
        composeRule.onAllNodesWithText(item.title).assertCountEquals(1)
        composeRule.onNodeWithContentDescription("Clear earthquake selection").assertIsDisplayed()
        composeRule.onNodeWithText("Show list").performClick()
        composeRule.onNodeWithText(item.title).assertIsSelected()
        composeRule.onNodeWithContentDescription("Clear earthquake selection").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Minimize earthquake list").performClick()
        composeRule.runOnIdle {
            assertEquals(item.id, selectedId)
            assertEquals(0, viewSwitches)
        }
        composeRule.onNodeWithContentDescription("Clear earthquake selection").performClick()
        composeRule.onNodeWithText(item.title).assertDoesNotExist()
        composeRule.onNodeWithText("Show list").performClick()
        composeRule.onAllNodesWithText(item.title).assertCountEquals(1)
        composeRule.runOnIdle { assertEquals(null, selectedId) }
    }

    @Test
    fun listSelectionAndViewSwitchingKeepTheSameEarthquake() {
        val item = EarthquakeMapItem("shima", 33.822, 136.9854, "M 6.0 - Shima, Japan")
        var state by mutableStateOf(EarthquakePreviewState(mapItems = listOf(item)))
        var selectedView by mutableStateOf(EarthquakeView.List)
        composeRule.setContent {
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

        composeRule.onNodeWithText(item.title).performClick()
        composeRule.onNodeWithText("Map").assertIsSelected()
        composeRule.onNodeWithText("Selected earthquake").assertIsDisplayed()
        composeRule.onNodeWithText(item.title).assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(item.id, state.selectedMapItemId) }

        composeRule.onNodeWithText("List").performClick()
        composeRule.onNodeWithText("List").assertIsSelected()
        composeRule.onNodeWithText(item.title).assertIsSelected()

        composeRule.onNodeWithText("Map").performClick()
        composeRule.onNodeWithContentDescription("Clear earthquake selection").performClick()
        composeRule.onNodeWithText("List").performClick()
        composeRule.runOnIdle { assertEquals(null, state.selectedMapItemId) }
        composeRule.onNodeWithText(item.title).assertIsDisplayed()
    }
}
