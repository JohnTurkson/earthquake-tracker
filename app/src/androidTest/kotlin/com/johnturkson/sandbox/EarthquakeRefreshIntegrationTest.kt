package com.johnturkson.sandbox

import android.app.NotificationManager
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.johnturkson.sandbox.data.EarthquakeApi
import com.johnturkson.sandbox.data.EarthquakeCollectionNetworkModel
import com.johnturkson.sandbox.data.EarthquakeDatabase
import com.johnturkson.sandbox.data.EarthquakeGeometryNetworkModel
import com.johnturkson.sandbox.data.EarthquakeMetadataNetworkModel
import com.johnturkson.sandbox.data.EarthquakeNetworkModel
import com.johnturkson.sandbox.data.EarthquakeNotifications
import com.johnturkson.sandbox.data.EarthquakePropertiesNetworkModel
import com.johnturkson.sandbox.data.EarthquakeRefreshRepository
import com.johnturkson.sandbox.data.EarthquakeRepository
import com.johnturkson.sandbox.data.EarthquakeSettingsRepository
import com.johnturkson.sandbox.work.EarthquakeRefreshWorker
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.resources.Resources
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import org.junit.Test

class EarthquakeRefreshIntegrationTest {
    @Test
    fun workerMergesHistoryDeduplicatesAliasesAndRetainsDataOnFailure() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val systemNotifications = context.getSystemService(NotificationManager::class.java)
        val id = "refresh-test-${UUID.randomUUID()}"
        val preferredId = "$id-preferred"
        val now = Clock.System.now()
        val json = Json { ignoreUnknownKeys = true }
        var requests = 0
        fun event(preferred: Boolean) =
            EarthquakeNetworkModel(
                if (preferred) {
                    preferredId
                } else {
                    id
                },
                "Feature",
                EarthquakePropertiesNetworkModel(
                    now,
                    now +
                        if (preferred) {
                            1.milliseconds
                        } else {
                            Duration.ZERO
                        },
                    6.2,
                    place = "Test Pacific",
                    ids =
                        if (preferred) {
                            ",$preferredId,$id-shared,"
                        } else {
                            ",$id,$id-shared,"
                        },
                    type = "earthquake",
                ),
                EarthquakeGeometryNetworkModel("Point", listOf(2.0, 1.0, 10.0)),
            )
        val client =
            HttpClient(
                MockEngine { request ->
                    assertEquals(
                        "/earthquakes/feed/v1.0/summary/all_day.geojson",
                        request.url.encodedPath,
                    )
                    requests++
                    if (requests == 3) {
                        throw IOException("Earthquake feed unavailable")
                    }
                    val response =
                        EarthquakeCollectionNetworkModel(
                            "FeatureCollection",
                            EarthquakeMetadataNetworkModel(
                                now,
                                "https://earthquake.usgs.gov/",
                                "Test feed",
                                200,
                                "1.0",
                                1,
                            ),
                            listOf(event(requests == 2)),
                        )
                    respond(
                        json.encodeToString(response),
                        headers =
                            headersOf(
                                HttpHeaders.ContentType,
                                ContentType.Application.Json.toString(),
                            ),
                    )
                }
            ) {
                defaultRequest { url("https://earthquake.usgs.gov/") }
                install(Resources)
                install(ContentNegotiation) { json(json) }
            }
        val database = Room.inMemoryDatabaseBuilder(context, EarthquakeDatabase::class.java).build()
        val storeJob = SupervisorJob()
        val file = File(context.cacheDir, "$id.preferences_pb")
        val store =
            PreferenceDataStoreFactory.create(
                scope = CoroutineScope(storeJob + Dispatchers.IO),
                produceFile = { file },
            )
        try {
            val settings = EarthquakeSettingsRepository(store)
            settings.setAlertsEnabled(true)
            val notifications = EarthquakeNotifications(context)
            val capability = notifications.capabilities()
            val allowed =
                capability.permissionGranted && capability.appEnabled && capability.alertsEnabled
            val api = EarthquakeApi(client)
            val repository =
                EarthquakeRepository(database.earthquakeDao(), database.monitoringDao())
            val refresher =
                EarthquakeRefreshRepository(
                    api,
                    database,
                    database.earthquakeDao(),
                    database.monitoringDao(),
                    settings,
                    notifications,
                    Dispatchers.IO,
                )
            val old = EarthquakeMonitoringFixtures.item(id = "$id-history", time = now - 2.days)
            database.earthquakeDao().mergeEarthquakes(listOf(old))
            val factory =
                object : WorkerFactory() {
                    override fun createWorker(
                        appContext: android.content.Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters,
                    ): ListenableWorker? =
                        if (workerClassName == EarthquakeRefreshWorker::class.java.name) {
                            EarthquakeRefreshWorker(
                                appContext,
                                workerParameters,
                                refresher,
                                EarthquakeMonitoringFixtures.connection(),
                            )
                        } else {
                            null
                        }
                }
            suspend fun runWorker() =
                TestListenableWorkerBuilder<EarthquakeRefreshWorker>(context)
                    .setWorkerFactory(factory)
                    .build()
                    .doWork()
            assertEquals(0, requests)
            repeat(2) { assertEquals(ListenableWorker.Result.success(), runWorker()) }
            val stored = repository.getEarthquakesFlow().first()
            assertEquals(setOf(old.id, preferredId), stored.map { it.id }.toSet())
            val metadata = assertNotNull(database.monitoringDao().getRefreshFlow().first())
            assertEquals(
                if (allowed) {
                    1
                } else {
                    0
                },
                database.monitoringDao().getDeliveredAlertsFlow(preferredId).first().size,
            )
            withTimeout(5.seconds) {
                while (
                    systemNotifications.activeNotifications.count {
                        it.tag == id || it.tag == preferredId
                    } !=
                        if (allowed) {
                            1
                        } else {
                            0
                        }
                ) delay(50.milliseconds)
            }
            assertFailsWith<IOException> { runWorker() }
            assertEquals(stored, repository.getEarthquakesFlow().first())
            assertEquals(metadata, database.monitoringDao().getRefreshFlow().first())
        } finally {
            systemNotifications.cancel(id, EarthquakeNotifications.AlertId)
            systemNotifications.cancel(preferredId, EarthquakeNotifications.AlertId)
            storeJob.cancelAndJoin()
            file.delete()
            client.close()
            database.close()
        }
    }
}
