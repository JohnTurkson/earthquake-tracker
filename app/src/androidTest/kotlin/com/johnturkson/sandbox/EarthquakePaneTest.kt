package com.johnturkson.sandbox

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.WindowSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.johnturkson.sandbox.ui.Destination
import com.johnturkson.sandbox.ui.EarthquakeApp
import com.johnturkson.sandbox.ui.EarthquakeMonitoringState
import com.johnturkson.sandbox.ui.EarthquakeNotificationState
import com.johnturkson.sandbox.ui.EarthquakeNotificationsScreen
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test

class EarthquakePaneTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()
    private var expanded by mutableStateOf(true)
    private var mapClicks by mutableIntStateOf(0)
    private var density = 1f
    private val background = Color(0xFF80BFB5)

    @Test
    fun floatingNotificationsDoNotShadeTheTransparentPaneArea() {
        composeRule.setContent { Content() }
        composeRule.onNodeWithText("Open alerts").performClick()
        composeRule.onNodeWithText("Earthquake alerts").assertIsDisplayed()
        val pixels = composeRule.onNodeWithTag("pane-host").captureToImage().toPixelMap()
        listOf(40, 180, 340).forEach { offset ->
            val x = pixels.width - (offset * density).toInt()
            val y = (40 * density).toInt()
            assertEquals(background.toArgb(), pixels[x, y].toArgb())
        }
        composeRule.onNodeWithText("Map taps: 0").performClick()
        composeRule.onNodeWithText("Map taps: 1").assertIsDisplayed()
        composeRule.onNodeWithText("Earthquake alerts").assertIsDisplayed()
    }

    @Test
    fun notificationStateSurvivesSwitchingBetweenFloatingAndCompactLayouts() {
        composeRule.setContent { Content() }
        composeRule.onNodeWithText("Open alerts").performClick()
        composeRule.onNodeWithText("M 7.0+").performClick()
        composeRule.onNodeWithText("M 7.0+").assertIsSelected()
        composeRule.runOnIdle { expanded = false }
        composeRule.onNodeWithText("M 7.0+").assertIsSelected()
        composeRule.onNodeWithText("Map taps: 0").assertDoesNotExist()
        composeRule.runOnIdle { expanded = true }
        composeRule.onNodeWithText("M 7.0+").assertIsSelected()
        composeRule.onNodeWithText("Map taps: 0").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Close earthquake alerts").performClick()
        composeRule.onNodeWithText("Earthquake alerts").assertDoesNotExist()
        composeRule.onNodeWithText("Open alerts").performClick()
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.onNodeWithText("Earthquake alerts").assertDoesNotExist()
    }

    @Composable
    private fun Content() {
        density = LocalDensity.current.density
        require(LocalWindowInfo.current.containerSize.width / density >= 900f)
        require(LocalWindowInfo.current.containerSize.height / density >= 600f)
        DeviceConfigurationOverride(
            DeviceConfigurationOverride.WindowSize(DpSize(if (expanded) 900.dp else 400.dp, 600.dp))
        ) {
            EarthquakeApp(Modifier.fillMaxSize().testTag("pane-host")) { route, actions ->
                when (route) {
                    Destination.EarthquakeMapRoute ->
                        Box(Modifier.fillMaxSize().background(background)) {
                            TextButton(
                                onClick = actions.onNotificationsClick,
                                modifier = Modifier.align(Alignment.CenterStart),
                            ) {
                                Text("Open alerts")
                            }
                            TextButton(
                                onClick = { mapClicks++ },
                                modifier = Modifier.align(Alignment.BottomStart),
                            ) {
                                Text("Map taps: $mapClicks")
                            }
                        }
                    Destination.EarthquakeListRoute -> Text("List destination")
                    Destination.EarthquakeNotificationsRoute -> {
                        var magnitude by rememberSaveable { mutableIntStateOf(6) }
                        EarthquakeNotificationsScreen(
                            state =
                                EarthquakeNotificationState(
                                    isChecking = false,
                                    permissionGranted = true,
                                    appEnabled = true,
                                    minimumMagnitude = magnitude,
                                ),
                            monitoring =
                                EarthquakeMonitoringState(
                                    status = EarthquakeMonitoringState.Status.Stopped
                                ),
                            onClose = actions.onNotificationsClose,
                            onMagnitudeSelected = { magnitude = it },
                            expanded = actions.expanded,
                        )
                    }
                }
            }
        }
    }
}
