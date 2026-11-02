package com.johnturkson.sandbox

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.johnturkson.sandbox.theme.EarthquakeTheme
import com.johnturkson.sandbox.ui.EarthquakeExplorerControls
import com.johnturkson.sandbox.ui.EarthquakeMapItem
import com.johnturkson.sandbox.ui.EarthquakeMonitoringCard
import com.johnturkson.sandbox.ui.EarthquakeMonitoringState
import com.johnturkson.sandbox.ui.EarthquakeNotificationPage
import com.johnturkson.sandbox.ui.EarthquakeNotificationState
import com.johnturkson.sandbox.ui.EarthquakeView
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test

class EarthquakeNotificationUiTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun permissionAndChannelStatesKeepPreferencesIndependent() {
        var state by mutableStateOf(EarthquakeNotificationState())
        var permissionRequests = 0
        var previewRequests = 0
        rule.setContent {
            EarthquakeTheme {
                EarthquakeNotificationPage(
                    state,
                    EarthquakeMonitoringState(),
                    {},
                    { permissionRequests++ },
                    { state = state.copy(alertsEnabled = it) },
                    { state = state.copy(minimumMagnitude = it) },
                    {},
                    {},
                    { previewRequests++ },
                    Modifier.fillMaxSize(),
                )
            }
        }
        rule.onNodeWithText("Checking notification settings…").assertIsDisplayed()
        rule.onNodeWithText("Notifications are disabled").assertDoesNotExist()
        rule.runOnIdle { state = state.copy(isChecking = false) }
        rule.onNodeWithText("Enable notifications").performClick()
        rule.runOnIdle {
            assertEquals(1, permissionRequests)
            state = state.copy(requiresSettings = true)
        }
        rule.onNodeWithText("Open Android settings").assertIsDisplayed()
        rule.onNodeWithContentDescription("Worldwide earthquake alerts").performClick()
        rule.onNodeWithText("Waiting for notification permission").assertIsDisplayed()
        rule.runOnIdle {
            state =
                state.copy(
                    permissionGranted = true,
                    appEnabled = true,
                    alertsChannelEnabled = false,
                )
        }
        rule.onNodeWithText("Notifications are disabled").assertDoesNotExist()
        rule.onNodeWithText("Earthquake alert channel is disabled").assertIsDisplayed()
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("Preview earthquake alert"))
        rule.onNodeWithText("Preview earthquake alert").assertIsNotEnabled()
        rule.runOnIdle { state = state.copy(alertsChannelEnabled = true) }
        rule.onNodeWithText("Preview earthquake alert").assertIsEnabled().performClick()
        rule.runOnIdle {
            assertEquals(1, previewRequests)
            assertEquals(true, state.alertsEnabled)
        }
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("M 7.0+"))
        rule.onNodeWithText("M 7.0+").performClick()
        rule.onNodeWithText("M 7.0+").assertIsSelected()
    }

    @Test
    fun refreshingDisablesDuplicateActionAndFailuresKeepTheTimestamp() {
        var state by
            mutableStateOf(
                EarthquakeMonitoringState(status = EarthquakeMonitoringState.Status.Running)
            )
        var requests = 0
        rule.setContent {
            EarthquakeTheme {
                EarthquakeMonitoringCard(state, { requests++ }, lastUpdatedLabel = "10:42")
            }
        }
        rule.onNodeWithText("Checks every minute · Updated 10:42").assertIsDisplayed()
        rule.onNodeWithContentDescription("Refresh earthquake data").performClick()
        rule.runOnIdle {
            assertEquals(1, requests)
            state = state.copy(refresh = EarthquakeMonitoringState.Refresh.Running)
        }
        rule.onNodeWithText("Existing data remains visible").assertIsDisplayed()
        rule.onNodeWithContentDescription("Refreshing earthquakes…").assertIsNotEnabled()
        rule.runOnIdle { state = state.copy(refresh = EarthquakeMonitoringState.Refresh.Failed) }
        rule.onNodeWithText("Couldn’t refresh earthquakes").assertIsDisplayed()
        rule.onNodeWithText("Saved data · Updated 10:42").assertIsDisplayed()
        rule.runOnIdle {
            state =
                state.copy(
                    status = EarthquakeMonitoringState.Status.Offline,
                    refresh = EarthquakeMonitoringState.Refresh.Failed,
                )
        }
        rule.onNodeWithText("Couldn’t refresh earthquakes").assertIsDisplayed()
        rule.runOnIdle { state = state.copy(refresh = EarthquakeMonitoringState.Refresh.Idle) }
        rule.onNodeWithText("Offline · Monitoring paused").assertIsDisplayed()
        rule.onNodeWithContentDescription("Refresh earthquake data").assertIsEnabled()
        rule.runOnIdle {
            state = EarthquakeMonitoringState(status = EarthquakeMonitoringState.Status.Failed)
        }
        rule.onNodeWithText("Couldn’t start monitoring · Try again").assertIsDisplayed()
    }

    @Test
    fun monitoringScrollsAwayAndPositionSurvivesMinimizeAndViewChanges() {
        val items = (1..50).map { EarthquakeMapItem("$it", 1.0, 2.0, "Earthquake $it") }
        var expanded by mutableStateOf(true)
        var panel by mutableStateOf(true)
        var view by mutableStateOf(EarthquakeView.List)
        lateinit var list: LazyListState
        rule.setContent {
            list = rememberLazyListState()
            EarthquakeTheme {
                EarthquakeExplorerControls(
                    items,
                    "1",
                    view,
                    expanded,
                    {},
                    {},
                    { view = it },
                    listState = list,
                    isListPanelVisible = panel,
                    onListPanelVisibilityChanged = { panel = it },
                    monitoring =
                        EarthquakeMonitoringState(
                            status = EarthquakeMonitoringState.Status.Running
                        ),
                )
            }
        }
        rule.onNodeWithText("Selected earthquake").assertDoesNotExist()
        rule.onNodeWithText("Live monitoring").assertIsDisplayed()
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("Earthquake 40"))
        rule.onNodeWithText("Live monitoring").assertDoesNotExist()
        var index = 0
        rule.runOnIdle { index = list.firstVisibleItemIndex }
        rule.onNodeWithContentDescription("Minimize earthquake list").performClick()
        rule.onNodeWithText("Live monitoring").assertIsDisplayed()
        rule.onNodeWithText("Earthquake 1").assertIsDisplayed()
        rule.onNodeWithContentDescription("Clear earthquake selection").assertIsDisplayed()
        rule.onNodeWithText("Selected earthquake").assertDoesNotExist()
        rule.onNodeWithText("Show list").performClick()
        rule.runOnIdle { assertEquals(index, list.firstVisibleItemIndex) }
        rule.runOnIdle { expanded = false }
        rule.onNodeWithText("Map").performClick()
        rule.onNodeWithText("Live monitoring").assertIsDisplayed()
        rule.onNodeWithText("List").performClick()
        rule.runOnIdle { assertEquals(index, list.firstVisibleItemIndex) }
        rule.onNodeWithText("Selected earthquake").assertDoesNotExist()
    }
}
