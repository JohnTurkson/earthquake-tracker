package com.johnturkson.sandbox

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.room.withTransaction
import androidx.test.platform.app.InstrumentationRegistry
import com.johnturkson.sandbox.data.EarthquakeAlertDatabaseModel
import com.johnturkson.sandbox.data.EarthquakeAlertDelivery
import com.johnturkson.sandbox.data.EarthquakeRefreshDatabaseModel
import com.johnturkson.sandbox.data.EarthquakeSettings
import com.johnturkson.sandbox.data.MainDatabase
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class EarthquakeMonitoringStorageTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    @get:Rule
    val helper =
        MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), MainDatabase::class.java)

    @Test
    fun dataRefreshMetadataAndAlertHistorySurviveReopening() = runTest {
        val name = "monitoring-test-${UUID.randomUUID()}"
        val item = EarthquakeMonitoringFixtures.item()
        try {
            val first = Room.databaseBuilder(context, MainDatabase::class.java, name).build()
            try {
                first.withTransaction {
                    first.earthquakeDao().mergeEarthquakes(listOf(item))
                    first
                        .monitoringDao()
                        .updateRefresh(EarthquakeRefreshDatabaseModel(updatedMillis = 200))
                }
                first.monitoringDao().recordAlert(EarthquakeAlertDatabaseModel(item.id, 200))
            } finally {
                first.close()
            }
            val reopened = Room.databaseBuilder(context, MainDatabase::class.java, name).build()
            try {
                assertEquals(listOf(item), reopened.earthquakeDao().getEarthquakesFlow().first())
                assertEquals(200, reopened.monitoringDao().getRefreshFlow().first()?.updatedMillis)
                assertEquals(
                    item.id,
                    reopened
                        .monitoringDao()
                        .getDeliveredAlertsFlow(item.id)
                        .first()
                        .single()
                        .eventId,
                )
            } finally {
                reopened.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
    }

    @Test
    fun mergeAndRefreshTimestampRollBackTogether() = runTest {
        val database = Room.inMemoryDatabaseBuilder(context, MainDatabase::class.java).build()
        val original = EarthquakeMonitoringFixtures.item()
        try {
            database.earthquakeDao().mergeEarthquakes(listOf(original))
            database
                .monitoringDao()
                .updateRefresh(EarthquakeRefreshDatabaseModel(updatedMillis = 200))
            assertFailsWith<IllegalStateException> {
                database.withTransaction {
                    database
                        .earthquakeDao()
                        .mergeEarthquakes(
                            listOf(original.copy(updated = original.updated + 1, magnitude = 7.0))
                        )
                    database
                        .monitoringDao()
                        .updateRefresh(EarthquakeRefreshDatabaseModel(updatedMillis = 300))
                    error("Storage transaction interrupted")
                }
            }
            assertEquals(listOf(original), database.earthquakeDao().getEarthquakesFlow().first())
            assertEquals(200, database.monitoringDao().getRefreshFlow().first()?.updatedMillis)
            database.withTransaction {
                database.earthquakeDao().mergeEarthquakes(emptyList())
                database
                    .monitoringDao()
                    .updateRefresh(EarthquakeRefreshDatabaseModel(updatedMillis = 400))
            }
            assertEquals(listOf(original), database.earthquakeDao().getEarthquakesFlow().first())
            assertEquals(400, database.monitoringDao().getRefreshFlow().first()?.updatedMillis)
        } finally {
            database.close()
        }
    }

    @Test
    fun changedPreferredIdsDoNotRepeatDeliveredAlerts() = runTest {
        val database = Room.inMemoryDatabaseBuilder(context, MainDatabase::class.java).build()
        try {
            val original =
                EarthquakeMonitoringFixtures.item(id = "original").copy(ids = ",original,shared,")
            database.earthquakeDao().mergeEarthquakes(listOf(original))
            database.monitoringDao().recordAlert(EarthquakeAlertDatabaseModel(original.id, 100_000))
            val revised =
                original.copy(
                    id = "preferred",
                    updated = original.updated + 1,
                    ids = ",preferred,shared,",
                )
            database.earthquakeDao().mergeEarthquakes(listOf(revised))
            var posts = 0
            val alerts =
                EarthquakeAlertDelivery(
                    { database.monitoringDao().getDeliveredAlertsFlow(it).first().isNotEmpty() },
                    {
                        posts++
                        true
                    },
                    database.monitoringDao()::recordAlert,
                )
            alerts.deliver(
                database.earthquakeDao().getEarthquakesFlow().first(),
                EarthquakeSettings(alertsEnabled = true),
                100_010,
            )
            assertEquals(0, posts)
            assertEquals(
                "original",
                database
                    .monitoringDao()
                    .getDeliveredAlertsFlow(revised.id)
                    .first()
                    .single()
                    .eventId,
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun migrationAddsMonitoringWithoutLosingVersionTwoEarthquakes() = runTest {
        val name = "monitoring-migration-${UUID.randomUUID()}"
        try {
            helper.createDatabase(name, 2).use { database ->
                database.execSQL(
                    "INSERT INTO earthquakes (id, time, updated, longitude, latitude, depth) VALUES ('retained', 100, 200, 2.0, 1.0, 10.0)"
                )
                database.execSQL(
                    "INSERT INTO earthquake_aliases (id, earthquakeId) VALUES ('retained', 'retained')"
                )
            }
            helper.runMigrationsAndValidate(name, 3, true).close()
            val database = Room.databaseBuilder(context, MainDatabase::class.java, name).build()
            try {
                assertEquals(
                    "retained",
                    database.earthquakeDao().getEarthquakesFlow().first().single().id,
                )
                assertNull(database.monitoringDao().getRefreshFlow().first())
                assertEquals(
                    emptyList(),
                    database.monitoringDao().getDeliveredAlertsFlow("retained").first(),
                )
            } finally {
                database.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
    }
}
