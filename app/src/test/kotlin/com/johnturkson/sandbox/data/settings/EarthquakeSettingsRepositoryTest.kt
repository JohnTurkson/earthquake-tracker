package com.johnturkson.sandbox.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class EarthquakeSettingsRepositoryTest {
    @get:Rule val folder = TemporaryFolder()

    @Test
    fun defaultsAreOptInAndPreferencesSurviveReopening() = runTest {
        val file = folder.newFolder().resolve("settings.preferences_pb")
        val job = Job(backgroundScope.coroutineContext[Job])
        val store =
            PreferenceDataStoreFactory.create(
                scope = CoroutineScope(backgroundScope.coroutineContext + job),
                produceFile = { file },
            )
        val repository = EarthquakeSettingsRepository(store)
        assertEquals(EarthquakeSettings(), repository.settings.first())
        repository.setAlertsEnabled(true)
        repository.setMagnitude(7)
        repository.recordPermissionRequest()
        job.cancelAndJoin()
        val reopened =
            PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { file })
        assertEquals(
            EarthquakeSettings(
                alertsEnabled = true,
                minimumMagnitude = 7,
                hasRequestedNotifications = true,
            ),
            EarthquakeSettingsRepository(reopened).settings.first(),
        )
    }

    @Test
    fun invalidThresholdDoesNotChangeSavedPreference() = runTest {
        val file = folder.newFolder().resolve("settings.preferences_pb")
        val repository =
            EarthquakeSettingsRepository(
                PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { file })
            )
        assertFailsWith<IllegalArgumentException> { repository.setMagnitude(4) }
        assertEquals(6, repository.settings.first().minimumMagnitude)
    }
}
