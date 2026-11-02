package com.johnturkson.sandbox

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import com.johnturkson.sandbox.data.EarthquakeLocation
import com.johnturkson.sandbox.data.EarthquakeLocationRepository
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class EarthquakeLocationRepositoryTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test
    fun missingLocationStaysNullAndLeavesDefaultAsSanFrancisco() = runTest {
        val file = temporaryFolder.newFolder().resolve("location.preferences_pb")
        val store =
            PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { file })
        assertNull(EarthquakeLocationRepository(store).lastKnownLocation.first())
        assertEquals(37.7749, EarthquakeLocation.Default.latitude)
        assertEquals(-122.4194, EarthquakeLocation.Default.longitude)
        assertNull(EarthquakeLocation.Default.timestampMillis)
    }

    @Test
    fun measuredLocationSurvivesReopeningDataStore() = runTest {
        val file = temporaryFolder.newFolder().resolve("location.preferences_pb")
        val storeJob = Job(backgroundScope.coroutineContext[Job])
        val firstStore =
            PreferenceDataStoreFactory.create(
                scope = CoroutineScope(backgroundScope.coroutineContext + storeJob),
                produceFile = { file },
            )
        val location = EarthquakeLocation(49.2827, -123.1207, 1_000L)
        EarthquakeLocationRepository(firstStore).updateLastKnownLocation(location)
        storeJob.cancelAndJoin()

        val reopenedStore =
            PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { file })
        assertEquals(
            location,
            EarthquakeLocationRepository(reopenedStore).lastKnownLocation.first(),
        )
    }

    @Test
    fun incompleteCoordinatesAreNotReturnedAsAUserLocation() = runTest {
        val file = temporaryFolder.newFolder().resolve("location.preferences_pb")
        val store =
            PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { file })
        store.edit { it[doublePreferencesKey("last_known_user_latitude")] = 49.2827 }
        assertNull(EarthquakeLocationRepository(store).lastKnownLocation.first())
    }

    @Test
    fun olderCachedFixCannotReplaceANewerFix() = runTest {
        val file = temporaryFolder.newFolder().resolve("location.preferences_pb")
        val store =
            PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { file })
        val repository = EarthquakeLocationRepository(store)
        val latest = EarthquakeLocation(49.2827, -123.1207, 2_000L)
        repository.updateLastKnownLocation(latest)
        repository.updateLastKnownLocation(EarthquakeLocation(37.7749, -122.4194, 1_000L))
        assertEquals(latest, repository.lastKnownLocation.first())
    }

    @Test
    fun invalidFixDoesNotReplaceStoredLocation() = runTest {
        val file = temporaryFolder.newFolder().resolve("location.preferences_pb")
        val store =
            PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { file })
        val repository = EarthquakeLocationRepository(store)
        val location = EarthquakeLocation(49.2827, -123.1207, 1_000L)
        repository.updateLastKnownLocation(location)
        assertFailsWith<IllegalArgumentException> {
            repository.updateLastKnownLocation(EarthquakeLocation(Double.NaN, 0.0, 2_000L))
        }
        assertEquals(location, repository.lastKnownLocation.first())
    }

    @Test
    fun unreadablePreferencesFallBackToNoKnownLocation() = runTest {
        val store =
            object : DataStore<Preferences> {
                override val data: Flow<Preferences> = flow { throw IOException("Read failed") }

                override suspend fun updateData(
                    transform: suspend (Preferences) -> Preferences
                ): Preferences {
                    throw IOException("Write failed")
                }
            }
        assertNull(EarthquakeLocationRepository(store).lastKnownLocation.first())
    }
}
