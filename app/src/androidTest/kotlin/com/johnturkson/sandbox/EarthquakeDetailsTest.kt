package com.johnturkson.sandbox

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.room.Room
import com.johnturkson.sandbox.data.EarthquakeRepository
import com.johnturkson.sandbox.data.MainDatabase
import com.johnturkson.sandbox.theme.EarthquakeTheme
import com.johnturkson.sandbox.ui.Destination
import com.johnturkson.sandbox.ui.EarthquakeDetailsCard
import com.johnturkson.sandbox.ui.EarthquakeDetailsItem
import com.johnturkson.sandbox.ui.EarthquakeDetailsScreen
import com.johnturkson.sandbox.ui.EarthquakeDetailsViewModel
import com.johnturkson.sandbox.ui.EarthquakeExplorerControls
import com.johnturkson.sandbox.ui.EarthquakeMapItem
import com.johnturkson.sandbox.ui.EarthquakeView
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class EarthquakeDetailsTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun communityReportExpansionAndListPositionRestoreTogether() {
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent {
            EarthquakeTheme {
                EarthquakeDetailsCard(
                    EarthquakeDetailsItem(
                        mapItem =
                            EarthquakeMapItem(
                                "report-event",
                                37.76,
                                -122.45,
                                "Earthquake",
                                magnitude = 4.2,
                            ),
                        depth = 8.4,
                        updated = Instant.parse("2026-11-01T18:24:00Z"),
                        place = "Near San Francisco, California",
                        felt = 128,
                        communityIntensity = 4.0,
                    ),
                    "Nov 1, 2026 · 10:24 AM",
                    {},
                )
            }
        }
        composeRule.onNodeWithText("Did You Feel It?").performScrollTo().performClick()
        composeRule.onNodeWithText("Community intensity").performScrollTo().assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        composeRule.onNodeWithText("Community intensity").assertIsDisplayed()
        composeRule.onNodeWithText("4.0").assertIsDisplayed()
    }

    @Test
    fun narrowExpandedDetailsKeepTheLocationActionVisibleBesideTheLeftPane() {
        var located = false
        composeRule.setContent {
            val width = LocalWindowInfo.current.containerSize.width.coerceAtLeast(1).toFloat()
            CompositionLocalProvider(LocalDensity provides Density(width / 840f)) {
                EarthquakeTheme {
                    EarthquakeExplorerControls(
                        items = listOf(EarthquakeMapItem("event", 37.0, -122.0, "An earthquake")),
                        selectedItemId = "event",
                        selectedView = EarthquakeView.Map,
                        expanded = true,
                        onItemClick = {},
                        onClearSelection = {},
                        onViewSelected = {},
                        detailsVisible = true,
                        showLocationButton = true,
                        onLocationClick = { located = true },
                    )
                }
            }
        }
        composeRule
            .onNodeWithContentDescription("Use my location")
            .assertIsDisplayed()
            .performClick()
        assertEquals(true, located)
    }

    @Test
    fun detailDestinationObservesSavedAliasesAndNewSourceRevisions() = runBlocking {
        val database =
            Room.inMemoryDatabaseBuilder(composeRule.activity, MainDatabase::class.java).build()
        val record =
            EarthquakeMonitoringFixtures.item(magnitude = 4.2)
                .copy(
                    ids = ",retained-alias,",
                    depth = 8.4,
                    magnitudeType = "mw",
                    felt = 128,
                    cdi = 4.0,
                    mmi = 3.8,
                )
        database.earthquakeDao().mergeEarthquakes(listOf(record))
        lateinit var model: EarthquakeDetailsViewModel
        try {
            composeRule.runOnUiThread {
                model =
                    EarthquakeDetailsViewModel(
                        Destination.EarthquakeDetailsRoute("retained-alias"),
                        EarthquakeRepository(database.earthquakeDao(), database.monitoringDao()),
                    )
                composeRule.activity.viewModelStore.put("details", model)
            }
            composeRule.waitUntil { !model.isLoading }
            assertEquals(record.id, model.item?.mapItem?.id)
            assertEquals(8.4, model.item?.depth)
            assertEquals(128, model.item?.felt)
            assertEquals(4.0, model.item?.communityIntensity)
            database
                .earthquakeDao()
                .mergeEarthquakes(
                    listOf(record.copy(depth = 9.1, updated = record.updated + 1.seconds))
                )
            composeRule.waitUntil { model.item?.depth == 9.1 }
            assertEquals(record.updated + 1.seconds, model.item?.updated)
        } finally {
            composeRule.runOnUiThread { composeRule.activity.viewModelStore.clear() }
            database.close()
        }
    }

    @Test
    fun unavailableDetailsCanCloseAndBecomeAvailableWhenRoomReceivesTheRecord(): Unit =
        runBlocking {
            val database =
                Room.inMemoryDatabaseBuilder(composeRule.activity, MainDatabase::class.java).build()
            val record = EarthquakeMonitoringFixtures.item().copy(id = "late-event", felt = 0)
            lateinit var model: EarthquakeDetailsViewModel
            var closed = false
            try {
                composeRule.runOnUiThread {
                    model =
                        EarthquakeDetailsViewModel(
                            Destination.EarthquakeDetailsRoute(record.id),
                            EarthquakeRepository(
                                database.earthquakeDao(),
                                database.monitoringDao(),
                            ),
                        )
                    composeRule.activity.viewModelStore.put("details", model)
                }
                composeRule.setContent {
                    EarthquakeTheme { EarthquakeDetailsScreen(model, onClose = { closed = true }) }
                }
                composeRule.waitUntil { !model.isLoading }
                assertNull(model.item)
                composeRule
                    .onNodeWithText(composeRule.activity.getString(R.string.event_unavailable))
                    .assertIsDisplayed()
                composeRule.onNodeWithContentDescription("Close earthquake details").performClick()
                assertEquals(true, closed)
                database.earthquakeDao().mergeEarthquakes(listOf(record))
                composeRule.waitUntil { model.item != null }
                assertFalse(model.isLoading)
                composeRule.onNodeWithText("Event information").assertIsDisplayed()
                composeRule.onNodeWithText("0").performScrollTo().assertIsDisplayed()
            } finally {
                composeRule.runOnUiThread { composeRule.activity.viewModelStore.clear() }
                database.close()
            }
        }
}
