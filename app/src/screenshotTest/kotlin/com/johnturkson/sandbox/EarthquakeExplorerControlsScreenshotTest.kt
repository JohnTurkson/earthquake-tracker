package com.johnturkson.sandbox

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import com.android.tools.screenshot.PreviewTest
import com.johnturkson.sandbox.theme.EarthquakeTheme
import com.johnturkson.sandbox.ui.EarthquakeExplorerControls
import com.johnturkson.sandbox.ui.EarthquakeMapItem
import com.johnturkson.sandbox.ui.EarthquakeView

class EarthquakeExplorerControlsScreenshotTest {
    private val item =
        EarthquakeMapItem(
            id = "shima",
            latitude = 33.822,
            longitude = 136.9854,
            title = "M6.0 - 58 km SSE of Shima, Japan",
            snippet = "Oct 31, 2026 · 10:15 PM",
        )
    private val second =
        EarthquakeMapItem(
            id = "alaska",
            latitude = 51.6994,
            longitude = -172.7931,
            title = "M4.5 - 111 km ESE of Atka, Alaska",
        )

    @PreviewTest
    @Preview(widthDp = 384, heightDp = 640, locale = "en-US")
    @Composable
    fun compactMapControls() {
        Controls(expanded = false)
    }

    @PreviewTest
    @Preview(widthDp = 320, heightDp = 640, locale = "en-US")
    @Composable
    fun compactNarrowMapControls() {
        Controls(expanded = false)
    }

    @PreviewTest
    @Preview(widthDp = 760, heightDp = 480, locale = "en-US")
    @Composable
    fun compactWideMapControls() {
        Controls(expanded = false)
    }

    @PreviewTest
    @Preview(widthDp = 384, heightDp = 640, locale = "en-US")
    @Composable
    fun compactRtlMapControls() {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Controls(expanded = false)
        }
    }

    @PreviewTest
    @Preview(widthDp = 384, heightDp = 640, locale = "en-US")
    @Composable
    fun compactSelectedMapControls() {
        Controls(expanded = false, selectedId = item.id)
    }

    @PreviewTest
    @Preview(widthDp = 384, heightDp = 640, locale = "en-US")
    @Composable
    fun compactSelectionWithoutTime() {
        Controls(expanded = false, selectedId = second.id)
    }

    @PreviewTest
    @Preview(widthDp = 384, heightDp = 640, locale = "en-US")
    @Composable
    fun compactDarkMapControls() {
        Controls(expanded = false, selectedId = item.id, dark = true)
    }

    @PreviewTest
    @Preview(widthDp = 320, heightDp = 720, fontScale = 1.5f, locale = "en-US")
    @Composable
    fun compactLargeTextMapControls() {
        Controls(expanded = false, selectedId = item.id)
    }

    @PreviewTest
    @Preview(widthDp = 960, heightDp = 720, locale = "en-US")
    @Composable
    fun expandedPanel() {
        Controls(expanded = true)
    }

    @PreviewTest
    @Preview(widthDp = 960, heightDp = 720, locale = "en-US")
    @Composable
    fun expandedEmptyPanel() {
        Controls(expanded = true, items = emptyList())
    }

    @PreviewTest
    @Preview(widthDp = 960, heightDp = 720, locale = "en-US")
    @Composable
    fun expandedSelectedPanel() {
        Controls(expanded = true, selectedId = item.id)
    }

    @PreviewTest
    @Preview(widthDp = 960, heightDp = 720, locale = "en-US")
    @Composable
    fun expandedSelectionWithoutTime() {
        Controls(expanded = true, selectedId = second.id)
    }

    @PreviewTest
    @Preview(widthDp = 960, heightDp = 720, locale = "en-US")
    @Composable
    fun expandedDarkPanel() {
        Controls(expanded = true, selectedId = item.id, dark = true)
    }

    @PreviewTest
    @Preview(widthDp = 840, heightDp = 600, fontScale = 1.5f, locale = "en-US")
    @Composable
    fun expandedLargeTextPanel() {
        Controls(expanded = true, selectedId = item.id)
    }

    @PreviewTest
    @Preview(widthDp = 960, heightDp = 720, locale = "en-US")
    @Composable
    fun minimizedPanel() {
        Controls(expanded = true, panelVisible = false)
    }

    @PreviewTest
    @Preview(widthDp = 960, heightDp = 720, locale = "en-US")
    @Composable
    fun minimizedSelectedPanel() {
        Controls(expanded = true, panelVisible = false, selectedId = item.id)
    }

    @PreviewTest
    @Preview(widthDp = 960, heightDp = 720, locale = "en-US")
    @Composable
    fun minimizedSelectionWithoutTime() {
        Controls(expanded = true, panelVisible = false, selectedId = second.id)
    }

    @PreviewTest
    @Preview(widthDp = 960, heightDp = 720, locale = "en-US")
    @Composable
    fun minimizedDarkPanel() {
        Controls(expanded = true, panelVisible = false, selectedId = item.id, dark = true)
    }

    @PreviewTest
    @Preview(widthDp = 840, heightDp = 600, fontScale = 1.5f, locale = "en-US")
    @Composable
    fun minimizedLargeTextPanel() {
        Controls(expanded = true, panelVisible = false, selectedId = item.id)
    }

    @Composable
    private fun Controls(
        expanded: Boolean,
        selectedId: String? = null,
        items: List<EarthquakeMapItem> = listOf(item, second),
        dark: Boolean = false,
        panelVisible: Boolean = true,
    ) {
        EarthquakeTheme(darkTheme = dark, dynamicColor = false) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainer)) {
                EarthquakeExplorerControls(
                    items = items,
                    selectedItemId = selectedId,
                    selectedView = EarthquakeView.Map,
                    expanded = expanded,
                    isListPanelVisible = panelVisible,
                    onItemClick = {},
                    onClearSelection = {},
                    onViewSelected = {},
                )
            }
        }
    }
}
