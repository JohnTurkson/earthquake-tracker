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
            EarthquakeRefreshDatabaseModel::class,
            EarthquakeAlertDatabaseModel::class,
        ],
    version = 3,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
)
abstract class MainDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao

    abstract fun earthquakeDao(): EarthquakeDao

    abstract fun monitoringDao(): EarthquakeMonitoringDao
}
