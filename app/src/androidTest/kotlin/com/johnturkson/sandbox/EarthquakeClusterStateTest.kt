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
import com.johnturkson.sandbox.data.EarthquakeDatabase
import com.johnturkson.sandbox.data.EarthquakeLocationProvider
import com.johnturkson.sandbox.data.EarthquakeLocationRepository
import com.johnturkson.sandbox.data.EarthquakeRepository
import com.johnturkson.sandbox.ui.Destination
import com.johnturkson.sandbox.ui.EarthquakeMapFixtures
import com.johnturkson.sandbox.ui.EarthquakeMonitoringObserver
import com.johnturkson.sandbox.ui.EarthquakeViewModel
import com.johnturkson.sandbox.work.EarthquakeMonitor
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class EarthquakeClusterStateTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun clusterIdsAndEventSelectionRestoreAndDismissIndependently() = runBlocking {
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
        val database = Room.inMemoryDatabaseBuilder(context, EarthquakeDatabase::class.java).build()
        val repository = EarthquakeRepository(database.earthquakeDao(), database.monitoringDao())
        val scheduler = EarthquakeMonitor(WorkManager.getInstance(context))
        fun create(handle: SavedStateHandle) =
            EarthquakeViewModel(
                Destination.EarthquakeMapRoute,
                handle,
                locationRepository,
                provider,
                repository,
                EarthquakeMonitoringObserver(repository, scheduler),
                scheduler,
            )
        val stored = EarthquakeMonitoringFixtures.item(magnitude = 3.7)
        database.earthquakeDao().mergeEarthquakes(listOf(stored))
        try {
            lateinit var restored: EarthquakeViewModel
            composeRule.runOnUiThread {
                val handle = SavedStateHandle()
                val original = create(handle)
                composeRule.activity.viewModelStore.put("original", original)
                val items = EarthquakeMapFixtures.denseItems.take(8)
                original.selectMapCluster(items + items.first())
                original.selectMapItem(items.last())
                val restoredHandle =
                    SavedStateHandle.createHandle(handle.savedStateProvider().saveState(), null)
                restored = create(restoredHandle)
                composeRule.activity.viewModelStore.put("restored", restored)
                assertEquals(items.map { it.id }, restored.selectedClusterItemIds)
                assertEquals(items.last().id, restored.selectedMapItemId)
                restored.dismissMapCluster()
                assertEquals(emptyList(), restored.selectedClusterItemIds)
                assertEquals(items.last().id, restored.selectedMapItemId)
                restored.selectMapCluster(items)
                restored.clearMapSelection()
                assertEquals(emptyList(), restored.selectedClusterItemIds)
                assertEquals(null, restored.selectedMapItemId)
                restored.selectMapCluster(items)
                restored.openNotificationEvent(stored.id)
            }
            composeRule.waitUntil(5_000) {
                restored.mapItems.isNotEmpty() && restored.requestedMapFocusId == stored.id
            }
            composeRule.runOnUiThread {
                assertEquals(3.7, restored.mapItems.single().magnitude)
                assertEquals(emptyList(), restored.selectedClusterItemIds)
                assertEquals(stored.id, restored.selectedMapItemId)
            }
        } finally {
            composeRule.runOnUiThread { composeRule.activity.viewModelStore.clear() }
            database.close()
        }
    }
}
