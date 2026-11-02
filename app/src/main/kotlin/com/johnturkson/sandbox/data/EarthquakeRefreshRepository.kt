package com.johnturkson.sandbox.data

import androidx.room.withTransaction
import com.johnturkson.sandbox.annotations.IoDispatcher
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

@Singleton
class EarthquakeRefreshRepository
@Inject
constructor(
    api: EarthquakeApi,
    private val database: MainDatabase,
    private val earthquakes: EarthquakeRepository,
    private val dao: EarthquakeDao,
    private val monitoringDao: EarthquakeMonitoringDao,
    connectivity: EarthquakeConnection,
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
    private val coordinator =
        EarthquakeRefreshCoordinator(
            isOnline = connectivity::isOnline,
            fetch = {
                val response =
                    withTimeoutOrNull(30_000) { api.getEarthquakes(EarthquakeFeed.ALL_DAY) }
                        ?: throw IOException("Earthquake refresh timed out")
                require(response.type == "FeatureCollection" && response.metadata.status == 200)
                response.features.map { it.toDatabaseModel() }
            },
            save = { items, savedAt ->
                database.withTransaction {
                    dao.mergeEarthquakes(items)
                    monitoringDao.updateRefresh(
                        EarthquakeRefreshDatabaseModel(updatedMillis = savedAt)
                    )
                }
            },
            nowMillis = clock::nowMillis,
            afterSave = { _, _ -> publishAlerts() },
        )
    val refreshStatus = coordinator.status

    fun getRefreshFlow() = monitoringDao.getRefreshFlow()

    override suspend fun refresh(): EarthquakeRefreshCoordinator.Result =
        withContext(ioDispatcher) { coordinator.refresh() }

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
