package com.johnturkson.sandbox.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities =
        [
            EarthquakeDatabaseModel::class,
            EarthquakeAliasDatabaseModel::class,
            EarthquakeRefreshDatabaseModel::class,
            EarthquakeAlertDatabaseModel::class,
        ],
    version = 1,
)
@TypeConverters(EarthquakeInstantConverters::class)
abstract class EarthquakeDatabase : RoomDatabase() {
    abstract fun earthquakeDao(): EarthquakeDao

    abstract fun monitoringDao(): EarthquakeMonitoringDao
}
