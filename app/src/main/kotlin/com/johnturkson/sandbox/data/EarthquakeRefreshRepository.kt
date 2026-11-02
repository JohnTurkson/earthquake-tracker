package com.johnturkson.sandbox.data

import androidx.room.withTransaction
import com.johnturkson.sandbox.annotations.IoDispatcher
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.serialization.JsonConvertException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.SerializationException

@Singleton
class EarthquakeRefreshRepository
@Inject
constructor(
    private val api: EarthquakeApi,
    private val database: MainDatabase,
    private val earthquakes: EarthquakeRepository,
    private val dao: EarthquakeDao,
    private val monitoringDao: EarthquakeMonitoringDao,
    private val connectivity: EarthquakeConnection,
    private val clock: EarthquakeClock,
    private val settings: EarthquakeSettingsRepository,
    notifications: EarthquakeNotifications,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : EarthquakeRefresher {
    private val alerts =
        EarthquakeAlertDelivery(
            { id -> monitoringDao.getDeliveredAlertsFlow(id).first().isNotEmpty() },
            notifications::postAlert,
            monitoringDao::recordAlert,
        )

    fun getRefreshFlow() = monitoringDao.getRefreshFlow()

    override suspend fun refresh(): EarthquakeRefreshResult =
        withContext(ioDispatcher) {
            if (!connectivity.isOnline()) return@withContext EarthquakeRefreshResult.Offline
            val items =
                try {
                    val response =
                        withTimeoutOrNull(30_000) { api.getEarthquakes(EarthquakeFeed.ALL_DAY) }
                            ?: throw IOException("Earthquake refresh timed out")
                    require(response.type == "FeatureCollection" && response.metadata.status == 200)
                    response.features.map { it.toDatabaseModel() }
                } catch (error: CancellationException) {
                    throw error
                } catch (_: JsonConvertException) {
                    return@withContext fetchFailure(EarthquakeRefreshResult.DataError)
                } catch (_: NoTransformationFoundException) {
                    return@withContext fetchFailure(EarthquakeRefreshResult.DataError)
                } catch (_: SerializationException) {
                    return@withContext fetchFailure(EarthquakeRefreshResult.DataError)
                } catch (_: IllegalArgumentException) {
                    return@withContext fetchFailure(EarthquakeRefreshResult.DataError)
                } catch (_: Exception) {
                    return@withContext fetchFailure(EarthquakeRefreshResult.NetworkError)
                }
            val savedAt = clock.nowMillis()
            try {
                database.withTransaction {
                    dao.mergeEarthquakes(items)
                    monitoringDao.updateRefresh(
                        EarthquakeRefreshDatabaseModel(updatedMillis = savedAt)
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                return@withContext EarthquakeRefreshResult.StorageError
            }
            try {
                publishAlerts()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {}
            EarthquakeRefreshResult.Success
        }

    private fun fetchFailure(result: EarthquakeRefreshResult): EarthquakeRefreshResult =
        if (connectivity.isOnline()) result else EarthquakeRefreshResult.Offline

    private suspend fun publishAlerts() {
        val preferences = settings.settings.first()
        if (!preferences.alertsEnabled) return
        val now = clock.nowMillis()
        val recent =
            earthquakes
                .getEarthquakesFlow(now - 60_000, now + 1, preferences.minimumMagnitude.toDouble())
                .first()
        alerts.deliver(recent, preferences, now)
        monitoringDao.pruneAlerts(now - 30L * 24 * 60 * 60 * 1000)
    }
}
