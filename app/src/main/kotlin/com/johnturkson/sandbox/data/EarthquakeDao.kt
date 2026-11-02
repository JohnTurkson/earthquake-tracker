package com.johnturkson.sandbox.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

@Dao
abstract class EarthquakeDao {
    @Query(
        """
        SELECT * FROM earthquakes
        WHERE time >= :startTime AND time < :endTime
        AND (:minimumMagnitude IS NULL OR magnitude >= :minimumMagnitude)
        ORDER BY time DESC, id ASC
        """
    )
    abstract fun getEarthquakesFlow(
        startTime: Long = Long.MIN_VALUE,
        endTime: Long = Long.MAX_VALUE,
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
    @Query(
        """
        SELECT earthquakes.* FROM earthquakes
        JOIN earthquake_aliases ON earthquakes.id = earthquake_aliases.earthquakeId
        WHERE earthquake_aliases.id = :id
        """
    )
    abstract fun getEarthquakeDetailFlow(id: String): Flow<EarthquakeWithDetailDatabaseModel?>

    @Transaction
    open suspend fun mergeEarthquakes(earthquakes: List<EarthquakeDatabaseModel>) {
        earthquakes.forEach { mergeEarthquake(it) }
    }

    @Transaction
    open suspend fun mergeEarthquake(
        earthquake: EarthquakeDatabaseModel,
        detail: EarthquakeDetailDatabaseModel? = null,
    ) {
        require(detail == null || detail.earthquakeId == earthquake.id)
        require(detail == null || detail.updated == earthquake.updated)
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
        val newestDetail =
            (getDetailsFlow(existingIds).first() + listOfNotNull(detail)).maxByOrNull { it.updated }
        val merged =
            newest.copy(
                detail =
                    newest.detail
                        ?: earthquake.detail
                        ?: existing.firstNotNullOfOrNull { it.detail }
            )
        upsertEarthquake(merged)
        upsertAliases(aliases.map { EarthquakeAliasDatabaseModel(it, merged.id) })
        if (newestDetail != null) {
            upsertDetail(newestDetail.copy(earthquakeId = merged.id))
        }
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

    @Query("SELECT * FROM earthquake_details WHERE earthquakeId IN (:ids)")
    protected abstract fun getDetailsFlow(
        ids: List<String>
    ): Flow<List<EarthquakeDetailDatabaseModel>>

    @Upsert protected abstract suspend fun upsertEarthquake(earthquake: EarthquakeDatabaseModel)

    @Upsert
    protected abstract suspend fun upsertAliases(aliases: List<EarthquakeAliasDatabaseModel>)

    @Upsert protected abstract suspend fun upsertDetail(detail: EarthquakeDetailDatabaseModel)

    @Query("DELETE FROM earthquakes WHERE id IN (:ids)")
    protected abstract suspend fun deleteEarthquakes(ids: List<String>)
}
