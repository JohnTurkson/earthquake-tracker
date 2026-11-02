package com.johnturkson.sandbox

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.johnturkson.sandbox.data.EarthquakeApi
import com.johnturkson.sandbox.data.EarthquakeConnection
import com.johnturkson.sandbox.data.EarthquakeNotifications
import com.johnturkson.sandbox.data.EarthquakeRefreshDatabaseModel
import com.johnturkson.sandbox.data.EarthquakeRefreshRepository
import com.johnturkson.sandbox.data.EarthquakeRefreshResult
import com.johnturkson.sandbox.data.EarthquakeSettingsRepository
import com.johnturkson.sandbox.data.MainDatabase
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
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import org.junit.Test

class EarthquakeRefreshRepositoryTest {
    @Test
    fun offlineDoesNotFetchOrChangeStoredData() = runBlocking {
        withFixture { fixture ->
            assertEquals(EarthquakeRefreshResult.Offline, fixture.refresher.refresh())
            assertEquals(0, fixture.requests)
            fixture.assertUnchanged()
        }
    }

    @Test
    fun networkDecodeAndConnectionLossPreserveStoredData() = runBlocking {
        withFixture { fixture ->
            fixture.connection.online.value = true
            fixture.response = { throw IOException("Network failure") }
            assertEquals(EarthquakeRefreshResult.NetworkError, fixture.refresher.refresh())
            fixture.assertUnchanged()
            fixture.response = { "invalid JSON" }
            assertEquals(EarthquakeRefreshResult.DataError, fixture.refresher.refresh())
            fixture.assertUnchanged()
            fixture.response = { fixture.feed.replace("FeatureCollection", "Invalid") }
            assertEquals(EarthquakeRefreshResult.DataError, fixture.refresher.refresh())
            fixture.assertUnchanged()
            fixture.response = {
                fixture.connection.online.value = false
                throw IOException("Connection lost")
            }
            assertEquals(EarthquakeRefreshResult.Offline, fixture.refresher.refresh())
            fixture.assertUnchanged()
        }
    }

    @Test
    fun storageFailureRollsBackRowsAndMetadataWithoutAlerting() = runBlocking {
        withFixture { fixture ->
            fixture.connection.online.value = true
            fixture.database.openHelper.writableDatabase.execSQL(
                """
                CREATE TRIGGER reject_refresh BEFORE UPDATE ON earthquake_refresh
                BEGIN SELECT RAISE(ABORT, 'Storage failure'); END
                """
                    .trimIndent()
            )
            assertEquals(EarthquakeRefreshResult.StorageError, fixture.refresher.refresh())
            fixture.assertUnchanged()
            assertEquals(0, fixture.settingsReads)
        }
    }

    @Test
    fun alertFailureHappensAfterCommitAndKeepsSuccessfulRefresh() = runBlocking {
        withFixture { fixture ->
            fixture.connection.online.value = true
            fixture.readSettings = {
                assertEquals(2, fixture.database.earthquakeDao().getEarthquakesFlow().first().size)
                assertTrue(
                    fixture.database.monitoringDao().getRefreshFlow().first()!!.updatedAt >
                        Instant.fromEpochMilliseconds(100)
                )
                throw SecurityException("Notifications unavailable")
            }
            assertEquals(EarthquakeRefreshResult.Success, fixture.refresher.refresh())
            assertEquals(1, fixture.settingsReads)
            assertEquals(2, fixture.database.earthquakeDao().getEarthquakesFlow().first().size)
        }
    }

    @Test
    fun cancellationPropagatesAndNextRefreshCanSucceed() = runBlocking {
        withFixture { fixture ->
            fixture.connection.online.value = true
            val started = CompletableDeferred<Unit>()
            val response = CompletableDeferred<String>()
            fixture.response = {
                started.complete(Unit)
                response.await()
            }
            val pending = async { fixture.refresher.refresh() }
            withTimeout(5_000) { started.await() }
            pending.cancelAndJoin()
            assertTrue(pending.isCancelled)
            fixture.assertUnchanged()
            fixture.response = { fixture.feed }
            assertEquals(EarthquakeRefreshResult.Success, fixture.refresher.refresh())
        }
    }

    private suspend fun withFixture(block: suspend (Fixture) -> Unit) {
        val fixture = Fixture()
        try {
            fixture.database.earthquakeDao().mergeEarthquakes(listOf(fixture.old))
            fixture.database.monitoringDao().updateRefresh(fixture.metadata)
            block(fixture)
        } finally {
            fixture.client.close()
            fixture.database.close()
        }
    }

    private class Fixture {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, MainDatabase::class.java).build()
        val old =
            EarthquakeMonitoringFixtures.item(id = "old", time = Instant.fromEpochMilliseconds(1))
        val metadata =
            EarthquakeRefreshDatabaseModel(updatedAt = Instant.fromEpochMilliseconds(100))
        val feed =
            """
            {"type":"FeatureCollection","metadata":{"generated":200,"url":"https://earthquake.usgs.gov/","title":"Test","status":200,"api":"1.0","count":1},"features":[{"type":"Feature","id":"new","properties":{"time":100,"updated":200,"mag":1.0},"geometry":{"type":"Point","coordinates":[2.0,1.0,10.0]}}]}
            """
                .trimIndent()
        var response: suspend () -> String = { feed }
        var requests = 0
        var settingsReads = 0
        var readSettings: suspend () -> Unit = {}
        val connection = Connection()
        val client =
            HttpClient(
                MockEngine {
                    requests++
                    respond(
                        response(),
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
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            }
        private val settings =
            EarthquakeSettingsRepository(
                object : DataStore<Preferences> {
                    override val data = flow {
                        settingsReads++
                        readSettings()
                        emit(emptyPreferences())
                    }

                    override suspend fun updateData(
                        transform: suspend (Preferences) -> Preferences
                    ): Preferences = error("Read-only fixture")
                }
            )
        val refresher =
            EarthquakeRefreshRepository(
                EarthquakeApi(client),
                database,
                database.earthquakeDao(),
                database.monitoringDao(),
                connection,
                settings,
                EarthquakeNotifications(context),
                Dispatchers.IO,
            )

        suspend fun assertUnchanged() {
            assertEquals(listOf(old), database.earthquakeDao().getEarthquakesFlow().first())
            assertEquals(metadata, database.monitoringDao().getRefreshFlow().first())
        }
    }

    private class Connection : EarthquakeConnection {
        override val online = MutableStateFlow(false)

        override fun isOnline() = online.value
    }
}
