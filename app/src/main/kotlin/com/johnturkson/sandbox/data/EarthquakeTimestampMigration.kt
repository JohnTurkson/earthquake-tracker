package com.johnturkson.sandbox.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlin.time.Instant

object EarthquakeTimestampMigration : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val converters = EarthquakeInstantConverters()
        db.execSQL("CREATE TEMP TABLE timestamp_backup_earthquakes AS SELECT * FROM earthquakes")
        db.execSQL(
            "CREATE TEMP TABLE timestamp_backup_earthquake_aliases AS SELECT * FROM earthquake_aliases"
        )
        db.execSQL(
            "CREATE TEMP TABLE timestamp_backup_earthquake_refresh AS SELECT * FROM earthquake_refresh"
        )
        db.execSQL(
            "CREATE TEMP TABLE timestamp_backup_earthquake_alerts AS SELECT * FROM earthquake_alerts"
        )
        db.execSQL("DROP TABLE earthquake_aliases")
        db.execSQL("DROP TABLE earthquakes")
        db.execSQL("DROP TABLE earthquake_refresh")
        db.execSQL("DROP TABLE earthquake_alerts")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `earthquakes` (`id` TEXT NOT NULL, `time` TEXT NOT NULL,
            `updated` TEXT NOT NULL, `longitude` REAL NOT NULL, `latitude` REAL NOT NULL,
            `depth` REAL NOT NULL, `magnitude` REAL, `place` TEXT, `tz` INTEGER, `url` TEXT,
            `detail` TEXT, `felt` INTEGER, `cdi` REAL, `mmi` REAL, `alert` TEXT, `status` TEXT,
            `tsunami` INTEGER, `significance` INTEGER, `network` TEXT, `code` TEXT, `ids` TEXT,
            `sources` TEXT, `types` TEXT, `stationCount` INTEGER, `minimumDistance` REAL, `rms`
            REAL, `gap` REAL, `magnitudeType` TEXT, `type` TEXT, `title` TEXT, PRIMARY
            KEY(`id`))
            """
        )
        db.execSQL(
            """
            INSERT INTO earthquakes (`id`, `time`, `updated`, `longitude`, `latitude`, `depth`,
            `magnitude`, `place`, `tz`, `url`, `detail`, `felt`, `cdi`, `mmi`, `alert`,
            `status`, `tsunami`, `significance`, `network`, `code`, `ids`, `sources`, `types`,
            `stationCount`, `minimumDistance`, `rms`, `gap`, `magnitudeType`, `type`, `title`)
            SELECT `id`, `time`, `updated`, `longitude`, `latitude`, `depth`, `magnitude`,
            `place`, `tz`, `url`, `detail`, `felt`, `cdi`, `mmi`, `alert`, `status`, `tsunami`,
            `significance`, `network`, `code`, `ids`, `sources`, `types`, `stationCount`,
            `minimumDistance`, `rms`, `gap`, `magnitudeType`, `type`, `title` FROM
            timestamp_backup_earthquakes
            """
        )
        db.query("SELECT id, time, updated FROM timestamp_backup_earthquakes").use { cursor ->
            while (cursor.moveToNext()) {
                db.execSQL(
                    "UPDATE earthquakes SET time = ?, updated = ? WHERE id = ?",
                    arrayOf(
                        converters.toTimestamp(Instant.fromEpochMilliseconds(cursor.getLong(1))),
                        converters.toTimestamp(Instant.fromEpochMilliseconds(cursor.getLong(2))),
                        cursor.getString(0),
                    ),
                )
            }
        }
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_earthquakes_time` ON `earthquakes` (`time`)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_earthquakes_updated` ON `earthquakes` (`updated`)"
        )
        db.execSQL("DROP TABLE timestamp_backup_earthquakes")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `earthquake_aliases` (`id` TEXT NOT NULL, `earthquakeId`
            TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`earthquakeId`) REFERENCES
            `earthquakes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )
            """
        )
        db.execSQL(
            """
            INSERT INTO earthquake_aliases (`id`, `earthquakeId`) SELECT `id`, `earthquakeId`
            FROM timestamp_backup_earthquake_aliases
            """
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_earthquake_aliases_earthquakeId` ON `earthquake_aliases` (`earthquakeId`)"
        )
        db.execSQL("DROP TABLE timestamp_backup_earthquake_aliases")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `earthquake_refresh` (`id` INTEGER NOT NULL, `updatedAt`
            TEXT NOT NULL, PRIMARY KEY(`id`))
            """
        )
        db.execSQL(
            """
            INSERT INTO earthquake_refresh (`id`, `updatedAt`) SELECT `id`, `updatedMillis` FROM
            timestamp_backup_earthquake_refresh
            """
        )
        db.query("SELECT id, updatedMillis FROM timestamp_backup_earthquake_refresh").use { cursor
            ->
            while (cursor.moveToNext()) {
                db.execSQL(
                    "UPDATE earthquake_refresh SET updatedAt = ? WHERE id = ?",
                    arrayOf(
                        converters.toTimestamp(Instant.fromEpochMilliseconds(cursor.getLong(1))),
                        cursor.getString(0),
                    ),
                )
            }
        }
        db.execSQL("DROP TABLE timestamp_backup_earthquake_refresh")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `earthquake_alerts` (`eventId` TEXT NOT NULL,
            `deliveredAt` TEXT NOT NULL, PRIMARY KEY(`eventId`))
            """
        )
        db.execSQL(
            """
            INSERT INTO earthquake_alerts (`eventId`, `deliveredAt`) SELECT `eventId`,
            `deliveredMillis` FROM timestamp_backup_earthquake_alerts
            """
        )
        db.query("SELECT eventId, deliveredMillis FROM timestamp_backup_earthquake_alerts").use {
            cursor ->
            while (cursor.moveToNext()) {
                db.execSQL(
                    "UPDATE earthquake_alerts SET deliveredAt = ? WHERE eventId = ?",
                    arrayOf(
                        converters.toTimestamp(Instant.fromEpochMilliseconds(cursor.getLong(1))),
                        cursor.getString(0),
                    ),
                )
            }
        }
        db.execSQL("DROP TABLE timestamp_backup_earthquake_alerts")
    }
}
