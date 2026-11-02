package com.johnturkson.sandbox

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
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
import org.junit.Rule
import org.junit.Test

class EarthquakeNavigationTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()
    private val owners = mutableMapOf<Destination, EarthquakeNavigationTestViewModel>()
    private var expanded by mutableStateOf(false)

    @Test
    fun destinationsExchangeResultsAndNotificationsReturnToThePreviousView() {
        composeRule.setContent { Content() }
        composeRule.onNodeWithText("Select last earthquake").performClick()
        composeRule.onNodeWithText("Open list").performClick()
        composeRule.onNodeWithText("Earthquake 79").assertIsSelected()
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
        composeRule.onNodeWithText("Earthquake 79").assertIsSelected()
    }

    @Test
    fun resizingKeepsIndependentOwnersAndDisplaysBothDestinations() {
        composeRule.setContent { Content() }
        composeRule.onNodeWithText("Open list").performClick()
        val listOwner = owners.getValue(Destination.EarthquakeListRoute)
        composeRule.runOnIdle { expanded = true }
        composeRule.onNodeWithText("Map destination").assertIsDisplayed()
        composeRule.onNodeWithText("Recent earthquakes").assertIsDisplayed()
        composeRule.onNodeWithText("Select last earthquake").performClick()
        composeRule.onNodeWithText("Earthquake 79").assertIsSelected()
        composeRule.onNode(hasScrollAction()).performScrollToIndex(0)
        composeRule.onNodeWithText("Select last earthquake").performClick()
        composeRule.onNodeWithText("Earthquake 79").assertIsSelected()
        composeRule.onNodeWithText("Notifications shortcut").performClick()
        composeRule.onNodeWithText("Notification destination").assertIsDisplayed()
        composeRule.onNodeWithText("Map destination").assertIsDisplayed()
        composeRule.onNodeWithText("Recent earthquakes").assertDoesNotExist()
        composeRule.onNodeWithText("Close notifications").performClick()
        composeRule.onNodeWithText("Earthquake 79").assertIsSelected()
        composeRule.onNodeWithContentDescription("Minimize earthquake list").performClick()
        composeRule.onNodeWithText("Recent earthquakes").assertDoesNotExist()
        composeRule.onNodeWithText("Map destination").assertIsDisplayed()
        composeRule.onNodeWithText("Open list").performClick()
        composeRule.onNodeWithText("Earthquake 79").assertIsSelected()
        composeRule.runOnIdle { expanded = false }
        composeRule.onNodeWithText("List").assertIsSelected()
        composeRule.onNodeWithText("Map destination").assertDoesNotExist()
        composeRule.onNodeWithText("Earthquake 79").assertIsSelected()
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
        composeRule.onNodeWithText("Earthquake 79").assertIsSelected()
        composeRule.runOnIdle {
            assertEquals(
                "event-79",
                owners.getValue(Destination.EarthquakeListRoute).selectedMapItemId,
            )
        }
    }

    @Composable
    private fun Content() {
        val widthPixels = LocalWindowInfo.current.containerSize.width.coerceAtLeast(1).toFloat()
        CompositionLocalProvider(
            LocalDensity provides Density(widthPixels / if (expanded) 1100f else 400f)
        ) {
            EarthquakeApp(
                destinationContent = { route, actions -> DestinationContent(route, actions) }
            )
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
                Box(Modifier.fillMaxSize()) {
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
                Column {
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
