package com.johnturkson.sandbox

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.work.WorkManager
import com.google.android.gms.location.LocationServices
import com.johnturkson.sandbox.data.EarthquakeConnection
import com.johnturkson.sandbox.data.EarthquakeLocationProvider
import com.johnturkson.sandbox.data.EarthquakeLocationRepository
import com.johnturkson.sandbox.data.EarthquakeRepository
import com.johnturkson.sandbox.data.MainDatabase
import com.johnturkson.sandbox.ui.Destination
import com.johnturkson.sandbox.ui.EarthquakeListViewModel
import com.johnturkson.sandbox.ui.EarthquakeMonitoringObserver
import com.johnturkson.sandbox.ui.EarthquakeSelectionResult
import com.johnturkson.sandbox.ui.EarthquakeViewModel
import com.johnturkson.sandbox.work.EarthquakeWorkScheduler
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class EarthquakeDestinationStateTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun independentSelectionResultsRestoreAndIgnoreOlderUpdates() = runBlocking {
        val store =
            object : DataStore<Preferences> {
                override val data = flowOf(emptyPreferences())

                override suspend fun updateData(
                    transform: suspend (Preferences) -> Preferences
                ): Preferences = transform(emptyPreferences())
            }
        val context = composeRule.activity.applicationContext
        val locationRepository = EarthquakeLocationRepository(store)
        val provider =
            EarthquakeLocationProvider(
                LocationServices.getFusedLocationProviderClient(context),
                context,
            )
        val database = Room.inMemoryDatabaseBuilder(context, MainDatabase::class.java).build()
        val repository = EarthquakeRepository(database.earthquakeDao(), database.monitoringDao())
        val scheduler = EarthquakeWorkScheduler(WorkManager.getInstance(context))
        val connection =
            object : EarthquakeConnection {
                override val online = MutableStateFlow(false)

                override fun isOnline() = false
            }
        fun create(handle: SavedStateHandle) =
            EarthquakeViewModel(
                Destination.EarthquakeMapRoute,
                handle,
                locationRepository,
                provider,
                repository,
                EarthquakeMonitoringObserver(repository, scheduler, connection),
                scheduler,
            )
        val item = EarthquakeMonitoringFixtures.item(magnitude = 3.7)
        database.earthquakeDao().mergeEarthquakes(listOf(item))
        try {
            composeRule.runOnUiThread {
                val map = create(SavedStateHandle())
                val listHandle = SavedStateHandle()
                val observer = EarthquakeMonitoringObserver(repository, scheduler, connection)
                val list =
                    EarthquakeListViewModel(
                        Destination.EarthquakeListRoute,
                        listHandle,
                        repository,
                        observer,
                        scheduler,
                    )
                composeRule.activity.viewModelStore.put("map", map)
                composeRule.activity.viewModelStore.put("list", list)
                val first = EarthquakeSelectionResult(item.id, 1L)
                map.applySelection(first, focus = false)
                assertEquals(null, list.selectedMapItemId)
                list.applySelection(first)
                assertEquals(item.id, list.selectedMapItemId)
                assertEquals(first, list.scrollRequest)
                val restoredHandle =
                    SavedStateHandle.createHandle(listHandle.savedStateProvider().saveState(), null)
                val restored =
                    EarthquakeListViewModel(
                        Destination.EarthquakeListRoute,
                        restoredHandle,
                        repository,
                        observer,
                        scheduler,
                    )
                composeRule.activity.viewModelStore.put("restored-list", restored)
                assertEquals(item.id, restored.selectedMapItemId)
                assertEquals(first, restored.scrollRequest)
                restored.consumeScrollRequest(first)
                assertEquals(null, restored.scrollRequest)
                val second = EarthquakeSelectionResult("second", 2L)
                restored.applySelection(second, scroll = false)
                assertEquals(item.id, map.selectedMapItemId)
                map.applySelection(second)
                assertEquals("second", map.selectedMapItemId)
                assertEquals("second", map.requestedMapFocusId)
                restored.applySelection(first)
                map.applySelection(first)
                assertEquals("second", restored.selectedMapItemId)
                assertEquals("second", map.selectedMapItemId)
                val clear = EarthquakeSelectionResult(null, 3L)
                restored.applySelection(clear)
                map.applySelection(clear)
                assertEquals(null, restored.selectedMapItemId)
                assertEquals(null, restored.scrollRequest)
                assertEquals(null, map.selectedMapItemId)
                assertEquals(null, map.requestedMapFocusId)
                val repeated = EarthquakeSelectionResult(item.id, 4L)
                restored.applySelection(repeated)
                restored.consumeScrollRequest(repeated)
                val latest = repeated.copy(revision = 5L)
                restored.applySelection(latest)
                restored.consumeScrollRequest(repeated)
                assertEquals(latest, restored.scrollRequest)
            }
        } finally {
            composeRule.runOnUiThread { composeRule.activity.viewModelStore.clear() }
            database.close()
        }
    }
}
