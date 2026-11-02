package com.johnturkson.sandbox.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow

@Dao
interface EarthquakeMonitoringDao {
    @Query("SELECT * FROM earthquake_refresh WHERE id = 1")
    fun getRefreshFlow(): Flow<EarthquakeRefreshDatabaseModel?>

    @Upsert suspend fun updateRefresh(metadata: EarthquakeRefreshDatabaseModel)

    @Query("SELECT * FROM earthquake_alerts WHERE eventId = :eventId")
    fun getAlertFlow(eventId: String): Flow<EarthquakeAlertDatabaseModel?>

    @Query(
        "SELECT alerts.* FROM earthquake_alerts AS alerts JOIN earthquake_aliases AS aliases ON alerts.eventId = aliases.id WHERE aliases.earthquakeId = :earthquakeId"
    )
    fun getDeliveredAlertsFlow(earthquakeId: String): Flow<List<EarthquakeAlertDatabaseModel>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun recordAlert(alert: EarthquakeAlertDatabaseModel)

    @Query("DELETE FROM earthquake_alerts WHERE deliveredAt < :before")
    suspend fun pruneAlerts(before: Instant)
}
