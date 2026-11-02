package com.johnturkson.sandbox.data

import androidx.room.withTransaction
import com.johnturkson.sandbox.annotations.IoDispatcher
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

@Singleton
class EarthquakeRefreshRepository
@Inject
constructor(
    private val api: EarthquakeApi,
    private val database: MainDatabase,
    private val dao: EarthquakeDao,
    private val monitoringDao: EarthquakeMonitoringDao,
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

    override suspend fun refresh() =
        withContext(ioDispatcher) {
            val response =
                withTimeoutOrNull(30.seconds) { api.getEarthquakes(EarthquakeFeed.ALL_DAY) }
                    ?: throw IOException("Earthquake refresh timed out")
            require(response.type == "FeatureCollection" && response.metadata.status == 200)
            val items = response.features.map { it.toDatabaseModel() }
            val savedAt = Clock.System.now()
            database.withTransaction {
                dao.mergeEarthquakes(items)
                monitoringDao.updateRefresh(EarthquakeRefreshDatabaseModel(updatedAt = savedAt))
            }
        }

    override suspend fun publishAlerts() =
        withContext(ioDispatcher) {
            val preferences = settings.settings.first()
            if (!preferences.alertsEnabled) return@withContext
            val now = Clock.System.now()
            val recent =
                dao.getEarthquakesFlow(
                        now - 1.minutes,
                        now + 1.nanoseconds,
                        preferences.minimumMagnitude.toDouble(),
                    )
                    .first()
            alerts.deliver(recent, preferences, now)
            monitoringDao.pruneAlerts(now - 30.days)
        }
}
