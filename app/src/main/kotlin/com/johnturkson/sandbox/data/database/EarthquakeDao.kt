package com.johnturkson.sandbox.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.johnturkson.sandbox.data.repository.aliasIds
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

@Dao
abstract class EarthquakeDao {
    @Query(
        """
        SELECT * FROM earthquakes
        WHERE (:startTime IS NULL OR time >= :startTime)
        AND (:endTime IS NULL OR time < :endTime)
        AND (:minimumMagnitude IS NULL OR magnitude >= :minimumMagnitude)
        ORDER BY time DESC, id ASC
        """
    )
    abstract fun getEarthquakesFlow(
        startTime: Instant? = null,
        endTime: Instant? = null,
        minimumMagnitude: Double? = null,
    ): Flow<List<EarthquakeDatabaseModel>>

    @Query(
        """
        SELECT earthquakes.* FROM earthquakes
        JOIN earthquake_aliases ON earthquakes.id = earthquake_aliases.earthquakeId
        WHERE earthquake_aliases.id = :id
        """
    )
    abstract fun getEarthquakeFlow(id: String): Flow<EarthquakeDatabaseModel?>

    @Transaction
    open suspend fun mergeEarthquakes(earthquakes: List<EarthquakeDatabaseModel>) {
        earthquakes.forEach { mergeEarthquake(it) }
    }

    @Transaction
    open suspend fun mergeEarthquake(earthquake: EarthquakeDatabaseModel) {
        val incomingAliases = earthquake.aliasIds()
        val existing = getAssociatedEarthquakesFlow(incomingAliases).first()
        val newestExisting = existing.maxByOrNull { it.updated }
        val newest =
            if (newestExisting != null && newestExisting.updated >= earthquake.updated) {
                newestExisting
            } else {
                earthquake
            }
        val existingIds = existing.map { it.id }
        val aliases =
            (incomingAliases + getAliasesFlow(existingIds).first().map { it.id }).distinct()
        val merged =
            newest.copy(
                detail =
                    newest.detail
                        ?: earthquake.detail
                        ?: existing.firstNotNullOfOrNull { it.detail }
            )
        upsertEarthquake(merged)
        upsertAliases(aliases.map { EarthquakeAliasDatabaseModel(it, merged.id) })
        deleteEarthquakes(existingIds.filter { it != merged.id })
    }

    @Query(
        """
        SELECT DISTINCT earthquakes.* FROM earthquakes
        JOIN earthquake_aliases ON earthquakes.id = earthquake_aliases.earthquakeId
        WHERE earthquake_aliases.id IN (:ids)
        """
    )
    protected abstract fun getAssociatedEarthquakesFlow(
        ids: List<String>
    ): Flow<List<EarthquakeDatabaseModel>>

    @Query("SELECT * FROM earthquake_aliases WHERE earthquakeId IN (:ids)")
    protected abstract fun getAliasesFlow(
        ids: List<String>
    ): Flow<List<EarthquakeAliasDatabaseModel>>

    @Upsert protected abstract suspend fun upsertEarthquake(earthquake: EarthquakeDatabaseModel)

    @Upsert
    protected abstract suspend fun upsertAliases(aliases: List<EarthquakeAliasDatabaseModel>)

    @Query("DELETE FROM earthquakes WHERE id IN (:ids)")
    protected abstract suspend fun deleteEarthquakes(ids: List<String>)
}
