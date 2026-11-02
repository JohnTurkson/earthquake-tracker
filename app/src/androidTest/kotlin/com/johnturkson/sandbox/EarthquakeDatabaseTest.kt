package com.johnturkson.sandbox

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.johnturkson.sandbox.data.EarthquakeAlertDatabaseModel
import com.johnturkson.sandbox.data.EarthquakeDatabase
import com.johnturkson.sandbox.data.EarthquakeRefreshDatabaseModel
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EarthquakeDatabaseTest {
    @Test
    fun freshDatabaseStoresTypedTimestampsAndReopensWithoutMigrations() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "earthquake-database-${UUID.randomUUID()}"
        val earthquake =
            EarthquakeMonitoringFixtures.item(
                    time = Instant.parse("1969-12-31T23:59:59.999999999Z"),
                    updated = Instant.parse("2026-11-01T00:00:00.123456789Z"),
                )
                .copy(ids = ",one,associated,")
        val refresh = EarthquakeRefreshDatabaseModel(updatedAt = earthquake.updated)
        val alert = EarthquakeAlertDatabaseModel("associated", earthquake.updated)
        try {
            val database =
                Room.databaseBuilder(context, EarthquakeDatabase::class.java, name).build()
            try {
                assertTrue(database.earthquakeDao().getEarthquakesFlow().first().isEmpty())
                assertNull(database.monitoringDao().getRefreshFlow().first())
                assertTrue(
                    database.monitoringDao().getDeliveredAlertsFlow(earthquake.id).first().isEmpty()
                )
                database.earthquakeDao().mergeEarthquakes(listOf(earthquake))
                database.monitoringDao().updateRefresh(refresh)
                database.monitoringDao().recordAlert(alert)
                val sqlite = database.openHelper.readableDatabase
                assertEquals(1, sqlite.version)
                sqlite
                    .query("SELECT typeof(time), time, typeof(updated), updated FROM earthquakes")
                    .use {
                        assertTrue(it.moveToFirst())
                        assertEquals("text", it.getString(0))
                        assertEquals("1969-12-31T23:59:59.999999999Z", it.getString(1))
                        assertEquals("text", it.getString(2))
                        assertEquals("2026-11-01T00:00:00.123456789Z", it.getString(3))
                    }
                sqlite
                    .query(
                        "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' AND name != 'android_metadata' AND name != 'room_master_table'"
                    )
                    .use {
                        val tables = buildSet { while (it.moveToNext()) add(it.getString(0)) }
                        assertEquals(
                            setOf(
                                "earthquakes",
                                "earthquake_aliases",
                                "earthquake_refresh",
                                "earthquake_alerts",
                            ),
                            tables,
                        )
                    }
            } finally {
                database.close()
            }
            val reopened =
                Room.databaseBuilder(context, EarthquakeDatabase::class.java, name).build()
            try {
                assertEquals(
                    listOf(earthquake),
                    reopened.earthquakeDao().getEarthquakesFlow().first(),
                )
                assertEquals(
                    earthquake,
                    reopened.earthquakeDao().getEarthquakeFlow("associated").first(),
                )
                assertEquals(refresh, reopened.monitoringDao().getRefreshFlow().first())
                assertEquals(
                    listOf(alert),
                    reopened.monitoringDao().getDeliveredAlertsFlow(earthquake.id).first(),
                )
            } finally {
                reopened.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
    }
}
