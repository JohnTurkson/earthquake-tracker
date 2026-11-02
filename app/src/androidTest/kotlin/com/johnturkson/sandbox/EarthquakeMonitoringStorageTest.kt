package com.johnturkson.sandbox

import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.platform.app.InstrumentationRegistry
import com.johnturkson.sandbox.data.EarthquakeAlertDatabaseModel
import com.johnturkson.sandbox.data.EarthquakeAlertDelivery
import com.johnturkson.sandbox.data.EarthquakeDatabase
import com.johnturkson.sandbox.data.EarthquakeRefreshDatabaseModel
import com.johnturkson.sandbox.data.EarthquakeSettings
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class EarthquakeMonitoringStorageTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun dataRefreshMetadataAndAlertHistorySurviveReopening() = runTest {
        val name = "monitoring-test-${UUID.randomUUID()}"
        val item = EarthquakeMonitoringFixtures.item()
        try {
            val first = Room.databaseBuilder(context, EarthquakeDatabase::class.java, name).build()
            try {
                first.withTransaction {
                    first.earthquakeDao().mergeEarthquakes(listOf(item))
                    first
                        .monitoringDao()
                        .updateRefresh(
                            EarthquakeRefreshDatabaseModel(
                                updatedAt = Instant.fromEpochMilliseconds(200)
                            )
                        )
                }
                first
                    .monitoringDao()
                    .recordAlert(
                        EarthquakeAlertDatabaseModel(item.id, Instant.fromEpochMilliseconds(200))
                    )
            } finally {
                first.close()
            }
            val reopened =
                Room.databaseBuilder(context, EarthquakeDatabase::class.java, name).build()
            try {
                assertEquals(listOf(item), reopened.earthquakeDao().getEarthquakesFlow().first())
                assertEquals(
                    Instant.fromEpochMilliseconds(200),
                    reopened.monitoringDao().getRefreshFlow().first()?.updatedAt,
                )
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
        val database = Room.inMemoryDatabaseBuilder(context, EarthquakeDatabase::class.java).build()
        val original = EarthquakeMonitoringFixtures.item()
        try {
            database.earthquakeDao().mergeEarthquakes(listOf(original))
            database
                .monitoringDao()
                .updateRefresh(
                    EarthquakeRefreshDatabaseModel(updatedAt = Instant.fromEpochMilliseconds(200))
                )
            assertFailsWith<IllegalStateException> {
                database.withTransaction {
                    database
                        .earthquakeDao()
                        .mergeEarthquakes(
                            listOf(
                                original.copy(
                                    updated = original.updated + 1.milliseconds,
                                    magnitude = 7.0,
                                )
                            )
                        )
                    database
                        .monitoringDao()
                        .updateRefresh(
                            EarthquakeRefreshDatabaseModel(
                                updatedAt = Instant.fromEpochMilliseconds(300)
                            )
                        )
                    error("Storage transaction interrupted")
                }
            }
            assertEquals(listOf(original), database.earthquakeDao().getEarthquakesFlow().first())
            assertEquals(
                Instant.fromEpochMilliseconds(200),
                database.monitoringDao().getRefreshFlow().first()?.updatedAt,
            )
            database.withTransaction {
                database.earthquakeDao().mergeEarthquakes(emptyList())
                database
                    .monitoringDao()
                    .updateRefresh(
                        EarthquakeRefreshDatabaseModel(
                            updatedAt = Instant.fromEpochMilliseconds(400)
                        )
                    )
            }
            assertEquals(listOf(original), database.earthquakeDao().getEarthquakesFlow().first())
            assertEquals(
                Instant.fromEpochMilliseconds(400),
                database.monitoringDao().getRefreshFlow().first()?.updatedAt,
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun changedPreferredIdsDoNotRepeatDeliveredAlerts() = runTest {
        val database = Room.inMemoryDatabaseBuilder(context, EarthquakeDatabase::class.java).build()
        try {
            val original =
                EarthquakeMonitoringFixtures.item(id = "original").copy(ids = ",original,shared,")
            database.earthquakeDao().mergeEarthquakes(listOf(original))
            database
                .monitoringDao()
                .recordAlert(
                    EarthquakeAlertDatabaseModel(
                        original.id,
                        Instant.fromEpochMilliseconds(100_000),
                    )
                )
            val revised =
                original.copy(
                    id = "preferred",
                    updated = original.updated + 1.milliseconds,
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
                Instant.fromEpochMilliseconds(100_010),
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
}
