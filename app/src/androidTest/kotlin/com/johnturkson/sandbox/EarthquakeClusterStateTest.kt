package com.johnturkson.sandbox

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.SavedStateHandle
import com.google.android.gms.location.LocationServices
import com.johnturkson.sandbox.data.EarthquakeLocationProvider
import com.johnturkson.sandbox.data.EarthquakeLocationRepository
import com.johnturkson.sandbox.ui.Destination
import com.johnturkson.sandbox.ui.EarthquakeMapFixtures
import com.johnturkson.sandbox.ui.EarthquakeViewModel
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test

class EarthquakeClusterStateTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun clusterIdsAndEventSelectionRestoreAndDismissIndependently() {
        composeRule.runOnUiThread {
            val store =
                object : DataStore<Preferences> {
                    override val data = flowOf(emptyPreferences())

                    override suspend fun updateData(
                        transform: suspend (Preferences) -> Preferences
                    ): Preferences = transform(emptyPreferences())
                }
            val context = composeRule.activity.applicationContext
            val repository = EarthquakeLocationRepository(store)
            val provider =
                EarthquakeLocationProvider(
                    LocationServices.getFusedLocationProviderClient(context),
                    context,
                )
            val handle = SavedStateHandle()
            val original =
                EarthquakeViewModel(Destination.EarthquakeRoute, handle, repository, provider)
            composeRule.activity.viewModelStore.put("original", original)
            val items = EarthquakeMapFixtures.denseItems.take(8)
            original.selectMapCluster(items + items.first())
            original.selectMapItem(items.last())
            val restoredHandle =
                SavedStateHandle.createHandle(handle.savedStateProvider().saveState(), null)
            val restored =
                EarthquakeViewModel(
                    Destination.EarthquakeRoute,
                    restoredHandle,
                    repository,
                    provider,
                )
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
        }
    }
}
