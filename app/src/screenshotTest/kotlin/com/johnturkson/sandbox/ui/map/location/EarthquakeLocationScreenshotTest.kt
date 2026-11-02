package com.johnturkson.sandbox.ui.map.location

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.johnturkson.sandbox.theme.EarthquakeTheme
import com.johnturkson.sandbox.ui.event.EarthquakeMapItem
import com.johnturkson.sandbox.ui.layout.EarthquakeControls
import com.johnturkson.sandbox.ui.layout.EarthquakeView
import com.johnturkson.sandbox.ui.map.EarthquakeEventSnackbar
import com.johnturkson.sandbox.ui.monitoring.EarthquakeMonitoringState

class EarthquakeLocationScreenshotTest {
    @PreviewTest
    @Preview(widthDp = 384, heightDp = 640, locale = "en-US")
    @Preview(widthDp = 320, heightDp = 720, locale = "en-US", fontScale = 1.5f)
    @Preview(widthDp = 720, heightDp = 384, locale = "en-US")
    @Composable
    fun selectedLocationAndSnackbar() {
        val data =
            object : SnackbarData {
                override val visuals =
                    object : SnackbarVisuals {
                        override val message = "That earthquake is no longer available."
                        override val actionLabel: String? = null
                        override val withDismissAction = false
                        override val duration = SnackbarDuration.Short
                    }

                override fun dismiss() = Unit

                override fun performAction() = Unit
            }
        EarthquakeTheme(dynamicColor = false) {
            EarthquakeControls(
                items = listOf(EarthquakeMapItem("fixture", 37.0, -122.0, "M4.5 - California")),
                selectedItemId = "fixture",
                selectedView = EarthquakeView.Map,
                expanded = false,
                onItemClick = {},
                onClearSelection = {},
                onViewSelected = {},
                showLocationButton = true,
                snackbarHost = { EarthquakeEventSnackbar(data) },
                onMoreDetails = {},
                monitoring =
                    EarthquakeMonitoringState(status = EarthquakeMonitoringState.Status.Running),
            )
        }
    }

    @PreviewTest
    @Preview(widthDp = 384, heightDp = 640, locale = "en-US")
    @Composable
    fun compactLocationControls() {
        EarthquakeTheme(dynamicColor = false) {
            EarthquakeControls(
                items = emptyList(),
                selectedItemId = null,
                selectedView = EarthquakeView.Map,
                expanded = false,
                onItemClick = {},
                onClearSelection = {},
                onViewSelected = {},
                showLocationButton = true,
                monitoring =
                    EarthquakeMonitoringState(status = EarthquakeMonitoringState.Status.Running),
            )
        }
    }

    @PreviewTest
    @Preview(widthDp = 960, heightDp = 720, locale = "en-US")
    @Composable
    fun expandedLocationControls() {
        EarthquakeTheme(dynamicColor = false) {
            EarthquakeControls(
                items = listOf(EarthquakeMapItem("fixture", 37.0, -122.0, "M4.5 - California")),
                selectedItemId = null,
                selectedView = EarthquakeView.Map,
                expanded = true,
                onItemClick = {},
                onClearSelection = {},
                onViewSelected = {},
                showLocationButton = true,
                monitoring =
                    EarthquakeMonitoringState(status = EarthquakeMonitoringState.Status.Running),
            )
        }
    }

    @PreviewTest
    @Preview(widthDp = 960, heightDp = 720, locale = "en-US")
    @Composable
    fun minimizedLocationControls() {
        EarthquakeTheme(dynamicColor = false) {
            EarthquakeControls(
                items = listOf(EarthquakeMapItem("fixture", 37.0, -122.0, "M4.5 - California")),
                selectedItemId = "fixture",
                selectedView = EarthquakeView.Map,
                expanded = true,
                isListPanelVisible = false,
                onItemClick = {},
                onClearSelection = {},
                onViewSelected = {},
                showLocationButton = true,
                monitoring =
                    EarthquakeMonitoringState(status = EarthquakeMonitoringState.Status.Running),
            )
        }
    }

    @PreviewTest
    @Preview(widthDp = 384, heightDp = 640, locale = "en-US")
    @Preview(widthDp = 320, heightDp = 720, locale = "en-US", fontScale = 1.5f)
    @Preview(widthDp = 720, heightDp = 384, locale = "en-US")
    @Preview(
        widthDp = 384,
        heightDp = 640,
        locale = "en-US",
        uiMode = Configuration.UI_MODE_NIGHT_YES,
    )
    @Composable
    fun compactSelectedLocationControls() {
        EarthquakeTheme(dynamicColor = false) {
            EarthquakeControls(
                items = listOf(EarthquakeMapItem("fixture", 37.0, -122.0, "M4.5 - California")),
                selectedItemId = "fixture",
                selectedView = EarthquakeView.Map,
                expanded = false,
                onItemClick = {},
                onClearSelection = {},
                onViewSelected = {},
                showLocationButton = true,
                onMoreDetails = {},
                monitoring =
                    EarthquakeMonitoringState(status = EarthquakeMonitoringState.Status.Running),
            )
        }
    }

    @PreviewTest
    @Preview(locale = "en-US")
    @Preview(locale = "en-US", uiMode = Configuration.UI_MODE_NIGHT_YES)
    @Composable
    fun requestLocation() {
        EarthquakeTheme(dynamicColor = false) {
            Box(Modifier.padding(16.dp)) { EarthquakeLocationButton(onClick = {}) }
        }
    }

    @PreviewTest
    @Preview(locale = "en-US")
    @Preview(locale = "en-US", fontScale = 1.5f)
    @Composable
    fun locationSettings() {
        EarthquakeTheme(dynamicColor = false) {
            Box(Modifier.padding(16.dp)) {
                EarthquakeLocationButton(onClick = {}, requiresSettings = true)
            }
        }
    }

    @PreviewTest
    @Preview(locale = "en-US", fontScale = 1.5f)
    @Composable
    fun largeTextLocation() {
        EarthquakeTheme(dynamicColor = false) {
            Box(Modifier.padding(16.dp)) { EarthquakeLocationButton(onClick = {}) }
        }
    }

    @PreviewTest
    @Preview(locale = "en-US")
    @Composable
    fun darkLocationSettings() {
        EarthquakeTheme(darkTheme = true, dynamicColor = false) {
            Box(Modifier.padding(16.dp)) {
                EarthquakeLocationButton(onClick = {}, requiresSettings = true)
            }
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
    @Composable
    fun locationRationale() {
        EarthquakeTheme(dynamicColor = false) { EarthquakeLocationRationale({}, {}) }
    }

    @PreviewTest
    @Preview(widthDp = 320, heightDp = 720, locale = "en-US", fontScale = 1.5f)
    @Composable
    fun largeTextLocationRationale() {
        EarthquakeTheme(dynamicColor = false) { EarthquakeLocationRationale({}, {}) }
    }
}
