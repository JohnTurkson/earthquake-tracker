package com.johnturkson.sandbox.data

import javax.inject.Inject
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow

class EarthquakeRepository
@Inject
constructor(
    private val dao: EarthquakeDao,
    private val monitoringDao: EarthquakeMonitoringDao,
) {
    fun getEarthquakesFlow(
        startTime: Instant? = null,
        endTime: Instant? = null,
        minimumMagnitude: Double? = null,
    ): Flow<List<EarthquakeDatabaseModel>> {
        return dao.getEarthquakesFlow(startTime, endTime, minimumMagnitude)
    }

    fun getEarthquakeFlow(id: String): Flow<EarthquakeDatabaseModel?> {
        return dao.getEarthquakeFlow(id)
    }

    fun getRefreshFlow(): Flow<EarthquakeRefreshDatabaseModel?> {
        return monitoringDao.getRefreshFlow()
    }
}
