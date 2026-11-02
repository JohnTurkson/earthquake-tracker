package com.johnturkson.sandbox.ui.notifications

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.johnturkson.sandbox.theme.EarthquakeTheme
import com.johnturkson.sandbox.ui.event.EarthquakeMapItem
import com.johnturkson.sandbox.ui.layout.EarthquakeControls
import com.johnturkson.sandbox.ui.layout.EarthquakeTopAppBar
import com.johnturkson.sandbox.ui.layout.EarthquakeView
import com.johnturkson.sandbox.ui.monitoring.EarthquakeMonitoringCard
import com.johnturkson.sandbox.ui.monitoring.EarthquakeMonitoringState
import kotlin.time.Instant

class EarthquakeNotificationsScreenshotTest {
    @PreviewTest
    @Preview(widthDp = 360, locale = "en-US")
    @Preview(widthDp = 360, locale = "en-US", uiMode = Configuration.UI_MODE_NIGHT_YES)
    @Preview(widthDp = 320, fontScale = 1.5f, locale = "en-US")
    @Composable
    fun Monitoring(
        @PreviewParameter(EarthquakeMonitoringStateProvider::class) state: EarthquakeMonitoringState
    ) {
        EarthquakeTheme(dynamicColor = false) {
            Box(Modifier.padding(12.dp)) {
                EarthquakeMonitoringCard(
                    state,
                    {},
                    lastUpdatedLabel =
                        if (state.lastUpdated != null) {
                            "10:42"
                        } else {
                            null
                        },
                )
            }
        }
    }

    @PreviewTest
    @Preview(widthDp = 840, locale = "en-US")
    @Preview(widthDp = 840, locale = "en-US", uiMode = Configuration.UI_MODE_NIGHT_YES)
    @Preview(widthDp = 840, fontScale = 1.5f, locale = "en-US")
    @Composable
    fun ExpandedMonitoring(
        @PreviewParameter(EarthquakeMonitoringStateProvider::class) state: EarthquakeMonitoringState
    ) {
        EarthquakeTheme(dynamicColor = false) {
            EarthquakeTopAppBar(
                monitoring = state,
                isListPanelVisible = false,
                onListPanelVisibilityChanged = {},
                onRefresh = {},
                onNotificationsClick = {},
                lastUpdatedLabel =
                    if (state.lastUpdated != null) {
                        "10:42"
                    } else {
                        null
                    },
            )
        }
    }

    @PreviewTest
    @Preview(widthDp = 840, locale = "en-US")
    @Preview(widthDp = 840, locale = "en-US", uiMode = Configuration.UI_MODE_NIGHT_YES)
    @Preview(widthDp = 840, fontScale = 1.5f, locale = "en-US")
    @Composable
    fun ExpandedMonitoringWithPanel(
        @PreviewParameter(EarthquakeMonitoringStateProvider::class) state: EarthquakeMonitoringState
    ) {
        EarthquakeTheme(dynamicColor = false) {
            EarthquakeTopAppBar(
                monitoring = state,
                isListPanelVisible = true,
                onListPanelVisibilityChanged = {},
                onRefresh = {},
                onNotificationsClick = {},
                lastUpdatedLabel =
                    if (state.lastUpdated != null) {
                        "10:42"
                    } else {
                        null
                    },
            )
        }
    }

    @PreviewTest
    @Preview(widthDp = 360, heightDp = 800, locale = "en-US")
    @Composable
    fun Page(
        @PreviewParameter(EarthquakeNotificationStateProvider::class)
        state: EarthquakeNotificationState
    ) {
        NotificationPageContent(state)
    }

    @PreviewTest
    @Preview(widthDp = 320, heightDp = 800, fontScale = 1.5f, locale = "en-US")
    @Composable
    fun LargePage() {
        NotificationPageContent(EarthquakeNotificationState(isChecking = false))
    }

    @PreviewTest
    @Preview(widthDp = 360, heightDp = 800, locale = "en-US")
    @Composable
    fun ChannelsAndMonitoring() {
        EarthquakeTheme(dynamicColor = false) {
            EarthquakeNotificationPage(
                EarthquakeNotificationState(
                    isChecking = false,
                    permissionGranted = true,
                    appEnabled = true,
                    monitoringChannelEnabled = false,
                    alertsEnabled = true,
                    settingsError = true,
                ),
                EarthquakeMonitoringState(status = EarthquakeMonitoringState.Status.Running),
                {},
                {},
                {},
                {},
                {},
                {},
                {},
                Modifier.fillMaxSize(),
                listState = rememberLazyListState(3),
            )
        }
    }

    @PreviewTest
    @Preview(widthDp = 360, heightDp = 800, locale = "en-US")
    @Composable
    fun DarkPage() {
        NotificationPageContent(EarthquakeNotificationState(isChecking = false), dark = true)
    }

    @PreviewTest
    @Preview(widthDp = 360, heightDp = 800, locale = "en-US")
    @Composable
    fun CheckingWithAlertsSelected() {
        NotificationPageContent(EarthquakeNotificationState(alertsEnabled = true))
    }

    @PreviewTest
    @Preview(widthDp = 360, heightDp = 400, locale = "en-US")
    @Preview(
        widthDp = 360,
        heightDp = 400,
        locale = "en-US",
        uiMode = Configuration.UI_MODE_NIGHT_YES,
    )
    @Preview(widthDp = 320, heightDp = 480, fontScale = 1.5f, locale = "en-US")
    @Composable
    fun PageActions(
        @PreviewParameter(EarthquakeNotificationActionStateProvider::class)
        state: EarthquakeNotificationState
    ) {
        NotificationPageContent(state, firstVisibleItemIndex = 4)
    }

    @PreviewTest
    @Preview(widthDp = 384, heightDp = 640, locale = "en-US")
    @Preview(
        widthDp = 384,
        heightDp = 640,
        locale = "en-US",
        uiMode = Configuration.UI_MODE_NIGHT_YES,
    )
    @Preview(widthDp = 320, heightDp = 720, fontScale = 1.5f, locale = "en-US")
    @Composable
    fun CompactNotificationScreen() {
        EarthquakeTheme(dynamicColor = false) {
            EarthquakeNotificationsScreen(
                state = EarthquakeNotificationState(isChecking = false),
                monitoring = EarthquakeMonitoringState(),
                onClose = {},
            )
        }
    }

    @PreviewTest
    @Preview(widthDp = 392, heightDp = 720, locale = "en-US")
    @Preview(
        widthDp = 392,
        heightDp = 720,
        locale = "en-US",
        uiMode = Configuration.UI_MODE_NIGHT_YES,
    )
    @Preview(widthDp = 392, heightDp = 720, locale = "en-US", fontScale = 1.5f)
    @Composable
    fun ExpandedNotificationScreen() {
        EarthquakeTheme(dynamicColor = false) {
            EarthquakeNotificationsScreen(
                state = EarthquakeNotificationState(isChecking = false),
                monitoring = EarthquakeMonitoringState(),
                onClose = {},
                expanded = true,
                modifier = Modifier.padding(top = 64.dp),
            )
        }
    }

    @PreviewTest
    @Preview(widthDp = 384, heightDp = 640, locale = "en-US")
    @Preview(
        widthDp = 384,
        heightDp = 640,
        locale = "en-US",
        uiMode = Configuration.UI_MODE_NIGHT_YES,
    )
    @Preview(widthDp = 320, heightDp = 800, fontScale = 1.5f, locale = "en-US")
    @Composable
    fun Rationale() {
        EarthquakeTheme(dynamicColor = false) { EarthquakeNotificationRationale({}, {}) }
    }

    @PreviewTest
    @Preview(widthDp = 1000, heightDp = 720, locale = "en-US")
    @Composable
    fun ExpandedNotifications() {
        Controls(expanded = true, page = true)
    }

    @PreviewTest
    @Preview(widthDp = 1000, heightDp = 720, locale = "en-US")
    @Composable
    fun ExpandedList() {
        Controls(expanded = true)
    }

    @PreviewTest
    @Preview(widthDp = 1000, heightDp = 720, locale = "en-US")
    @Composable
    fun ExpandedScrolled() {
        Controls(expanded = true, scrolled = true)
    }

    @PreviewTest
    @Preview(widthDp = 1000, heightDp = 720, locale = "en-US")
    @Composable
    fun ExpandedMinimized() {
        Controls(expanded = true, minimized = true)
    }

    @PreviewTest
    @Preview(widthDp = 384, heightDp = 720, locale = "en-US")
    @Composable
    fun CompactMapRefreshing() {
        Controls(expanded = false, minimized = true, refreshing = true)
    }

    @PreviewTest
    @Preview(widthDp = 384, heightDp = 720, locale = "en-US")
    @Composable
    fun CompactScrolled() {
        Controls(expanded = false, scrolled = true)
    }

    @PreviewTest
    @Preview(widthDp = 360, heightDp = 800, locale = "en-US")
    @Composable
    fun MonitoringPage(
        @PreviewParameter(EarthquakeMonitoringStatusProvider::class)
        status: EarthquakeMonitoringState.Status
    ) {
        EarthquakeTheme(dynamicColor = false) {
            EarthquakeNotificationPage(
                EarthquakeNotificationState(
                    isChecking = false,
                    permissionGranted = true,
                    appEnabled = true,
                    alertsEnabled = true,
                ),
                EarthquakeMonitoringState(status = status),
                {},
                {},
                {},
                {},
                {},
                {},
                {},
                Modifier.fillMaxSize(),
            )
        }
    }

    @PreviewTest
    @Preview(widthDp = 360, locale = "en-US")
    @Composable
    fun DarkMonitoring() {
        EarthquakeTheme(darkTheme = true, dynamicColor = false) {
            EarthquakeMonitoringCard(
                EarthquakeMonitoringState(refresh = EarthquakeMonitoringState.Refresh.Failed),
                {},
            )
        }
    }

    @PreviewTest
    @Preview(widthDp = 360, locale = "en-US")
    @Composable
    fun DarkSuccessfulRefresh() {
        EarthquakeTheme(darkTheme = true, dynamicColor = false) {
            EarthquakeMonitoringCard(
                EarthquakeMonitoringState(
                    status = EarthquakeMonitoringState.Status.Running,
                    refresh = EarthquakeMonitoringState.Refresh.Succeeded,
                ),
                {},
                lastUpdatedLabel = "10:42",
            )
        }
    }

    @Composable
    private fun NotificationPageContent(
        state: EarthquakeNotificationState,
        dark: Boolean = isSystemInDarkTheme(),
        firstVisibleItemIndex: Int = 0,
    ) {
        EarthquakeTheme(darkTheme = dark, dynamicColor = false) {
            EarthquakeNotificationPage(
                state,
                EarthquakeMonitoringState(),
                {},
                {},
                {},
                {},
                {},
                {},
                {},
                Modifier.fillMaxSize(),
                listState = rememberLazyListState(firstVisibleItemIndex),
            )
        }
    }

    @Composable
    private fun Controls(
        expanded: Boolean,
        page: Boolean = false,
        scrolled: Boolean = false,
        minimized: Boolean = false,
        refreshing: Boolean = false,
    ) {
        val items =
            (1..40).map {
                EarthquakeMapItem(
                    "$it",
                    33.8,
                    136.9,
                    "M6.0 – Example earthquake $it",
                    snippet = "Oct 31, 2026 · 10:42 PM",
                )
            }
        EarthquakeTheme(dynamicColor = false) {
            EarthquakeControls(
                items,
                "1",
                if (minimized) {
                    EarthquakeView.Map
                } else {
                    EarthquakeView.List
                },
                expanded,
                {},
                {},
                {},
                listState =
                    rememberLazyListState(
                        if (scrolled) {
                            10
                        } else {
                            0
                        }
                    ),
                isListPanelVisible = !minimized,
                monitoring =
                    EarthquakeMonitoringState(
                        status = EarthquakeMonitoringState.Status.Running,
                        refresh =
                            if (refreshing) {
                                EarthquakeMonitoringState.Refresh.Running
                            } else {
                                EarthquakeMonitoringState.Refresh.Idle
                            },
                    ),
                notificationsVisible = page,
                notificationContent = { modifier ->
                    EarthquakeNotificationPage(
                        EarthquakeNotificationState(isChecking = false),
                        EarthquakeMonitoringState(),
                        {},
                        {},
                        {},
                        {},
                        {},
                        {},
                        {},
                        modifier,
                    )
                },
            )
        }
    }
}

class EarthquakeMonitoringStateProvider : PreviewParameterProvider<EarthquakeMonitoringState> {
    override val values =
        EarthquakeMonitoringState.Refresh.entries.asSequence().flatMap { refresh ->
            sequenceOf(
                EarthquakeMonitoringState(refresh = refresh),
                EarthquakeMonitoringState(
                    refresh = refresh,
                    lastUpdated = Instant.fromEpochMilliseconds(1),
                ),
            )
        }
}

class EarthquakeNotificationStateProvider : PreviewParameterProvider<EarthquakeNotificationState> {
    private val enabled =
        EarthquakeNotificationState(
            isChecking = false,
            permissionGranted = true,
            appEnabled = true,
            alertsEnabled = true,
        )
    override val values =
        sequenceOf(
            EarthquakeNotificationState(),
            EarthquakeNotificationState(isChecking = false),
            EarthquakeNotificationState(
                isChecking = false,
                requiresSettings = true,
                alertsEnabled = true,
            ),
            enabled,
            enabled.copy(appEnabled = false, requiresSettings = true),
            enabled.copy(alertsChannelEnabled = false),
            enabled.copy(monitoringChannelEnabled = false),
            enabled.copy(alertsEnabled = false),
            enabled.copy(minimumMagnitude = 5),
            enabled.copy(minimumMagnitude = 7),
        )
}

class EarthquakeMonitoringStatusProvider :
    PreviewParameterProvider<EarthquakeMonitoringState.Status> {
    override val values = EarthquakeMonitoringState.Status.entries.asSequence()
}

class EarthquakeNotificationActionStateProvider :
    PreviewParameterProvider<EarthquakeNotificationState> {
    private val enabled =
        EarthquakeNotificationState(
            isChecking = false,
            permissionGranted = true,
            appEnabled = true,
            alertsEnabled = true,
        )
    override val values =
        sequenceOf(
            enabled,
            enabled.copy(alertsEnabled = false),
            enabled.copy(settingsError = true),
            enabled.copy(alertsChannelEnabled = false),
        )
}
