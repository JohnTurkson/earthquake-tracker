package com.johnturkson.sandbox

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.WindowSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer
import com.johnturkson.sandbox.ui.Destination
import com.johnturkson.sandbox.ui.EarthquakeApp
import com.johnturkson.sandbox.ui.EarthquakeListScreen
import com.johnturkson.sandbox.ui.EarthquakeListState
import com.johnturkson.sandbox.ui.EarthquakeMapItem
import com.johnturkson.sandbox.ui.EarthquakeMonitoringState
import com.johnturkson.sandbox.ui.EarthquakeNavigationActions
import com.johnturkson.sandbox.ui.EarthquakeSelectionEffect
import com.johnturkson.sandbox.ui.EarthquakeSelectionResult
import com.johnturkson.sandbox.ui.EarthquakeView
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

class EarthquakeNavigationTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()
    private val owners = mutableMapOf<Destination, EarthquakeNavigationTestViewModel>()
    private var windowSize by mutableStateOf(DpSize(400.dp, 600.dp))

    @Test
    fun compactNotificationsOpenAndCloseImmediatelyFromMapAndList() {
        composeRule.setContent { Content() }
        val mapBounds = paneBounds("Map pane")
        assertImmediateNotifications("Notifications shortcut", "Map pane", mapBounds, back = false)
        composeRule.onNodeWithText("Open list").performClick()
        val listBounds = paneBounds("List pane")
        assertImmediateNotifications(null, "List pane", listBounds, back = true)
        composeRule.onNodeWithText("Map").performClick()
        composeRule.onNodeWithText("Map destination").assertIsDisplayed()
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithText("Open list").performClick()
        composeRule.mainClock.advanceTimeBy(64)
        val movingBounds = paneBounds("List pane")
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        assertTrue(movingBounds != paneBounds("List pane"))
    }

    @Test
    fun expandedNotificationsReplaceAndRestoreTheListImmediately() {
        windowSize = DpSize(900.dp, 600.dp)
        composeRule.setContent { Content() }
        val listBounds = paneBounds("List pane")
        assertImmediateNotifications(
            "Notifications shortcut",
            "List pane",
            listBounds,
            back = false,
        )
        composeRule.onNodeWithContentDescription("Minimize earthquake list").performClick()
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithText("Notifications shortcut").performClick()
        composeRule.mainClock.advanceTimeBy(64)
        composeRule.onNodeWithText("Notification destination").assertIsDisplayed()
        composeRule.onNodeWithText("Map destination").assertIsDisplayed()
        composeRule.onNodeWithTag("List pane").assertDoesNotExist()
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.mainClock.advanceTimeBy(64)
        composeRule.onNodeWithTag("Notifications pane").assertDoesNotExist()
        composeRule.onNodeWithTag("List pane").assertDoesNotExist()
        composeRule.onNodeWithText("Map destination").assertIsDisplayed()
        composeRule.mainClock.autoAdvance = true
    }

    @Test
    fun destinationsExchangeResultsAndNotificationsReturnToThePreviousView() {
        composeRule.setContent { Content() }
        composeRule.onNodeWithText("Select last earthquake").performClick()
        composeRule.onNodeWithText("Open list").performClick()
        assertLastEarthquakeSelected()
        val mapOwner = owners.getValue(Destination.EarthquakeMapRoute)
        val listOwner = owners.getValue(Destination.EarthquakeListRoute)
        assertNotSame(mapOwner, listOwner)
        composeRule.onNodeWithText("Earthquake 79").performClick()
        composeRule.onNodeWithText("Map selection: event-79").assertIsDisplayed()
        composeRule.onNodeWithText("Open list").performClick()
        composeRule.onNodeWithText("List").assertIsSelected()
        composeRule.onNodeWithText("Earthquake alerts", substring = false).assertDoesNotExist()
        composeRule.onNodeWithText("Notifications shortcut").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Earthquake alerts").performClick()
        composeRule.onNodeWithText("Notification destination").assertIsDisplayed()
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.onNodeWithText("List").assertIsSelected()
        composeRule.runOnIdle {
            assertSame(mapOwner, owners.getValue(Destination.EarthquakeMapRoute))
            assertSame(listOwner, owners.getValue(Destination.EarthquakeListRoute))
        }
        composeRule.onNodeWithText("Map").performClick()
        composeRule.onNodeWithText("Notifications shortcut").performClick()
        composeRule.onNodeWithText("Notification destination").assertIsDisplayed()
        composeRule.onNodeWithText("Close notifications").performClick()
        composeRule.onNodeWithText("Map selection: event-79").assertIsDisplayed()
        composeRule.onNodeWithText("Open list").performClick()
        assertLastEarthquakeSelected()
    }

    @Test
    fun resizingKeepsIndependentOwnersAndDisplaysBothDestinations() {
        composeRule.setContent { Content() }
        composeRule.onNodeWithText("Open list").performClick()
        composeRule.onNodeWithText("List").assertIsSelected()
        val listOwner = owners.getValue(Destination.EarthquakeListRoute)
        composeRule.runOnIdle { windowSize = DpSize(900.dp, 600.dp) }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onNodeWithText("Map destination").isDisplayed()
        }
        composeRule.onNodeWithText("Map destination").assertIsDisplayed()
        composeRule.onNodeWithText("Recent earthquakes").assertIsDisplayed()
        composeRule.onNodeWithText("Select last earthquake").performClick()
        assertLastEarthquakeSelected()
        composeRule.onNode(hasScrollAction()).performScrollToIndex(0)
        composeRule.onNodeWithText("Select last earthquake").performClick()
        assertLastEarthquakeSelected()
        composeRule.onNodeWithText("Notifications shortcut").performClick()
        composeRule.onNodeWithText("Notification destination").assertIsDisplayed()
        composeRule.onNodeWithText("Map destination").assertIsDisplayed()
        composeRule.onNodeWithText("Recent earthquakes").assertDoesNotExist()
        composeRule.onNodeWithText("Close notifications").performClick()
        assertLastEarthquakeSelected()
        composeRule.onNodeWithContentDescription("Minimize earthquake list").performClick()
        composeRule.onNodeWithText("Recent earthquakes").assertDoesNotExist()
        composeRule.onNodeWithText("Map destination").assertIsDisplayed()
        composeRule.onNodeWithText("Open list").performClick()
        assertLastEarthquakeSelected()
        composeRule.runOnIdle { windowSize = DpSize(400.dp, 600.dp) }
        composeRule.onNodeWithText("List").assertIsSelected()
        composeRule.onNodeWithText("Map destination").assertDoesNotExist()
        assertLastEarthquakeSelected()
        composeRule.runOnIdle {
            assertSame(listOwner, owners.getValue(Destination.EarthquakeListRoute))
        }
    }

    @Test
    fun pendingSelectionSurvivesSavedStateRestorationBeforeListIsComposed() {
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent { Content() }
        composeRule.onNodeWithText("Select last earthquake").performClick()
        restoration.emulateSavedInstanceStateRestore()
        composeRule.onNodeWithText("Open list").performClick()
        assertLastEarthquakeSelected()
        composeRule.runOnIdle {
            assertEquals(
                "event-79",
                owners.getValue(Destination.EarthquakeListRoute).selectedMapItemId,
            )
        }
    }

    @Test
    fun panesFollowWidthAndHeightSizeClassesAcrossResizeBoundaries() {
        composeRule.setContent { Content() }
        composeRule.onNodeWithText("Open list").performClick()
        composeRule.onNodeWithText("List").assertIsSelected()
        val listOwner = owners.getValue(Destination.EarthquakeListRoute)
        for ((size, showsBothDestinations) in
            listOf(
                DpSize(839.dp, 600.dp) to false,
                DpSize(840.dp, 600.dp) to true,
                DpSize(900.dp, 479.dp) to false,
                DpSize(900.dp, 480.dp) to true,
                DpSize(400.dp, 600.dp) to false,
            )) {
            composeRule.runOnIdle { windowSize = size }
            if (showsBothDestinations) {
                composeRule.onNodeWithText("Map destination").assertIsDisplayed()
                composeRule.onNodeWithText("Recent earthquakes").assertIsDisplayed()
            } else {
                composeRule.onNodeWithText("Map destination").assertDoesNotExist()
                composeRule.onNodeWithText("List").assertIsSelected()
            }
            composeRule.runOnIdle {
                assertSame(listOwner, owners.getValue(Destination.EarthquakeListRoute))
            }
        }
    }

    private fun assertLastEarthquakeSelected() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onNodeWithText("Earthquake 79").isDisplayed()
        }
        composeRule.onNodeWithText("Earthquake 79").assertIsSelected()
    }

    private fun paneBounds(tag: String): Rect =
        composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

    private fun assertImmediateNotifications(
        shortcut: String?,
        previousPane: String,
        previousBounds: Rect,
        back: Boolean,
    ) {
        composeRule.mainClock.autoAdvance = false
        if (shortcut != null) composeRule.onNodeWithText(shortcut).performClick()
        else composeRule.onNodeWithContentDescription("Earthquake alerts").performClick()
        composeRule.mainClock.advanceTimeBy(64)
        composeRule.onNodeWithTag(previousPane).assertDoesNotExist()
        val notificationBounds = paneBounds("Notifications pane")
        composeRule.mainClock.advanceTimeBy(64)
        assertEquals(notificationBounds, paneBounds("Notifications pane"))
        if (back) {
            composeRule.runOnUiThread {
                composeRule.activity.onBackPressedDispatcher.onBackPressed()
            }
        } else {
            composeRule.onNodeWithText("Close notifications").performClick()
        }
        composeRule.mainClock.advanceTimeBy(64)
        composeRule.onNodeWithTag("Notifications pane").assertDoesNotExist()
        assertEquals(previousBounds, paneBounds(previousPane))
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
    }

    @Composable
    private fun Content() {
        val availableSize =
            with(LocalDensity.current) { LocalWindowInfo.current.containerSize.toSize().toDpSize() }
        require(availableSize.width >= 900.dp && availableSize.height >= 600.dp) {
            "Run navigation tests in a window at least 900 dp wide and 600 dp tall."
        }
        Box(Modifier.fillMaxSize()) {
            DeviceConfigurationOverride(DeviceConfigurationOverride.WindowSize(windowSize)) {
                EarthquakeApp(
                    destinationContent = { route, actions -> DestinationContent(route, actions) }
                )
            }
        }
    }

    @Composable
    private fun DestinationContent(route: Destination, actions: EarthquakeNavigationActions) {
        val owner = viewModel { EarthquakeNavigationTestViewModel(createSavedStateHandle()) }
        owners[route] = owner
        when (route) {
            Destination.EarthquakeMapRoute -> {
                EarthquakeSelectionEffect(
                    "map_selection",
                    actions.pendingMapSelection,
                    { owner.applySelection(it, false) },
                    actions.onMapSelectionConsumed,
                )
                Box(Modifier.fillMaxSize().testTag("Map pane")) {
                    Column(
                        Modifier.align(Alignment.TopEnd)
                            .padding(
                                top = 40.dp,
                                end = if (actions.notificationsVisible) 400.dp else 16.dp,
                            )
                    ) {
                        Text("Map destination")
                        Text("Map selection: ${owner.selectedMapItemId}")
                        TextButton(
                            onClick = {
                                owner.applySelection(actions.onMapSelection("event-79"), false)
                            }
                        ) {
                            Text("Select last earthquake")
                        }
                        TextButton(
                            onClick = {
                                if (actions.expanded) actions.onListPanelVisibilityChanged(true)
                                else actions.onViewSelected(EarthquakeView.List)
                            }
                        ) {
                            Text("Open list")
                        }
                        TextButton(onClick = actions.onNotificationsClick) {
                            Text("Notifications shortcut")
                        }
                    }
                }
            }
            Destination.EarthquakeListRoute -> {
                EarthquakeSelectionEffect(
                    "list_selection",
                    actions.pendingListSelection,
                    { owner.applySelection(it, true) },
                    actions.onListSelectionConsumed,
                )
                EarthquakeListScreen(
                    modifier = Modifier.testTag("List pane"),
                    state = owner,
                    onItemClick = {
                        owner.applySelection(actions.onListSelection(it.id), false)
                        if (!actions.expanded) actions.onViewSelected(EarthquakeView.Map)
                    },
                    expanded = actions.expanded,
                    onMinimize = { actions.onListPanelVisibilityChanged(false) },
                    onViewSelected = actions.onViewSelected,
                    onNotificationsClick = actions.onNotificationsClick,
                    onScrollHandled = { owner.scrollRequest = null },
                )
            }
            Destination.EarthquakeNotificationsRoute ->
                Column(Modifier.fillMaxSize().testTag("Notifications pane")) {
                    Text("Notification destination")
                    TextButton(onClick = actions.onNotificationsClose) {
                        Text("Close notifications")
                    }
                }
        }
    }
}

class EarthquakeNavigationTestViewModel(savedStateHandle: SavedStateHandle) :
    ViewModel(), EarthquakeListState {
    override val mapItems =
        (0 until 80).map { EarthquakeMapItem("event-$it", 37.0, -122.0, "Earthquake $it") }
    override val monitoring = EarthquakeMonitoringState()
    override var selectedMapItemId: String? by
        savedStateHandle
            .saved(serializer = MutableStateSerializer<String?>()) { mutableStateOf(null) }
            .getValue(this, this::selectedMapItemId)
    override var scrollRequest: EarthquakeSelectionResult? by mutableStateOf(null)

    fun applySelection(result: EarthquakeSelectionResult, scroll: Boolean) {
        selectedMapItemId = result.earthquakeId
        scrollRequest = if (scroll) result else null
    }
}
