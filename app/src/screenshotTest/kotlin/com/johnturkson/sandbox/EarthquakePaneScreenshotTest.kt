package com.johnturkson.sandbox

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveComponentOverrideApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.AdaptStrategy
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.LocalAnimatedPaneOverride
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffold
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldDefaults
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldDestinationItem
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.calculateThreePaneScaffoldValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.johnturkson.sandbox.theme.EarthquakeTheme
import com.johnturkson.sandbox.ui.EarthquakeListPanel
import com.johnturkson.sandbox.ui.EarthquakeMapItem
import com.johnturkson.sandbox.ui.EarthquakeMonitoringState
import com.johnturkson.sandbox.ui.EarthquakeNotificationState
import com.johnturkson.sandbox.ui.EarthquakeNotificationsScreen
import com.johnturkson.sandbox.ui.EarthquakePaneOverride

class EarthquakePaneScreenshotTest {
    @PreviewTest
    @Preview(widthDp = 900, heightDp = 720, locale = "en-US")
    @Preview(
        widthDp = 900,
        heightDp = 720,
        locale = "en-US",
        uiMode = Configuration.UI_MODE_NIGHT_YES,
    )
    @Preview(widthDp = 900, heightDp = 600, locale = "en-US", fontScale = 1.5f)
    @Composable
    fun notifications() {
        Pane(notifications = true)
    }

    @PreviewTest
    @Preview(widthDp = 900, heightDp = 720, locale = "en-US")
    @Preview(
        widthDp = 900,
        heightDp = 720,
        locale = "en-US",
        uiMode = Configuration.UI_MODE_NIGHT_YES,
    )
    @Preview(widthDp = 900, heightDp = 600, locale = "en-US", fontScale = 1.5f)
    @Composable
    fun list() {
        Pane(notifications = false)
    }

    @PreviewTest
    @Preview(widthDp = 400, heightDp = 720, locale = "en-US")
    @Composable
    fun compactNotifications() {
        Pane(notifications = true, expanded = false)
    }

    @OptIn(
        ExperimentalMaterial3AdaptiveApi::class,
        ExperimentalMaterial3AdaptiveComponentOverrideApi::class,
    )
    @Composable
    private fun Pane(notifications: Boolean, expanded: Boolean = true) {
        EarthquakeTheme(darkTheme = isSystemInDarkTheme(), dynamicColor = false) {
            val original = LocalAnimatedPaneOverride.current
            val override = remember(original) { EarthquakePaneOverride(original) }
            val strategies =
                SupportingPaneScaffoldDefaults.adaptStrategies(
                    supportingPaneAdaptStrategy =
                        if (expanded) AdaptStrategy.Levitate(Alignment.CenterStart)
                        else AdaptStrategy.Hide,
                    extraPaneAdaptStrategy =
                        if (expanded) AdaptStrategy.Levitate(Alignment.CenterEnd)
                        else AdaptStrategy.Hide,
                )
            val value =
                calculateThreePaneScaffoldValue(
                    maxHorizontalPartitions = 1,
                    adaptStrategies = strategies,
                    currentDestination =
                        ThreePaneScaffoldDestinationItem<Any>(
                            if (notifications) SupportingPaneScaffoldRole.Extra
                            else SupportingPaneScaffoldRole.Supporting
                        ),
                )
            CompositionLocalProvider(LocalAnimatedPaneOverride provides override) {
                SupportingPaneScaffold(
                    directive =
                        calculatePaneScaffoldDirective(currentWindowAdaptiveInfoV2())
                            .copy(maxHorizontalPartitions = 1, maxVerticalPartitions = 1),
                    value = value,
                    mainPane = {
                        AnimatedPane { Box(Modifier.fillMaxSize().background(Color(0xFF80BFB5))) }
                    },
                    supportingPane = {
                        AnimatedPane(Modifier.preferredWidth(392.dp).preferredHeight(1f)) {
                            EarthquakeListPanel(
                                items =
                                    listOf(
                                        EarthquakeMapItem(
                                            "fixture",
                                            37.0,
                                            -122.0,
                                            "Northern California",
                                            magnitude = 5.2,
                                        )
                                    ),
                                selectedItemId = null,
                                onItemClick = {},
                                onMinimize = {},
                                listState = rememberLazyListState(),
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                            )
                        }
                    },
                    extraPane = {
                        AnimatedPane(Modifier.preferredWidth(392.dp).preferredHeight(1f)) {
                            EarthquakeNotificationsScreen(
                                state =
                                    EarthquakeNotificationState(
                                        isChecking = false,
                                        permissionGranted = true,
                                        appEnabled = true,
                                    ),
                                monitoring =
                                    EarthquakeMonitoringState(
                                        status = EarthquakeMonitoringState.Status.Running
                                    ),
                                onClose = {},
                                expanded = expanded,
                            )
                        }
                    },
                )
            }
        }
    }
}
