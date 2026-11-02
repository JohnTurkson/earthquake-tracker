package com.johnturkson.sandbox

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.johnturkson.sandbox.data.MainDatabase
import kotlin.test.assertEquals
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
