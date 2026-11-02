package com.johnturkson.sandbox

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.johnturkson.sandbox.theme.EarthquakeTheme
import com.johnturkson.sandbox.ui.EarthquakeDetailsCard
import com.johnturkson.sandbox.ui.EarthquakeDetailsItem
import com.johnturkson.sandbox.ui.EarthquakeDetailsScreen
import com.johnturkson.sandbox.ui.EarthquakeDetailsState
import com.johnturkson.sandbox.ui.EarthquakeExplorerControls
import com.johnturkson.sandbox.ui.EarthquakeListRow
import com.johnturkson.sandbox.ui.EarthquakeMapItem
import com.johnturkson.sandbox.ui.EarthquakeMonitoringState
import com.johnturkson.sandbox.ui.EarthquakeSelectionCard
import com.johnturkson.sandbox.ui.EarthquakeView
import kotlin.time.Instant

class EarthquakeDetailsScreenshotTest {
    private val mapItem =
        EarthquakeMapItem(
            id = "us-example",
            latitude = 37.76,
            longitude = -122.45,
            title = "M4.2 - Near San Francisco, CA",
            snippet = "Nov 1, 2026 · 10:15 AM",
            magnitude = 4.2,
        )
    private val item =
        EarthquakeDetailsItem(
            mapItem = mapItem,
            depth = 8.4,
            updated = Instant.parse("2026-11-01T18:24:00Z"),
            place = "Near San Francisco, California",
            magnitudeType = "mw",
            status = "reviewed",
            felt = 128,
            communityIntensity = 4.0,
            network = "nc",
            url = "https://earthquake.usgs.gov/earthquakes/eventpage/us-example",
        )

    @PreviewTest
    @Preview(widthDp = 384, heightDp = 720, locale = "en-US")
    @Preview(
        widthDp = 384,
        heightDp = 720,
        locale = "en-US",
        uiMode = Configuration.UI_MODE_NIGHT_YES,
    )
    @Composable
    fun compactDetails() {
        Theme { Card() }
    }

    @PreviewTest
    @Preview(widthDp = 320, heightDp = 720, fontScale = 1.5f, locale = "en-US")
    @Composable
    fun narrowLargeTextDetails() {
        Theme { Card() }
    }

    @PreviewTest
    @Preview(widthDp = 384, heightDp = 720, locale = "en-US")
    @Preview(
        widthDp = 384,
        heightDp = 720,
        locale = "en-US",
        uiMode = Configuration.UI_MODE_NIGHT_YES,
    )
    @Preview(widthDp = 320, heightDp = 720, fontScale = 1.5f, locale = "en-US")
    @Composable
    fun missingMeasurements() {
        Theme {
            Card(
                item =
                    item.copy(
                        mapItem =
                            mapItem.copy(
                                snippet = null,
                                magnitude = null,
                                title = null,
                            ),
                        place = null,
                        depth = -0.4,
                        magnitudeType = null,
                        status = null,
                        felt = null,
                        communityIntensity = null,
                        network = null,
                        url = null,
                    )
            )
        }
    }

    @PreviewTest
    @Preview(widthDp = 384, heightDp = 720, locale = "en-US")
    @Composable
    fun zeroMeasurements() {
        Theme {
            Card(
                item =
                    item.copy(
                        depth = 0.0,
                        felt = 0,
                        communityIntensity = 0.0,
                        status = "automatic",
                    )
            )
        }
    }

    @PreviewTest
    @Preview(widthDp = 384, heightDp = 720, locale = "en-US")
    @Preview(widthDp = 320, heightDp = 720, fontScale = 1.5f, locale = "en-US")
    @Composable
    fun expandedCommunityReports() {
        Theme {
            EarthquakeDetailsCard(
                item,
                "Nov 1, 2026 · 10:24 AM",
                {},
                Modifier.fillMaxSize().padding(16.dp),
                listState = rememberLazyListState(initialFirstVisibleItemIndex = 9),
                initialFeltExpanded = true,
            )
        }
    }

    @PreviewTest
    @Preview(widthDp = 384, heightDp = 480, locale = "en-US")
    @Preview(widthDp = 320, heightDp = 640, fontScale = 1.5f, locale = "en-US")
    @Composable
    fun scrolledSourceInformation() {
        Theme { Card(scrolled = true) }
    }

    @PreviewTest
    @Preview(widthDp = 384, heightDp = 720, locale = "en-US")
    @Composable
    fun rightToLeftDetails() {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Theme { Card() }
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
    @Preview(widthDp = 392, heightDp = 600, fontScale = 1.5f, locale = "en-US")
    @Composable
    fun loadingDetails() {
        Theme { EarthquakeDetailsScreen(PreviewState(true), {}) }
    }

    @PreviewTest
    @Preview(widthDp = 384, heightDp = 640, locale = "en-US")
    @Preview(
        widthDp = 384,
        heightDp = 640,
        locale = "en-US",
        uiMode = Configuration.UI_MODE_NIGHT_YES,
    )
    @Preview(widthDp = 392, heightDp = 600, fontScale = 1.5f, locale = "en-US")
    @Composable
    fun unavailableDetails() {
        Theme { EarthquakeDetailsScreen(PreviewState(false), {}) }
    }

    @PreviewTest
    @Preview(widthDp = 392, heightDp = 720, locale = "en-US")
    @Composable
    fun expandedLoadingDetails() {
        Theme { EarthquakeDetailsScreen(PreviewState(true), {}, expanded = true) }
    }

    @PreviewTest
    @Preview(widthDp = 392, heightDp = 720, locale = "en-US")
    @Composable
    fun expandedUnavailableDetails() {
        Theme { EarthquakeDetailsScreen(PreviewState(false), {}, expanded = true) }
    }

    @PreviewTest
    @Preview(widthDp = 384, heightDp = 360, locale = "en-US")
    @Preview(
        widthDp = 384,
        heightDp = 360,
        locale = "en-US",
        uiMode = Configuration.UI_MODE_NIGHT_YES,
    )
    @Preview(widthDp = 320, heightDp = 480, fontScale = 1.5f, locale = "en-US")
    @Composable
    fun selectionCardEntryPoint() {
        Theme {
            Box(Modifier.fillMaxSize()) {
                EarthquakeSelectionCard(mapItem, {}, Modifier.padding(16.dp), onMoreDetails = {})
            }
        }
    }

    @PreviewTest
    @Preview(widthDp = 384, heightDp = 360, locale = "en-US")
    @Composable
    fun selectionCardWithoutTime() {
        Theme {
            Box(Modifier.fillMaxSize()) {
                EarthquakeSelectionCard(
                    mapItem.copy(snippet = null),
                    {},
                    Modifier.padding(16.dp),
                    onMoreDetails = {},
                )
            }
        }
    }

    @PreviewTest
    @Preview(widthDp = 384, locale = "en-US")
    @Preview(widthDp = 320, fontScale = 1.5f, locale = "en-US")
    @Composable
    fun listRowEntryPoint() {
        Theme { EarthquakeListRow(mapItem, false, {}, onMoreDetails = {}) }
    }

    @PreviewTest
    @Preview(widthDp = 384, locale = "en-US", uiMode = Configuration.UI_MODE_NIGHT_YES)
    @Composable
    fun selectedListRowWithoutTime() {
        Theme {
            EarthquakeListRow(
                mapItem.copy(snippet = null),
                true,
                {},
                showMapAction = false,
                onMoreDetails = {},
            )
        }
    }

    @PreviewTest
    @Preview(widthDp = 1100, heightDp = 720, locale = "en-US")
    @Preview(widthDp = 840, heightDp = 600, locale = "en-US")
    @Preview(
        widthDp = 1100,
        heightDp = 720,
        locale = "en-US",
        uiMode = Configuration.UI_MODE_NIGHT_YES,
    )
    @Composable
    fun expandedDetailsInLeftPane() {
        Expanded()
    }

    @PreviewTest
    @Preview(widthDp = 840, heightDp = 600, fontScale = 1.5f, locale = "en-US")
    @Composable
    fun expandedDetailsWithLargeText() {
        Expanded()
    }

    @PreviewTest
    @Preview(widthDp = 1100, heightDp = 720, locale = "en-US")
    @Composable
    fun minimizedMapEntryPoint() {
        Theme {
            EarthquakeExplorerControls(
                items = listOf(mapItem),
                selectedItemId = mapItem.id,
                selectedView = EarthquakeView.Map,
                expanded = true,
                onItemClick = {},
                onClearSelection = {},
                onViewSelected = {},
                isListPanelVisible = false,
                onMoreDetails = {},
                showLocationButton = true,
            )
        }
    }

    @PreviewTest
    @Preview(widthDp = 384, heightDp = 720, locale = "en-US")
    @Preview(widthDp = 320, heightDp = 720, fontScale = 1.5f, locale = "en-US")
    @Preview(
        widthDp = 384,
        heightDp = 720,
        locale = "en-US",
        uiMode = Configuration.UI_MODE_NIGHT_YES,
    )
    @Composable
    fun compactMapEntryPoint() {
        Theme {
            EarthquakeExplorerControls(
                items = listOf(mapItem),
                selectedItemId = mapItem.id,
                selectedView = EarthquakeView.Map,
                expanded = false,
                onItemClick = {},
                onClearSelection = {},
                onViewSelected = {},
                onMoreDetails = {},
                showLocationButton = true,
                monitoring = EarthquakeMonitoringState(),
            )
        }
    }

    @Composable
    private fun Expanded() {
        Theme {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerLow)) {
                EarthquakeExplorerControls(
                    items = listOf(mapItem),
                    selectedItemId = mapItem.id,
                    selectedView = EarthquakeView.Map,
                    expanded = true,
                    onItemClick = {},
                    onClearSelection = {},
                    onViewSelected = {},
                    isListPanelVisible = false,
                    renderListContent = false,
                    detailsVisible = true,
                    showLocationButton = true,
                    monitoring = EarthquakeMonitoringState(),
                )
                Box(Modifier.align(Alignment.CenterStart).width(392.dp).fillMaxSize()) {
                    Card(top = 80)
                }
            }
        }
    }

    @Composable
    private fun Card(
        item: EarthquakeDetailsItem = this.item,
        scrolled: Boolean = false,
        top: Int = 12,
    ) {
        EarthquakeDetailsCard(
            item,
            "Nov 1, 2026 · 10:24 AM",
            {},
            Modifier.fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, top = top.dp, bottom = 16.dp),
            listState =
                rememberLazyListState(initialFirstVisibleItemIndex = if (scrolled) 9 else 0),
        )
    }

    @Composable
    private fun Theme(content: @Composable () -> Unit) {
        EarthquakeTheme(dynamicColor = false) { content() }
    }

    private data class PreviewState(
        override val isLoading: Boolean,
        override val item: EarthquakeDetailsItem? = null,
    ) : EarthquakeDetailsState
}
