package com.johnturkson.sandbox.data

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities =
        [
            ItemDatabaseModel::class,
            EarthquakeDatabaseModel::class,
            EarthquakeAliasDatabaseModel::class,
            EarthquakeDetailDatabaseModel::class,
        ],
    version = 2,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class MainDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao

    abstract fun earthquakeDao(): EarthquakeDao
}
