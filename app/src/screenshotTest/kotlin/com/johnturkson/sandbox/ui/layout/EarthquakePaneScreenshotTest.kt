package com.johnturkson.sandbox.ui.layout

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.AdaptStrategy
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffold
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldDefaults
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldDestinationItem
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.calculateThreePaneScaffoldValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.johnturkson.sandbox.theme.EarthquakeTheme
import com.johnturkson.sandbox.ui.event.EarthquakeMapItem
import com.johnturkson.sandbox.ui.list.EarthquakeListPanel
import com.johnturkson.sandbox.ui.monitoring.EarthquakeMonitoringState
import com.johnturkson.sandbox.ui.notifications.EarthquakeNotificationState
import com.johnturkson.sandbox.ui.notifications.EarthquakeNotificationsScreen

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
    fun Notifications() {
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
    fun List() {
        Pane(notifications = false)
    }

    @PreviewTest
    @Preview(widthDp = 400, heightDp = 720, locale = "en-US")
    @Composable
    fun CompactNotifications() {
        Pane(notifications = true, expanded = false)
    }

    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    @Composable
    private fun Pane(notifications: Boolean, expanded: Boolean = true) {
        EarthquakeTheme(darkTheme = isSystemInDarkTheme(), dynamicColor = false) {
            val strategies =
                SupportingPaneScaffoldDefaults.adaptStrategies(
                    supportingPaneAdaptStrategy =
                        if (expanded) {
                            AdaptStrategy.Levitate(Alignment.CenterStart)
                        } else {
                            AdaptStrategy.Hide
                        },
                    extraPaneAdaptStrategy =
                        if (expanded) {
                            AdaptStrategy.Levitate(Alignment.CenterEnd)
                        } else {
                            AdaptStrategy.Hide
                        },
                )
            val value =
                calculateThreePaneScaffoldValue(
                    maxHorizontalPartitions = 1,
                    adaptStrategies = strategies,
                    currentDestination =
                        ThreePaneScaffoldDestinationItem<Any>(
                            if (notifications) {
                                SupportingPaneScaffoldRole.Extra
                            } else {
                                SupportingPaneScaffoldRole.Supporting
                            }
                        ),
                )
            EarthquakePaneOverrides {
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
                                modifier =
                                    Modifier.padding(
                                        top =
                                            if (expanded) {
                                                64.dp
                                            } else {
                                                0.dp
                                            }
                                    ),
                            )
                        }
                    },
                )
            }
        }
    }
}
