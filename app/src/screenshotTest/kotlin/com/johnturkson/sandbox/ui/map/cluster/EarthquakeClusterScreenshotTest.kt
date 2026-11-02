package com.johnturkson.sandbox.ui.map.cluster

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.johnturkson.sandbox.theme.EarthquakeTheme
import com.johnturkson.sandbox.ui.event.EarthquakeMapItem
import com.johnturkson.sandbox.ui.map.EarthquakeMapFixtures

class EarthquakeClusterScreenshotTest {
    @PreviewTest
    @Preview(locale = "en-US")
    @Preview(locale = "en-US", uiMode = Configuration.UI_MODE_NIGHT_YES)
    @Preview(locale = "en-US", fontScale = 1.5f)
    @Composable
    fun pair() {
        Badge(2, null)
    }

    @PreviewTest
    @Preview(locale = "en-US")
    @Composable
    fun countAndMagnitude() {
        Badge(30, 4.8)
    }

    @PreviewTest
    @Preview(locale = "en-US", fontScale = 1.5f)
    @Composable
    fun largeCountAndText() {
        Badge(1200, 6.2)
    }

    @PreviewTest
    @Preview(locale = "en-US")
    @Composable
    fun darkBadge() {
        Badge(8, 0.0, dark = true)
    }

    @PreviewTest
    @Preview(widthDp = 384, locale = "en-US")
    @Composable
    fun populatedSheet() {
        Sheet()
    }

    @PreviewTest
    @Preview(widthDp = 384, locale = "en-US")
    @Composable
    fun selectedSheet() {
        Sheet(selected = "synthetic-0")
    }

    @PreviewTest
    @Preview(widthDp = 384, locale = "en-US")
    @Composable
    fun emptySheet() {
        Sheet(items = emptyList())
    }

    @PreviewTest
    @Preview(widthDp = 384, locale = "en-US")
    @Composable
    fun singleUnknownMagnitude() {
        Sheet(
            items =
                listOf(
                    EarthquakeMapItem("unknown", 0.0, 0.0, "Earthquake with no reported magnitude")
                )
        )
    }

    @PreviewTest
    @Preview(widthDp = 384, locale = "en-US")
    @Composable
    fun darkSheet() {
        Sheet(dark = true, selected = "synthetic-0")
    }

    @PreviewTest
    @Preview(widthDp = 320, locale = "en-US", fontScale = 1.5f)
    @Composable
    fun largeTextSheet() {
        Sheet(selected = "synthetic-0", maxHeight = 640)
    }

    @PreviewTest
    @Preview(widthDp = 320, locale = "en-US", fontScale = 1.5f)
    @Composable
    fun shortLargeTextSheet() {
        Sheet(maxHeight = 240)
    }

    @PreviewTest
    @Preview(widthDp = 400, locale = "en-US")
    @Composable
    fun compactHeightSheet() {
        Sheet(maxHeight = 216)
    }

    @PreviewTest
    @Preview(widthDp = 400, locale = "en-US")
    @Composable
    fun mediumHeightSheet() {
        Sheet(maxHeight = 288)
    }

    @PreviewTest
    @Preview(widthDp = 384, locale = "en-US")
    @Preview(widthDp = 384, locale = "en-US", uiMode = Configuration.UI_MODE_NIGHT_YES)
    @Preview(widthDp = 320, locale = "en-US", fontScale = 1.5f)
    @Composable
    fun selectedUnknownMagnitude() {
        Sheet(
            items =
                listOf(
                    EarthquakeMapItem("unknown", 0.0, 0.0, "Earthquake with no reported magnitude")
                ),
            selected = "unknown",
        )
    }

    @PreviewTest
    @Preview(widthDp = 384, locale = "en-US")
    @Preview(widthDp = 384, locale = "en-US", uiMode = Configuration.UI_MODE_NIGHT_YES)
    @Preview(widthDp = 320, locale = "en-US", fontScale = 1.5f)
    @Composable
    fun scrolledSheet() {
        Sheet(selected = EarthquakeMapFixtures.denseItems.last().id, firstVisibleItemIndex = 120)
    }

    @Composable
    private fun Badge(count: Int, magnitude: Double?, dark: Boolean = isSystemInDarkTheme()) {
        EarthquakeTheme(darkTheme = dark, dynamicColor = false) {
            Surface { Box(Modifier.padding(12.dp)) { EarthquakeClusterMarker(count, magnitude) } }
        }
    }

    @Composable
    private fun Sheet(
        items: List<EarthquakeMapItem> = EarthquakeMapFixtures.denseItems,
        selected: String? = null,
        dark: Boolean = isSystemInDarkTheme(),
        maxHeight: Int = 480,
        firstVisibleItemIndex: Int = 0,
    ) {
        EarthquakeTheme(darkTheme = dark, dynamicColor = false) {
            Surface {
                EarthquakeClusterSheetContent(
                    items,
                    selected,
                    {},
                    {},
                    Modifier.heightIn(max = maxHeight.dp),
                    listState = rememberLazyListState(firstVisibleItemIndex),
                )
            }
        }
    }
}
