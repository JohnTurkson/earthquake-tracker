package com.johnturkson.sandbox

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.johnturkson.sandbox.data.MainDatabase
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
                val database = Room.databaseBuilder(context, MainDatabase::class.java, name).build()
                try {
                    val earthquake = database.earthquakeDao().getEarthquakesFlow().first().single()
                    assertEquals("retained", earthquake.id)
                    assertEquals(200L, earthquake.updated)
                    assertEquals(2.0, earthquake.longitude)
                    assertEquals(1.0, earthquake.latitude)
                    assertEquals(10.0, earthquake.depth)
                    assertEquals(
                        earthquake,
                        database.earthquakeDao().getEarthquakeFlow("associated").first(),
                    )
                    assertEquals(
                        if (version >= 3) 300L else null,
                        database.monitoringDao().getRefreshFlow().first()?.updatedMillis,
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
    fun migratesVersionOneWithoutLosingExistingItems() = runTest {
        val name = "earthquake-migration-test"
        helper.createDatabase(name, 1).use { database ->
            database.execSQL("INSERT INTO ItemDatabaseModel (id) VALUES ('existing-item')")
        }
        helper.runMigrationsAndValidate(name, 2, true).use { database ->
            database.query("SELECT id FROM ItemDatabaseModel").use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals("existing-item", cursor.getString(0))
            }
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.databaseBuilder(context, MainDatabase::class.java, name).build()
        try {
            assertEquals(emptyList(), database.earthquakeDao().getEarthquakesFlow().first())
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }
}
