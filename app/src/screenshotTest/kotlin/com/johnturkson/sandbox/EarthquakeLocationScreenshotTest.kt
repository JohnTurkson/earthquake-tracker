package com.johnturkson.sandbox

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.johnturkson.sandbox.theme.EarthquakeTheme
import com.johnturkson.sandbox.ui.EarthquakeExplorerControls
import com.johnturkson.sandbox.ui.EarthquakeLocationButton
import com.johnturkson.sandbox.ui.EarthquakeLocationRationale
import com.johnturkson.sandbox.ui.EarthquakeMapItem
import com.johnturkson.sandbox.ui.EarthquakeView

class EarthquakeLocationScreenshotTest {
    @PreviewTest
    @Preview(widthDp = 384, heightDp = 640, locale = "en-US")
    @Composable
    fun compactLocationControls() {
        EarthquakeTheme(dynamicColor = false) {
            EarthquakeExplorerControls(
                items = emptyList(),
                selectedItemId = null,
                selectedView = EarthquakeView.Map,
                expanded = false,
                onItemClick = {},
                onClearSelection = {},
                onViewSelected = {},
                showLocationButton = true,
            )
        }
    }

    @PreviewTest
    @Preview(widthDp = 960, heightDp = 720, locale = "en-US")
    @Composable
    fun expandedLocationControls() {
        EarthquakeTheme(dynamicColor = false) {
            EarthquakeExplorerControls(
                items = listOf(EarthquakeMapItem("fixture", 37.0, -122.0, "M 4.5 - California")),
                selectedItemId = null,
                selectedView = EarthquakeView.Map,
                expanded = true,
                onItemClick = {},
                onClearSelection = {},
                onViewSelected = {},
                showLocationButton = true,
            )
        }
    }

    @PreviewTest
    @Preview(locale = "en-US")
    @Composable
    fun requestLocation() {
        EarthquakeTheme(dynamicColor = false) {
            Box(Modifier.padding(16.dp)) { EarthquakeLocationButton(onClick = {}) }
        }
    }

    @PreviewTest
    @Preview(locale = "en-US")
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
