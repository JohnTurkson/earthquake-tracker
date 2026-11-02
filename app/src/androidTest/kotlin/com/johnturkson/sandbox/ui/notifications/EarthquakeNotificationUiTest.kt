package com.johnturkson.sandbox.ui.notifications

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.johnturkson.sandbox.theme.EarthquakeTheme
import com.johnturkson.sandbox.ui.event.EarthquakeMapItem
import com.johnturkson.sandbox.ui.layout.EarthquakeControls
import com.johnturkson.sandbox.ui.layout.EarthquakeTopAppBar
import com.johnturkson.sandbox.ui.layout.EarthquakeView
import com.johnturkson.sandbox.ui.monitoring.EarthquakeMonitoringCard
import com.johnturkson.sandbox.ui.monitoring.EarthquakeMonitoringState
import kotlin.test.assertEquals
import kotlin.time.Instant
import org.junit.Rule
import org.junit.Test

class EarthquakeNotificationUiTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun monitoringStatusAndUpdateTimeUseSeparateTextAndNormalTimestampWeight() {
        var state by
            mutableStateOf(
                EarthquakeMonitoringState(
                    status = EarthquakeMonitoringState.Status.Running,
                    lastUpdated = Instant.parse("2026-11-01T10:42:00Z"),
                )
            )
        rule.setContent {
            EarthquakeTheme {
                EarthquakeMonitoringCard(
                    state,
                    {},
                    Modifier.width(640.dp),
                    lastUpdatedLabel =
                        if (state.lastUpdated != null) {
                            "10:42"
                        } else {
                            null
                        },
                )
            }
        }
        rule.onNodeWithText("Live monitoring", useUnmergedTree = true).assertIsDisplayed()
        val timestamp = rule.onNodeWithText("Updated 10:42", useUnmergedTree = true)
        timestamp.assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        timestamp.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        rule.runOnIdle {
            assertEquals(FontWeight.Normal, layouts.single().layoutInput.style.fontWeight)
            state = state.copy(refresh = EarthquakeMonitoringState.Refresh.Running)
        }
        timestamp.assertIsDisplayed()
        rule.runOnIdle { state = state.copy(refresh = EarthquakeMonitoringState.Refresh.Failed) }
        timestamp.assertIsDisplayed()
        rule.runOnIdle {
            state = state.copy(refresh = EarthquakeMonitoringState.Refresh.Idle, lastUpdated = null)
        }
        timestamp.assertDoesNotExist()
        rule
            .onNodeWithText("Checks every minute", substring = true, useUnmergedTree = true)
            .assertDoesNotExist()
        rule
            .onNodeWithText("Manual refresh available", substring = true, useUnmergedTree = true)
            .assertDoesNotExist()
    }

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
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("M7.0+"))
        rule.onNodeWithText("M7.0+").performClick()
        rule.onNodeWithText("M7.0+").assertIsSelected()
    }

    @Test
    fun everyMonitoringAndRefreshCombinationKeepsTheSameAppBarHeight() {
        var state by mutableStateOf(EarthquakeMonitoringState())
        rule.setContent {
            EarthquakeTheme(dynamicColor = false) {
                Column {
                    EarthquakeMonitoringCard(
                        state,
                        {},
                        Modifier.width(320.dp).testTag("compact-monitoring"),
                        lastUpdatedLabel = "10:42",
                    )
                    EarthquakeTopAppBar(
                        state,
                        false,
                        {},
                        {},
                        {},
                        Modifier.width(840.dp).testTag("expanded-monitoring"),
                        lastUpdatedLabel = "10:42",
                    )
                }
            }
        }
        for (status in EarthquakeMonitoringState.Status.entries) {
            for (refresh in EarthquakeMonitoringState.Refresh.entries) {
                rule.runOnIdle { state = state.copy(status = status, refresh = refresh) }
                rule.onNodeWithTag("compact-monitoring").assertHeightIsEqualTo(64.dp)
                rule.onNodeWithTag("expanded-monitoring").assertHeightIsEqualTo(64.dp)
            }
        }
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
        rule
            .onNodeWithContentDescription("Live monitoring · Updated 10:42", substring = true)
            .assertIsDisplayed()
        rule.onNodeWithContentDescription("Refresh earthquake data").performClick()
        rule.runOnIdle {
            assertEquals(1, requests)
            state = state.copy(refresh = EarthquakeMonitoringState.Refresh.Running)
        }
        rule
            .onNodeWithContentDescription(
                "Refreshing earthquakes… · Updated 10:42",
                substring = true,
            )
            .assertIsDisplayed()
        rule.onNodeWithContentDescription("Refreshing earthquakes…").assertIsNotEnabled()
        rule.runOnIdle { state = state.copy(refresh = EarthquakeMonitoringState.Refresh.Failed) }
        rule
            .onNodeWithContentDescription("Couldn’t refresh earthquakes", substring = true)
            .assertIsDisplayed()
        rule.onNodeWithContentDescription("Updated 10:42", substring = true).assertIsDisplayed()
        rule.runOnIdle { state = state.copy(refresh = EarthquakeMonitoringState.Refresh.Succeeded) }
        rule
            .onNodeWithContentDescription("Earthquakes updated", substring = true)
            .assertIsDisplayed()
        rule.onNodeWithContentDescription("Updated 10:42", substring = true).assertIsDisplayed()
        rule.onNodeWithContentDescription("Refresh earthquake data").assertIsEnabled()
        rule.runOnIdle {
            state = EarthquakeMonitoringState(status = EarthquakeMonitoringState.Status.Failed)
        }
        rule
            .onNodeWithContentDescription("Couldn’t start monitoring · Try again", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun expandedMonitoringStaysInAppBarAndListPositionSurvivesLayoutChanges() {
        val items = (1..50).map { EarthquakeMapItem("$it", 1.0, 2.0, "Earthquake $it") }
        var expanded by mutableStateOf(true)
        var panel by mutableStateOf(true)
        var view by mutableStateOf(EarthquakeView.List)
        lateinit var list: LazyListState
        rule.setContent {
            list = rememberLazyListState()
            EarthquakeTheme {
                EarthquakeControls(
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
        rule.onNodeWithContentDescription("Live monitoring", substring = true).assertIsDisplayed()
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("Earthquake 40"))
        rule.onNodeWithContentDescription("Live monitoring", substring = true).assertIsDisplayed()
        var index = 0
        var firstVisibleKey: Any? = null
        rule.runOnIdle {
            index = list.firstVisibleItemIndex
            firstVisibleKey = list.layoutInfo.visibleItemsInfo.first().key
        }
        rule.onNodeWithContentDescription("Minimize earthquake list").performClick()
        rule.onNodeWithContentDescription("Live monitoring", substring = true).assertIsDisplayed()
        rule.onNodeWithText("Earthquake 1").assertIsDisplayed()
        rule.onNodeWithContentDescription("Clear earthquake selection").assertIsDisplayed()
        rule.onNodeWithText("Selected earthquake").assertDoesNotExist()
        rule.onNodeWithText("Show list").performClick()
        rule.runOnIdle { assertEquals(index, list.firstVisibleItemIndex) }
        rule.runOnIdle { expanded = false }
        rule.onNodeWithText("Map").performClick()
        rule.onNodeWithContentDescription("Live monitoring", substring = true).assertIsDisplayed()
        rule.onNodeWithText("List").performClick()
        rule.runOnIdle {
            assertEquals(firstVisibleKey, list.layoutInfo.visibleItemsInfo.first().key)
        }
        rule.onNodeWithContentDescription("Live monitoring", substring = true).assertDoesNotExist()
        rule.onNodeWithText("Selected earthquake").assertDoesNotExist()
    }
}
