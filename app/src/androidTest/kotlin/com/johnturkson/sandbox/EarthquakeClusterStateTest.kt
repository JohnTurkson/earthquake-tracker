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
import com.johnturkson.sandbox.data.EarthquakeApi
import com.johnturkson.sandbox.data.EarthquakeClock
import com.johnturkson.sandbox.data.EarthquakeConnection
import com.johnturkson.sandbox.data.EarthquakeLocationProvider
import com.johnturkson.sandbox.data.EarthquakeLocationRepository
import com.johnturkson.sandbox.data.EarthquakeNotifications
import com.johnturkson.sandbox.data.EarthquakeRefreshRepository
import com.johnturkson.sandbox.data.EarthquakeRepository
import com.johnturkson.sandbox.data.EarthquakeSettingsRepository
import com.johnturkson.sandbox.data.MainDatabase
import com.johnturkson.sandbox.ui.Destination
import com.johnturkson.sandbox.ui.EarthquakeMapFixtures
import com.johnturkson.sandbox.ui.EarthquakeViewModel
import com.johnturkson.sandbox.work.EarthquakeWorkScheduler
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
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
        val database = Room.inMemoryDatabaseBuilder(context, MainDatabase::class.java).build()
        val client = HttpClient(MockEngine { error("Unexpected network request") })
        val api = EarthquakeApi(client)
        val repository = EarthquakeRepository(api, database.earthquakeDao(), Json, Dispatchers.IO)
        val settings = EarthquakeSettingsRepository(store)
        val notifications = EarthquakeNotifications(context)
        val scheduler = EarthquakeWorkScheduler(WorkManager.getInstance(context))
        val connection =
            object : EarthquakeConnection {
                override val online = MutableStateFlow(false)

                override fun isOnline() = false
            }
        val refresher =
            EarthquakeRefreshRepository(
                api,
                database,
                repository,
                database.earthquakeDao(),
                database.monitoringDao(),
                connection,
                EarthquakeClock(),
                settings,
                notifications,
                Dispatchers.IO,
            )
        fun create(handle: SavedStateHandle) =
            EarthquakeViewModel(
                Destination.EarthquakeRoute,
                handle,
                locationRepository,
                provider,
                repository,
                refresher,
                settings,
                notifications,
                scheduler,
                connection,
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
            client.close()
            database.close()
        }
    }
}
