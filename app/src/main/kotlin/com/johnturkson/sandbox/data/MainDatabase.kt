package com.johnturkson.sandbox.data

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteTable
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec

@Database(
    entities =
        [
            EarthquakeDatabaseModel::class,
            EarthquakeAliasDatabaseModel::class,
            EarthquakeRefreshDatabaseModel::class,
            EarthquakeAlertDatabaseModel::class,
        ],
    version = 6,
    autoMigrations =
        [
            AutoMigration(from = 1, to = 2),
            AutoMigration(from = 2, to = 3),
            AutoMigration(from = 3, to = 4, spec = MainDatabase.RemoveProductsMigration::class),
            AutoMigration(from = 5, to = 6, spec = MainDatabase.EarthquakeOnlyMigration::class),
        ],
)
@TypeConverters(EarthquakeInstantConverters::class)
abstract class MainDatabase : RoomDatabase() {
    abstract fun earthquakeDao(): EarthquakeDao

    abstract fun monitoringDao(): EarthquakeMonitoringDao

    @DeleteTable(tableName = "earthquake_details") class RemoveProductsMigration : AutoMigrationSpec

    @DeleteTable(tableName = "ItemDatabaseModel") class EarthquakeOnlyMigration : AutoMigrationSpec
}
