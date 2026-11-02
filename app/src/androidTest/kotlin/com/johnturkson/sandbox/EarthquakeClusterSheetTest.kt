package com.johnturkson.sandbox

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.espresso.Espresso.pressBack
import com.johnturkson.sandbox.theme.EarthquakeTheme
import com.johnturkson.sandbox.ui.EarthquakeClusterSheet
import com.johnturkson.sandbox.ui.EarthquakeMapFixtures
import com.johnturkson.sandbox.ui.EarthquakeMapItem
import com.johnturkson.sandbox.ui.EarthquakePreviewState
import com.johnturkson.sandbox.ui.EarthquakeScreen
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test

class EarthquakeClusterSheetTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun allMembersAreScrollableAndSelectionDoesNotDismissSheet() {
        val items = EarthquakeMapFixtures.denseItems
        var selected by mutableStateOf<String?>(null)
        var isOpen by mutableStateOf(true)
        composeRule.setContent {
            EarthquakeTheme {
                if (isOpen)
                    EarthquakeClusterSheet(
                        items = items,
                        selectedItemId = selected,
                        onItemClick = { selected = it.id },
                        onDismiss = { isOpen = false },
                    )
            }
        }
        composeRule.onNodeWithText("120 earthquakes in this area").assertIsDisplayed()
        composeRule.onNode(hasScrollToIndexAction()).performScrollToIndex(items.size)
        composeRule.onNodeWithText(items.last().title).performClick().assertIsSelected()
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
        composeRule.setContent {
            EarthquakeTheme {
                EarthquakeScreen(
                    state = state,
                    onMapItemClick = { state = state.copy(selectedMapItemId = it.id) },
                    onMapClick = { state = state.copy(selectedMapItemId = null) },
                    onClusterDismiss = { state = state.copy(selectedClusterItemIds = emptyList()) },
                )
            }
        }
        composeRule.onNodeWithContentDescription("Close earthquake cluster").assertIsDisplayed()
        pressBack()
        composeRule.onNodeWithContentDescription("Close earthquake cluster").assertDoesNotExist()
        composeRule.onNodeWithText("Selected earthquake").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(items.first().id, state.selectedMapItemId)
            assertEquals(emptyList(), state.selectedClusterItemIds)
        }
    }
}
