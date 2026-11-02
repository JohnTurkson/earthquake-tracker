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
            ItemDatabaseModel::class,
            EarthquakeDatabaseModel::class,
            EarthquakeAliasDatabaseModel::class,
            EarthquakeRefreshDatabaseModel::class,
            EarthquakeAlertDatabaseModel::class,
        ],
    version = 5,
    autoMigrations =
        [
            AutoMigration(from = 1, to = 2),
            AutoMigration(from = 2, to = 3),
            AutoMigration(from = 3, to = 4, spec = MainDatabase.RemoveProductsMigration::class),
        ],
)
@TypeConverters(EarthquakeInstantConverters::class)
abstract class MainDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao

    abstract fun earthquakeDao(): EarthquakeDao

    abstract fun monitoringDao(): EarthquakeMonitoringDao

    @DeleteTable(tableName = "earthquake_details") class RemoveProductsMigration : AutoMigrationSpec
}
