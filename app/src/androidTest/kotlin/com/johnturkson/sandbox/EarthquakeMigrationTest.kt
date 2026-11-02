package com.johnturkson.sandbox

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.johnturkson.sandbox.data.EarthquakeTimestampMigration
import com.johnturkson.sandbox.data.MainDatabase
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EarthquakeMigrationTest {
    @get:Rule
    val helper =
        MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), MainDatabase::class.java)

    @Test
    fun convertsLegacyTimestampsToIsoTextAndRetainsAliasesAndMonitoring() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        for (version in listOf(3, 4, 5)) {
            val name = "timestamp-migration-${UUID.randomUUID()}"
            try {
                helper.createDatabase(name, version).use { database ->
                    database.execSQL("INSERT INTO ItemDatabaseModel (id) VALUES ('retained-item')")
                    database.execSQL(
                        if (version >= 5)
                            "INSERT INTO earthquakes (id, time, updated, longitude, latitude, depth) VALUES ('retained', '1969-12-31T23:59:59.999000000Z', '1970-01-01T00:00:01.000000000Z', 2.0, 1.0, 10.0)"
                        else
                            "INSERT INTO earthquakes (id, time, updated, longitude, latitude, depth) VALUES ('retained', -1, 1000, 2.0, 1.0, 10.0)"
                    )
                    database.execSQL(
                        "INSERT INTO earthquake_aliases (id, earthquakeId) VALUES ('alias', 'retained')"
                    )
                    database.execSQL(
                        if (version >= 5)
                            "INSERT INTO earthquake_refresh (id, updatedAt) VALUES (1, '1970-01-01T00:00:02.000000000Z')"
                        else "INSERT INTO earthquake_refresh (id, updatedMillis) VALUES (1, 2000)"
                    )
                    database.execSQL(
                        if (version >= 5)
                            "INSERT INTO earthquake_alerts (eventId, deliveredAt) VALUES ('alias', '1970-01-01T00:00:03.000000000Z')"
                        else
                            "INSERT INTO earthquake_alerts (eventId, deliveredMillis) VALUES ('alias', 3000)"
                    )
                }
                helper.runMigrationsAndValidate(name, 6, true, EarthquakeTimestampMigration).use {
                    database ->
                    database.query("SELECT typeof(time), time, updated FROM earthquakes").use {
                        cursor ->
                        assertEquals(true, cursor.moveToFirst())
                        assertEquals("text", cursor.getString(0))
                        assertEquals("1969-12-31T23:59:59.999000000Z", cursor.getString(1))
                        assertEquals("1970-01-01T00:00:01.000000000Z", cursor.getString(2))
                    }
                    database
                        .query("SELECT name FROM sqlite_master WHERE name = 'ItemDatabaseModel'")
                        .use { cursor -> assertFalse(cursor.moveToFirst()) }
                }
                val database =
                    Room.databaseBuilder(context, MainDatabase::class.java, name)
                        .addMigrations(EarthquakeTimestampMigration)
                        .build()
                try {
                    assertEquals(
                        Instant.fromEpochMilliseconds(-1),
                        database.earthquakeDao().getEarthquakeFlow("alias").first()!!.time,
                    )
                    assertEquals(
                        Instant.fromEpochMilliseconds(2000),
                        database.monitoringDao().getRefreshFlow().first()!!.updatedAt,
                    )
                    assertEquals(
                        Instant.fromEpochMilliseconds(3000),
                        database
                            .monitoringDao()
                            .getDeliveredAlertsFlow("retained")
                            .first()
                            .single()
                            .deliveredAt,
                    )
                } finally {
                    database.close()
                }
            } finally {
                context.deleteDatabase(name)
            }
        }
    }

    @Test
    fun dropsUnusedProductsAndRetainsStoredEventsAndMonitoring() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        for (version in listOf(2, 3)) {
            val name = "earthquake-products-migration-${UUID.randomUUID()}"
            try {
                helper.createDatabase(name, version).use { database ->
                    database.execSQL(
                        "INSERT INTO earthquakes (id, time, updated, longitude, latitude, depth) VALUES ('retained', 100, 200, 2.0, 1.0, 10.0)"
                    )
                    database.execSQL(
                        "INSERT INTO earthquake_aliases (id, earthquakeId) VALUES ('associated', 'retained')"
                    )
                    database.execSQL(
                        "INSERT INTO earthquake_details (earthquakeId, updated, productsJson) VALUES ('retained', 200, '{}')"
                    )
                    if (version >= 3) {
                        database.execSQL(
                            "INSERT INTO earthquake_refresh (id, updatedMillis) VALUES (1, 300)"
                        )
                        database.execSQL(
                            "INSERT INTO earthquake_alerts (eventId, deliveredMillis) VALUES ('associated', 400)"
                        )
                    }
                }
                helper.runMigrationsAndValidate(name, 4, true).use { database ->
                    database
                        .query("SELECT name FROM sqlite_master WHERE name = 'earthquake_details'")
                        .use { cursor -> assertFalse(cursor.moveToFirst()) }
                }
                val database =
                    Room.databaseBuilder(context, MainDatabase::class.java, name)
                        .addMigrations(EarthquakeTimestampMigration)
                        .build()
                try {
                    val earthquake = database.earthquakeDao().getEarthquakesFlow().first().single()
                    assertEquals("retained", earthquake.id)
                    assertEquals(Instant.fromEpochMilliseconds(200), earthquake.updated)
                    assertEquals(2.0, earthquake.longitude)
                    assertEquals(1.0, earthquake.latitude)
                    assertEquals(10.0, earthquake.depth)
                    assertEquals(
                        earthquake,
                        database.earthquakeDao().getEarthquakeFlow("associated").first(),
                    )
                    assertEquals(
                        if (version >= 3) Instant.fromEpochMilliseconds(300) else null,
                        database.monitoringDao().getRefreshFlow().first()?.updatedAt,
                    )
                    assertEquals(
                        if (version >= 3) listOf("associated") else emptyList(),
                        database.monitoringDao().getDeliveredAlertsFlow("retained").first().map {
                            it.eventId
                        },
                    )
                } finally {
                    database.close()
                }
            } finally {
                context.deleteDatabase(name)
            }
        }
    }

    @Test
    fun migratesVersionOneToEarthquakeOnlyStorage() = runTest {
        val name = "earthquake-migration-${UUID.randomUUID()}"
        helper.createDatabase(name, 1).use { database ->
            database.execSQL("INSERT INTO ItemDatabaseModel (id) VALUES ('existing-item')")
        }
        helper.runMigrationsAndValidate(name, 6, true, EarthquakeTimestampMigration).use { database
            ->
            database.query("SELECT name FROM sqlite_master WHERE name = 'ItemDatabaseModel'").use {
                cursor ->
                assertFalse(cursor.moveToFirst())
            }
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database =
            Room.databaseBuilder(context, MainDatabase::class.java, name)
                .addMigrations(EarthquakeTimestampMigration)
                .build()
        try {
            assertEquals(emptyList(), database.earthquakeDao().getEarthquakesFlow().first())
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }
}
