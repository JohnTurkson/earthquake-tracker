package com.johnturkson.sandbox

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.johnturkson.sandbox.theme.EarthquakeTheme
import com.johnturkson.sandbox.ui.EarthquakeListPreviewState
import com.johnturkson.sandbox.ui.EarthquakeListRow
import com.johnturkson.sandbox.ui.EarthquakeListScreen
import com.johnturkson.sandbox.ui.EarthquakeMapItem
import com.johnturkson.sandbox.ui.EarthquakeSelectionCard
import com.johnturkson.sandbox.ui.EarthquakeView
import com.johnturkson.sandbox.ui.EarthquakeViewSwitcher

class EarthquakeViewsScreenshotTest {
    private val item =
        EarthquakeMapItem(
            id = "shima",
            latitude = 33.822,
            longitude = 136.9854,
            title = "M 6.0 - 58 km SSE of Shima, Japan",
            snippet = "Oct 31, 2026 · 10:15 PM",
        )
    private val second =
        EarthquakeMapItem(
            id = "alaska",
            latitude = 51.6994,
            longitude = -172.7931,
            title = "M 4.5 - 111 km ESE of Atka, Alaska",
        )

    @PreviewTest
    @Preview(widthDp = 384, heightDp = 640, locale = "en-US")
    @Composable
    fun compactList() {
        ListScreen()
    }

    @PreviewTest
    @Preview(widthDp = 384, heightDp = 640, locale = "en-US")
    @Composable
    fun selectedList() {
        ListScreen(selectedId = item.id)
    }

    @PreviewTest
    @Preview(widthDp = 384, heightDp = 640, locale = "en-US")
    @Composable
    fun emptyListView() {
        ListScreen(items = emptyList())
    }

    @PreviewTest
    @Preview(widthDp = 384, heightDp = 640, locale = "en-US")
    @Composable
    fun darkList() {
        ListScreen(dark = true)
    }

    @PreviewTest
    @Preview(widthDp = 320, heightDp = 720, fontScale = 1.5f, locale = "en-US")
    @Composable
    fun largeTextList() {
        ListScreen(selectedId = item.id)
    }

    @PreviewTest
    @Preview(locale = "en-US")
    @Preview(locale = "en-US", uiMode = Configuration.UI_MODE_NIGHT_YES)
    @Preview(locale = "en-US", fontScale = 1.5f)
    @Composable
    fun selectedMapSwitcher() {
        EarthquakeTheme(dynamicColor = false) {
            Box(Modifier.padding(12.dp)) {
                EarthquakeViewSwitcher(EarthquakeView.Map, {}, Modifier.width(280.dp))
            }
        }
    }

    @PreviewTest
    @Preview(locale = "en-US")
    @Preview(locale = "en-US", uiMode = Configuration.UI_MODE_NIGHT_YES)
    @Preview(locale = "en-US", fontScale = 1.5f)
    @Composable
    fun selectedListSwitcher() {
        EarthquakeTheme(dynamicColor = false) {
            Box(Modifier.padding(12.dp)) {
                EarthquakeViewSwitcher(EarthquakeView.List, {}, Modifier.width(280.dp))
            }
        }
    }

    @PreviewTest
    @Preview(locale = "en-US")
    @Composable
    fun iconOnlyMapSwitcher() {
        CompactSwitcher(EarthquakeView.Map)
    }

    @PreviewTest
    @Preview(locale = "en-US")
    @Composable
    fun iconOnlyListSwitcher() {
        CompactSwitcher(EarthquakeView.List)
    }

    @PreviewTest
    @Preview(locale = "en-US")
    @Composable
    fun darkIconOnlySwitcher() {
        CompactSwitcher(EarthquakeView.List, dark = true)
    }

    @PreviewTest
    @Preview(locale = "en-US", fontScale = 1.5f)
    @Composable
    fun largeTextIconOnlySwitcher() {
        CompactSwitcher(EarthquakeView.Map)
    }

    @PreviewTest
    @Preview(locale = "en-US")
    @Composable
    fun darkIconOnlyMapSwitcher() {
        CompactSwitcher(EarthquakeView.Map, dark = true)
    }

    @PreviewTest
    @Preview(locale = "en-US", fontScale = 1.5f)
    @Composable
    fun largeTextIconOnlyListSwitcher() {
        CompactSwitcher(EarthquakeView.List)
    }

    @Composable
    private fun CompactSwitcher(selectedView: EarthquakeView, dark: Boolean = false) {
        EarthquakeTheme(darkTheme = dark, dynamicColor = false) {
            Box(Modifier.padding(12.dp)) {
                EarthquakeViewSwitcher(selectedView, {}, Modifier.width(160.dp))
            }
        }
    }

    @PreviewTest
    @Preview(locale = "en-US")
    @Composable
    fun selectedEarthquake() {
        EarthquakeTheme(dynamicColor = false) {
            Box(Modifier.padding(12.dp)) {
                EarthquakeSelectionCard(item, {}, Modifier.width(360.dp))
            }
        }
    }

    @PreviewTest
    @Preview(locale = "en-US", fontScale = 1.5f)
    @Composable
    fun largeTextSelectedEarthquake() {
        EarthquakeTheme(dynamicColor = false) {
            Box(Modifier.padding(12.dp)) {
                EarthquakeSelectionCard(item, {}, Modifier.width(296.dp))
            }
        }
    }

    @PreviewTest
    @Preview(locale = "en-US")
    @Composable
    fun earthquakeWithoutTime() {
        EarthquakeTheme(dynamicColor = false) {
            Surface(Modifier.width(384.dp)) {
                EarthquakeListRow(second, isSelected = false, onClick = {})
            }
        }
    }

    @PreviewTest
    @Preview(locale = "en-US")
    @Preview(locale = "en-US", uiMode = Configuration.UI_MODE_NIGHT_YES)
    @Preview(locale = "en-US", fontScale = 1.5f)
    @Composable
    fun selectedEarthquakeWithoutTime() {
        EarthquakeTheme(dynamicColor = false) {
            Box(Modifier.padding(12.dp)) {
                EarthquakeSelectionCard(second, {}, Modifier.width(360.dp))
            }
        }
    }

    @Composable
    private fun ListScreen(
        items: List<EarthquakeMapItem> = listOf(item, second),
        selectedId: String? = null,
        dark: Boolean = false,
    ) {
        EarthquakeTheme(darkTheme = dark, dynamicColor = false) {
            EarthquakeListScreen(
                state =
                    EarthquakeListPreviewState(
                        mapItems = items,
                        selectedMapItemId = selectedId,
                    ),
                onItemClick = {},
            )
        }
    }
}
