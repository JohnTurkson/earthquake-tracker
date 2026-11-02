package com.johnturkson.sandbox.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass.Companion.HEIGHT_DP_MEDIUM_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import com.johnturkson.sandbox.theme.EarthquakeTheme
import com.johnturkson.sandbox.ui.EarthquakeDetailsCard
import com.johnturkson.sandbox.ui.EarthquakeDetailsItem
import com.johnturkson.sandbox.ui.EarthquakeMapItem
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
class EarthquakeReportPrototypeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EarthquakeTheme(dynamicColor = false) {
                val uriHandler = LocalUriHandler.current
                Box(
                    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    val expanded =
                        currentWindowAdaptiveInfoV2()
                            .windowSizeClass
                            .isAtLeastBreakpoint(
                                WIDTH_DP_EXPANDED_LOWER_BOUND,
                                HEIGHT_DP_MEDIUM_LOWER_BOUND,
                            )
                    Box(
                        Modifier.align(Alignment.CenterStart)
                            .then(if (expanded) Modifier.width(392.dp) else Modifier.fillMaxSize())
                            .fillMaxSize()
                            .padding(WindowInsets.safeDrawing.asPaddingValues())
                            .padding(16.dp)
                    ) {
                        EarthquakeDetailsCard(
                            item =
                                EarthquakeDetailsItem(
                                    mapItem =
                                        EarthquakeMapItem(
                                            id = "ci38457511",
                                            latitude = 35.7695,
                                            longitude = -117.5993333,
                                            title = "M7.1 - Ridgecrest Earthquake Sequence",
                                            magnitude = 7.1,
                                            eventTime = Instant.parse("2019-07-06T03:19:53.040Z"),
                                        ),
                                    place = "Ridgecrest Earthquake Sequence",
                                    depth = 8.0,
                                    updated = Instant.fromEpochMilliseconds(1792369421520),
                                    magnitudeType = "mw",
                                    status = "reviewed",
                                    felt = 39888,
                                    communityIntensity = 8.7,
                                    network = "ci",
                                    url =
                                        "https://earthquake.usgs.gov/earthquakes/eventpage/ci38457511",
                                    shakingMapUrl =
                                        "https://earthquake.usgs.gov/product/shakemap/ci38457511/atlas/1594160054783/download/intensity.jpg",
                                    feltMapUrl =
                                        "https://earthquake.usgs.gov/product/dyfi/ci38457511/us/1785244957753/ci38457511_ciim.jpg",
                                ),
                            updatedTime = "Oct 18, 2026 · 5:23 PM PDT",
                            onClose = ::finish,
                            modifier = Modifier.fillMaxSize(),
                            onOpenSource = uriHandler::openUri,
                            initialShakingExpanded = true,
                        )
                    }
                }
            }
        }
    }
}
