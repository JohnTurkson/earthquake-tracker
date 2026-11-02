package com.johnturkson.sandbox

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
import com.johnturkson.sandbox.ui.EarthquakeExplorerControls
import com.johnturkson.sandbox.ui.EarthquakeMapItem
import com.johnturkson.sandbox.ui.EarthquakeMonitoringCard
import com.johnturkson.sandbox.ui.EarthquakeMonitoringState
import com.johnturkson.sandbox.ui.EarthquakeNotificationPage
import com.johnturkson.sandbox.ui.EarthquakeNotificationRationale
import com.johnturkson.sandbox.ui.EarthquakeNotificationState
import com.johnturkson.sandbox.ui.EarthquakeView

class EarthquakeNotificationsScreenshotTest {
    @PreviewTest
    @Preview(widthDp = 360, locale = "en-US")
    @Preview(widthDp = 320, fontScale = 1.5f, locale = "en-US")
    @Composable
    fun monitoring(
        @PreviewParameter(EarthquakeMonitoringStateProvider::class) state: EarthquakeMonitoringState
    ) {
        EarthquakeTheme(dynamicColor = false) {
            Box(Modifier.padding(12.dp)) {
                EarthquakeMonitoringCard(
                    state,
                    {},
                    lastUpdatedLabel = if (state.lastUpdatedMillis != null) "10:42" else null,
                )
            }
        }
    }

    @PreviewTest
    @Preview(widthDp = 360, heightDp = 800, locale = "en-US")
    @Composable
    fun page(
        @PreviewParameter(EarthquakeNotificationStateProvider::class)
        state: EarthquakeNotificationState
    ) {
        Page(state)
    }

    @PreviewTest
    @Preview(widthDp = 320, heightDp = 800, fontScale = 1.5f, locale = "en-US")
    @Composable
    fun largePage() {
        Page(EarthquakeNotificationState(isChecking = false))
    }

    @PreviewTest
    @Preview(widthDp = 360, heightDp = 800, locale = "en-US")
    @Composable
    fun channelsAndMonitoring() {
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
    fun darkPage() {
        Page(EarthquakeNotificationState(isChecking = false), dark = true)
    }

    @PreviewTest
    @Preview(widthDp = 384, heightDp = 640, locale = "en-US")
    @Preview(widthDp = 320, heightDp = 800, fontScale = 1.5f, locale = "en-US")
    @Composable
    fun rationale() {
        EarthquakeTheme(dynamicColor = false) { EarthquakeNotificationRationale({}, {}) }
    }

    @PreviewTest
    @Preview(widthDp = 1000, heightDp = 720, locale = "en-US")
    @Composable
    fun expandedNotifications() {
        Controls(expanded = true, page = true)
    }

    @PreviewTest
    @Preview(widthDp = 1000, heightDp = 720, locale = "en-US")
    @Composable
    fun expandedList() {
        Controls(expanded = true)
    }

    @PreviewTest
    @Preview(widthDp = 1000, heightDp = 720, locale = "en-US")
    @Composable
    fun expandedScrolled() {
        Controls(expanded = true, scrolled = true)
    }

    @PreviewTest
    @Preview(widthDp = 1000, heightDp = 720, locale = "en-US")
    @Composable
    fun expandedMinimized() {
        Controls(expanded = true, minimized = true)
    }

    @PreviewTest
    @Preview(widthDp = 384, heightDp = 720, locale = "en-US")
    @Composable
    fun compactMapRefreshing() {
        Controls(expanded = false, minimized = true, refreshing = true)
    }

    @PreviewTest
    @Preview(widthDp = 384, heightDp = 720, locale = "en-US")
    @Composable
    fun compactScrolled() {
        Controls(expanded = false, scrolled = true)
    }

    @PreviewTest
    @Preview(widthDp = 360, heightDp = 800, locale = "en-US")
    @Composable
    fun monitoringPage(
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
    fun darkMonitoring() {
        EarthquakeTheme(darkTheme = true, dynamicColor = false) {
            EarthquakeMonitoringCard(
                EarthquakeMonitoringState(status = EarthquakeMonitoringState.Status.Offline),
                {},
            )
        }
    }

    @Composable
    private fun Page(state: EarthquakeNotificationState, dark: Boolean = false) {
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
                    "M 6.0 – Example earthquake $it",
                    snippet = "Oct 31, 2026 · 10:42 PM",
                )
            }
        EarthquakeTheme(dynamicColor = false) {
            EarthquakeExplorerControls(
                items,
                "1",
                if (minimized) EarthquakeView.Map else EarthquakeView.List,
                expanded,
                {},
                {},
                {},
                listState = rememberLazyListState(if (scrolled) 10 else 0),
                isListPanelVisible = !minimized,
                monitoring =
                    EarthquakeMonitoringState(
                        status = EarthquakeMonitoringState.Status.Running,
                        refresh =
                            if (refreshing) EarthquakeMonitoringState.Refresh.Running
                            else EarthquakeMonitoringState.Refresh.Idle,
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
        sequenceOf(
            EarthquakeMonitoringState(status = EarthquakeMonitoringState.Status.Checking),
            EarthquakeMonitoringState(),
            EarthquakeMonitoringState(lastUpdatedMillis = 1),
            EarthquakeMonitoringState(status = EarthquakeMonitoringState.Status.Starting),
            EarthquakeMonitoringState(status = EarthquakeMonitoringState.Status.Running),
            EarthquakeMonitoringState(
                status = EarthquakeMonitoringState.Status.Running,
                lastUpdatedMillis = 1,
            ),
            EarthquakeMonitoringState(
                status = EarthquakeMonitoringState.Status.Offline,
                lastUpdatedMillis = 1,
            ),
            EarthquakeMonitoringState(status = EarthquakeMonitoringState.Status.Failed),
            EarthquakeMonitoringState(refresh = EarthquakeMonitoringState.Refresh.Queued),
            EarthquakeMonitoringState(refresh = EarthquakeMonitoringState.Refresh.Running),
            EarthquakeMonitoringState(refresh = EarthquakeMonitoringState.Refresh.Offline),
            EarthquakeMonitoringState(
                refresh = EarthquakeMonitoringState.Refresh.Offline,
                hasStoredData = true,
            ),
            EarthquakeMonitoringState(
                refresh = EarthquakeMonitoringState.Refresh.NetworkError,
                lastUpdatedMillis = 1,
            ),
            EarthquakeMonitoringState(refresh = EarthquakeMonitoringState.Refresh.DataError),
            EarthquakeMonitoringState(
                refresh = EarthquakeMonitoringState.Refresh.StorageError,
                lastUpdatedMillis = 1,
            ),
        )
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
