package com.johnturkson.sandbox.ui.layout

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
import com.johnturkson.sandbox.ui.event.EarthquakeMapItem

class EarthquakeControlsScreenshotTest {
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
    fun CompactMapControls() {
        Controls(expanded = false)
    }

    @PreviewTest
    @Preview(widthDp = 320, heightDp = 640, locale = "en-US")
    @Composable
    fun CompactNarrowMapControls() {
        Controls(expanded = false)
    }

    @PreviewTest
    @Preview(widthDp = 760, heightDp = 480, locale = "en-US")
    @Composable
    fun CompactWideMapControls() {
        Controls(expanded = false)
    }

    @PreviewTest
    @Preview(widthDp = 384, heightDp = 640, locale = "en-US")
    @Composable
    fun CompactRtlMapControls() {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Controls(expanded = false)
        }
    }

    @PreviewTest
    @Preview(widthDp = 384, heightDp = 640, locale = "en-US")
    @Composable
    fun CompactSelectedMapControls() {
        Controls(expanded = false, selectedId = item.id)
    }

    @PreviewTest
    @Preview(widthDp = 384, heightDp = 640, locale = "en-US")
    @Composable
    fun CompactSelectionWithoutTime() {
        Controls(expanded = false, selectedId = second.id)
    }

    @PreviewTest
    @Preview(widthDp = 384, heightDp = 640, locale = "en-US")
    @Composable
    fun CompactDarkMapControls() {
        Controls(expanded = false, selectedId = item.id, dark = true)
    }

    @PreviewTest
    @Preview(widthDp = 320, heightDp = 720, fontScale = 1.5f, locale = "en-US")
    @Composable
    fun CompactLargeTextMapControls() {
        Controls(expanded = false, selectedId = item.id)
    }

    @PreviewTest
    @Preview(widthDp = 960, heightDp = 720, locale = "en-US")
    @Composable
    fun ExpandedPanel() {
        Controls(expanded = true)
    }

    @PreviewTest
    @Preview(widthDp = 960, heightDp = 720, locale = "en-US")
    @Composable
    fun ExpandedEmptyPanel() {
        Controls(expanded = true, items = emptyList())
    }

    @PreviewTest
    @Preview(widthDp = 960, heightDp = 720, locale = "en-US")
    @Composable
    fun ExpandedSelectedPanel() {
        Controls(expanded = true, selectedId = item.id)
    }

    @PreviewTest
    @Preview(widthDp = 960, heightDp = 720, locale = "en-US")
    @Composable
    fun ExpandedSelectionWithoutTime() {
        Controls(expanded = true, selectedId = second.id)
    }

    @PreviewTest
    @Preview(widthDp = 960, heightDp = 720, locale = "en-US")
    @Composable
    fun ExpandedDarkPanel() {
        Controls(expanded = true, selectedId = item.id, dark = true)
    }

    @PreviewTest
    @Preview(widthDp = 840, heightDp = 600, fontScale = 1.5f, locale = "en-US")
    @Composable
    fun ExpandedLargeTextPanel() {
        Controls(expanded = true, selectedId = item.id)
    }

    @PreviewTest
    @Preview(widthDp = 960, heightDp = 720, locale = "en-US")
    @Composable
    fun MinimizedPanel() {
        Controls(expanded = true, panelVisible = false)
    }

    @PreviewTest
    @Preview(widthDp = 960, heightDp = 720, locale = "en-US")
    @Composable
    fun MinimizedSelectedPanel() {
        Controls(expanded = true, panelVisible = false, selectedId = item.id)
    }

    @PreviewTest
    @Preview(widthDp = 960, heightDp = 720, locale = "en-US")
    @Composable
    fun MinimizedSelectionWithoutTime() {
        Controls(expanded = true, panelVisible = false, selectedId = second.id)
    }

    @PreviewTest
    @Preview(widthDp = 960, heightDp = 720, locale = "en-US")
    @Composable
    fun MinimizedDarkPanel() {
        Controls(expanded = true, panelVisible = false, selectedId = item.id, dark = true)
    }

    @PreviewTest
    @Preview(widthDp = 840, heightDp = 600, fontScale = 1.5f, locale = "en-US")
    @Composable
    fun MinimizedLargeTextPanel() {
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
                EarthquakeControls(
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
