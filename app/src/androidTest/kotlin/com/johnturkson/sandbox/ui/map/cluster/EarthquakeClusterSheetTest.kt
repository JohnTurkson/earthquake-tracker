package com.johnturkson.sandbox.ui.map.cluster

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.WindowSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso.pressBack
import com.johnturkson.sandbox.theme.EarthquakeTheme
import com.johnturkson.sandbox.ui.event.EarthquakeMapItem
import com.johnturkson.sandbox.ui.map.EarthquakeMapFixtures
import com.johnturkson.sandbox.ui.map.EarthquakePreviewState
import com.johnturkson.sandbox.ui.map.EarthquakeScreen
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test

class EarthquakeClusterSheetTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun sheetHeightFollowsCompactAndMediumWindowHeightClasses() {
        var windowSize by mutableStateOf(DpSize(400.dp, 479.dp))
        var contentHeight = 0
        composeRule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.WindowSize(windowSize)) {
                EarthquakeTheme {
                    EarthquakeClusterSheet(
                        items = EarthquakeMapFixtures.denseItems,
                        selectedItemId = null,
                        onItemClick = {},
                        onDismiss = {},
                        modifier = Modifier.onSizeChanged { contentHeight = it.height },
                    )
                }
            }
        }
        for ((height, expectedMaximum) in
            listOf(479.dp to 215.55.dp, 480.dp to 288.dp, 599.dp to 359.4.dp, 600.dp to 360.dp)) {
            composeRule.runOnIdle { windowSize = DpSize(400.dp, height) }
            composeRule.runOnIdle {
                assertEquals(
                    with(composeRule.density) { expectedMaximum.roundToPx() },
                    contentHeight,
                )
            }
            composeRule.onNodeWithContentDescription("Close earthquake cluster").assertIsDisplayed()
            composeRule
                .onNode(hasScrollToIndexAction())
                .performScrollToIndex(EarthquakeMapFixtures.denseItems.size)
            composeRule
                .onNodeWithText(requireNotNull(EarthquakeMapFixtures.denseItems.last().title))
                .assertIsDisplayed()
        }
    }

    @Test
    fun allMembersAreScrollableAndSelectionDoesNotDismissSheet() {
        val items = EarthquakeMapFixtures.denseItems
        var selected by mutableStateOf<String?>(null)
        var isOpen by mutableStateOf(true)
        composeRule.setContent {
            EarthquakeTheme {
                if (isOpen) {
                    EarthquakeClusterSheet(
                        items = items,
                        selectedItemId = selected,
                        onItemClick = { selected = it.id },
                        onDismiss = { isOpen = false },
                    )
                }
            }
        }
        composeRule.onNodeWithText("120 earthquakes in this area").assertIsDisplayed()
        composeRule.onNode(hasScrollToIndexAction()).performScrollToIndex(items.size)
        composeRule
            .onNodeWithText(requireNotNull(items.last().title))
            .performClick()
            .assertIsSelected()
        composeRule.onNodeWithText("Selected earthquake").assertDoesNotExist()
        composeRule.onNodeWithText("120 earthquakes in this area").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(items.last().id, selected) }
        composeRule.onNodeWithContentDescription("Close earthquake cluster").performClick()
        composeRule.onNodeWithText("120 earthquakes in this area").assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(items.last().id, selected) }
    }

    @Test
    fun missingMembersShowEmptyStateAndRemainDismissible() {
        var items by mutableStateOf(EarthquakeMapFixtures.denseItems.take(2))
        var dismissed = false
        composeRule.setContent {
            EarthquakeTheme { EarthquakeClusterSheet(items, null, {}, { dismissed = true }) }
        }
        composeRule.runOnIdle { items = emptyList<EarthquakeMapItem>() }
        composeRule.onNodeWithText("These earthquakes are no longer available.").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Close earthquake cluster").performClick()
        composeRule.runOnIdle { assertEquals(true, dismissed) }
    }

    @Test
    fun backDismissesTheScreenSheetAndKeepsTheSelectedEarthquake() {
        val items = EarthquakeMapFixtures.denseItems.take(2)
        var state by
            mutableStateOf(
                EarthquakePreviewState(
                    mapItems = items,
                    selectedMapItemId = items.first().id,
                    selectedClusterItemIds = items.map { it.id },
                )
            )
        var windowSize by mutableStateOf(DpSize(400.dp, 600.dp))
        composeRule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.WindowSize(windowSize)) {
                EarthquakeTheme {
                    EarthquakeScreen(
                        state = state,
                        onMapItemClick = { state = state.copy(selectedMapItemId = it.id) },
                        onMapClick = { state = state.copy(selectedMapItemId = null) },
                        onClusterDismiss = {
                            state = state.copy(selectedClusterItemIds = emptyList())
                        },
                    )
                }
            }
        }
        for (expanded in listOf(false, true)) {
            composeRule.runOnIdle {
                windowSize =
                    DpSize(
                        if (expanded) {
                            900.dp
                        } else {
                            400.dp
                        },
                        600.dp,
                    )
                state = state.copy(selectedClusterItemIds = items.map { it.id })
            }
            composeRule.onNodeWithContentDescription("Close earthquake cluster").assertIsDisplayed()
            composeRule.onNodeWithText(requireNotNull(items.first().title)).assertIsSelected()
            composeRule.onNodeWithText("Selected earthquake").assertDoesNotExist()
            composeRule
                .onNodeWithContentDescription("Minimize earthquake list")
                .assertDoesNotExist()
            pressBack()
            composeRule
                .onNodeWithContentDescription("Close earthquake cluster")
                .assertDoesNotExist()
            composeRule.onNodeWithText(requireNotNull(items.first().title)).assertIsDisplayed()
            if (expanded) {
                composeRule
                    .onNodeWithContentDescription("Minimize earthquake list")
                    .assertIsDisplayed()
                composeRule
                    .onNodeWithContentDescription("Clear earthquake selection")
                    .assertDoesNotExist()
            } else {
                composeRule
                    .onNodeWithContentDescription("Clear earthquake selection")
                    .assertIsDisplayed()
            }
            composeRule.onNodeWithText("Selected earthquake").assertDoesNotExist()
            composeRule.runOnIdle {
                assertEquals(items.first().id, state.selectedMapItemId)
                assertEquals(emptyList(), state.selectedClusterItemIds)
            }
        }
    }
}
