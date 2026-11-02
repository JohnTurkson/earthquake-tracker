package com.johnturkson.sandbox

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import app.cash.turbine.test
import com.johnturkson.sandbox.data.EarthquakeApi
import com.johnturkson.sandbox.data.EarthquakeCollectionNetworkModel
import com.johnturkson.sandbox.data.EarthquakeDatabase
import com.johnturkson.sandbox.data.EarthquakeDatabaseModel
import com.johnturkson.sandbox.data.EarthquakeGeometryNetworkModel
import com.johnturkson.sandbox.data.EarthquakeNetworkModel
import com.johnturkson.sandbox.data.EarthquakeNotifications
import com.johnturkson.sandbox.data.EarthquakePropertiesNetworkModel
import com.johnturkson.sandbox.data.EarthquakeRefreshRepository
import com.johnturkson.sandbox.data.EarthquakeRepository
import com.johnturkson.sandbox.data.EarthquakeSettingsRepository
import com.johnturkson.sandbox.data.toDatabaseModel
import com.johnturkson.sandbox.work.EarthquakeRefreshWorker
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.resources.Resources
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import java.io.IOException
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EarthquakeStorageTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var database: EarthquakeDatabase

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(context, EarthquakeDatabase::class.java).build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun insertsRevisesAndIgnoresOlderAndEqualResponses() = runTest {
        val dao = database.earthquakeDao()
        dao.mergeEarthquakes(
            listOf(
                earthquake(updated = Instant.fromEpochMilliseconds(100)),
                earthquake(id = "other"),
            )
        )
        val revised = earthquake(updated = Instant.fromEpochMilliseconds(200), magnitude = 5.5)
        dao.mergeEarthquakes(listOf(revised))
        dao.mergeEarthquakes(
            listOf(
                earthquake(updated = Instant.fromEpochMilliseconds(150)),
                earthquake(updated = Instant.fromEpochMilliseconds(200)),
            )
        )
        assertEquals(revised, dao.getEarthquakeFlow("test").first())
        assertEquals(2, dao.getEarthquakesFlow().first().size)
    }

    @Test
    fun textTimestampsPreserveNanosecondsAndHalfOpenQueries() = runTest {
        val dao = database.earthquakeDao()
        val start = Instant.parse("2026-11-01T00:00:00Z")
        val events =
            listOf(
                earthquake(id = "before", time = start - 1.nanoseconds),
                earthquake(id = "at", time = start),
                earthquake(id = "after", time = start + 1.nanoseconds),
            )
        dao.mergeEarthquakes(events)
        assertEquals(
            listOf("after", "at", "before"),
            dao.getEarthquakesFlow().first().map { it.id },
        )
        assertEquals(
            listOf(events[1]),
            dao.getEarthquakesFlow(start, start + 1.nanoseconds).first(),
        )
        assertEquals(listOf(events[2]), dao.getEarthquakesFlow(start + 1.nanoseconds).first())
        val revised = events[1].copy(updated = events[1].updated + 1.nanoseconds, magnitude = 5.5)
        dao.mergeEarthquakes(listOf(revised))
        dao.mergeEarthquakes(listOf(events[1]))
        assertEquals(revised, dao.getEarthquakeFlow("at").first())
    }

    @Test
    fun queriesByAliasTimeAndMagnitudeWithDeterministicOrdering() = runTest {
        val dao = database.earthquakeDao()
        dao.mergeEarthquakes(
            listOf(
                earthquake(id = "b", time = Instant.fromEpochMilliseconds(200)),
                earthquake(
                    id = "a",
                    time = Instant.fromEpochMilliseconds(200),
                    aliases = ",a,alias-a,",
                ),
                earthquake(id = "old", time = Instant.fromEpochMilliseconds(100)),
                earthquake(
                    id = "unknown",
                    time = Instant.fromEpochMilliseconds(250),
                    magnitude = null,
                ),
            )
        )
        assertEquals("a", dao.getEarthquakeFlow("alias-a").first()!!.id)
        assertNull(dao.getEarthquakeFlow("missing").first())
        assertEquals(
            listOf("unknown", "a", "b", "old"),
            dao.getEarthquakesFlow().first().map { it.id },
        )
        assertEquals(
            listOf("a", "b"),
            dao.getEarthquakesFlow(
                    Instant.fromEpochMilliseconds(200),
                    Instant.fromEpochMilliseconds(250),
                )
                .first()
                .map { it.id },
        )
        assertEquals(
            listOf("a", "b", "old"),
            dao.getEarthquakesFlow(minimumMagnitude = 4.0).first().map { it.id },
        )
    }

    @Test
    fun emitsDatabaseChangesThroughFlows() = runTest {
        val dao = database.earthquakeDao()
        dao.getEarthquakeFlow("test").test {
            assertNull(awaitItem())
            dao.mergeEarthquakes(listOf(earthquake(updated = Instant.fromEpochMilliseconds(100))))
            assertEquals(Instant.fromEpochMilliseconds(100L), awaitItem()!!.updated)
            dao.mergeEarthquakes(listOf(earthquake(updated = Instant.fromEpochMilliseconds(200))))
            assertEquals(Instant.fromEpochMilliseconds(200L), awaitItem()!!.updated)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun filteredListFlowEmitsAsRevisionsEnterAndLeaveTheQuery() = runTest {
        val dao = database.earthquakeDao()
        dao.getEarthquakesFlow(
                startTime = Instant.fromEpochMilliseconds(100),
                endTime = Instant.fromEpochMilliseconds(300),
                minimumMagnitude = 4.0,
            )
            .test {
                assertEquals(emptyList(), awaitItem())
                dao.mergeEarthquakes(
                    listOf(
                        earthquake(
                            updated = Instant.fromEpochMilliseconds(100),
                            time = Instant.fromEpochMilliseconds(200),
                        )
                    )
                )
                assertEquals(listOf("test"), awaitItem().map { it.id })
                dao.mergeEarthquakes(
                    listOf(
                        earthquake(
                            updated = Instant.fromEpochMilliseconds(200),
                            time = Instant.fromEpochMilliseconds(200),
                            magnitude = 3.0,
                        )
                    )
                )
                assertEquals(emptyList(), awaitItem())
                dao.mergeEarthquakes(
                    listOf(
                        earthquake(
                            updated = Instant.fromEpochMilliseconds(300),
                            time = Instant.fromEpochMilliseconds(200),
                            magnitude = 5.0,
                        )
                    )
                )
                assertEquals(5.0, awaitItem().single().magnitude)
                dao.mergeEarthquakes(
                    listOf(
                        earthquake(
                            updated = Instant.fromEpochMilliseconds(400),
                            time = Instant.fromEpochMilliseconds(300),
                        )
                    )
                )
                assertEquals(emptyList(), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
    }

    @Test
    fun repositoryFlowEmitsSummaryAndPreferredIdChanges() = runTest {
        val repository = repository()
        repository.getEarthquakeFlow("old-alias").test {
            assertNull(awaitItem())
            database
                .earthquakeDao()
                .mergeEarthquakes(listOf(earthquake(id = "old", aliases = ",old,old-alias,")))
            assertEquals("old", awaitItem()!!.id)
            database
                .earthquakeDao()
                .mergeEarthquakes(
                    listOf(
                        earthquake(
                            id = "new",
                            updated = Instant.fromEpochMilliseconds(200),
                            aliases = ",new,old,",
                        )
                    )
                )
            assertEquals("new", awaitItem()!!.id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun mergesAssociatedIdsAndPreservesAliasesWhenPreferredIdChanges() = runTest {
        val dao = database.earthquakeDao()
        dao.mergeEarthquake(
            earthquake(
                id = "old",
                updated = Instant.fromEpochMilliseconds(100),
                aliases = ",old,old-alias,",
            )
        )
        dao.mergeEarthquakes(
            listOf(
                earthquake(
                    id = "new",
                    updated = Instant.fromEpochMilliseconds(200),
                    aliases = ",new,old,",
                )
            )
        )
        dao.mergeEarthquakes(
            listOf(earthquake(id = "old", updated = Instant.fromEpochMilliseconds(50)))
        )
        assertEquals(listOf("new"), dao.getEarthquakesFlow().first().map { it.id })
        for (id in listOf("old", "new", "old-alias")) {
            assertEquals("new", dao.getEarthquakeFlow(id).first()!!.id)
        }
    }

    @Test
    fun collapsesPreviouslySeparateRowsWhenSourceAssociatesThem() = runTest {
        val dao = database.earthquakeDao()
        dao.mergeEarthquakes(
            listOf(
                earthquake(id = "first", updated = Instant.fromEpochMilliseconds(100)),
                earthquake(id = "second", updated = Instant.fromEpochMilliseconds(200)),
            )
        )
        dao.mergeEarthquakes(
            listOf(
                earthquake(
                    id = "first",
                    updated = Instant.fromEpochMilliseconds(300),
                    aliases = ",first,second,",
                )
            )
        )
        assertEquals(1, dao.getEarthquakesFlow().first().size)
        assertEquals("first", dao.getEarthquakeFlow("second").first()!!.id)
    }

    @Test
    fun concurrentRefreshesCannotReplaceNewerDataWithOlderData() = runTest {
        val dao = database.earthquakeDao()
        listOf(200L, 100L, 400L, 300L)
            .map { revision ->
                async(Dispatchers.Default) {
                    dao.mergeEarthquakes(
                        listOf(earthquake(updated = Instant.fromEpochMilliseconds(revision)))
                    )
                }
            }
            .awaitAll()
        assertEquals(
            Instant.fromEpochMilliseconds(400L),
            dao.getEarthquakeFlow("test").first()!!.updated,
        )
    }

    @Test
    fun rollsBackTheWholeBatchWhenAWriteFails() = runTest {
        database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER reject_test BEFORE INSERT ON earthquakes WHEN NEW.id = 'reject' BEGIN SELECT RAISE(ABORT, 'test failure'); END"
        )
        assertFailsWith<android.database.sqlite.SQLiteConstraintException> {
            database
                .earthquakeDao()
                .mergeEarthquakes(listOf(earthquake(), earthquake(id = "reject")))
        }
        assertTrue(database.earthquakeDao().getEarthquakesFlow().first().isEmpty())
    }

    @Test
    fun workerRefreshesSummaryAndKeepsHistoryOnEmptyResponseAndNetworkFailure() = runTest {
        var response = "earthquakes-summary.json"
        var unavailable = false
        client {
            if (unavailable) respond("unavailable", HttpStatusCode.ServiceUnavailable)
            else respondFixture(response)
        }
            .use { client ->
                val repository = repository()
                refresh(client)
                val stored = repository.getEarthquakesFlow().first()
                assertEquals(2, stored.size)
                response = "earthquakes-empty.json"
                refresh(client)
                assertEquals(stored, repository.getEarthquakesFlow().first())
                unavailable = true
                refresh(client, expectedFailure = true)
                assertEquals(stored, repository.getEarthquakesFlow().first())
            }
    }

    @Test
    fun repositoryRefreshMetadataEmitsAfterWorkerCommitAndSurvivesFailure() = runTest {
        var unavailable = false
        client {
            if (unavailable) throw IOException("offline")
            respondFixture("earthquakes-summary.json")
        }
            .use { client ->
                val repository = repository()
                repository.getRefreshFlow().test {
                    assertNull(awaitItem())
                    refresh(client)
                    val saved = awaitItem()!!
                    assertTrue(saved.updatedAt > Instant.fromEpochMilliseconds(0))
                    assertEquals(2, repository.getEarthquakesFlow().first().size)
                    unavailable = true
                    refresh(client, expectedFailure = true)
                    assertEquals(saved, repository.getRefreshFlow().first())
                    expectNoEvents()
                    cancelAndIgnoreRemainingEvents()
                }
            }
    }

    @Test
    fun workerDoesNotWritePartOfAnInvalidResponse() = runTest {
        val collection =
            json
                .parseToJsonElement(EarthquakeJsonFixtures.read("earthquakes-summary.json"))
                .jsonObject
        val features = collection.getValue("features").jsonArray
        val invalid =
            JsonObject(
                features.last().jsonObject +
                    ("geometry" to
                        json.parseToJsonElement("""{"type":"Point","coordinates":[0]}"""))
            )
        val invalidCollection =
            JsonObject(collection + ("features" to JsonArray(listOf(features.first(), invalid))))
        client {
            respond(
                invalidCollection.toString(),
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
            .use { client ->
                database.earthquakeDao().mergeEarthquakes(listOf(earthquake()))
                refresh(client, expectedFailure = true)
                assertEquals(
                    listOf("test"),
                    database.earthquakeDao().getEarthquakesFlow().first().map { it.id },
                )
            }
    }

    @Test
    fun historicalAliasesRetainAllObservedEventsThroughOfflineRefreshWhileCollecting() = runTest {
        val collection =
            json.decodeFromString<EarthquakeCollectionNetworkModel>(
                EarthquakeJsonFixtures.read("earthquakes-summary.json")
            )
        val original =
            collection.features.first().let {
                it.copy(
                    id = "usc0001xgp",
                    properties =
                        it.properties.copy(
                            ids = ",usc0001xgp,",
                            time = Instant.fromEpochMilliseconds(1299822384120),
                            updated = Instant.fromEpochMilliseconds(100),
                        ),
                )
            }
        val priorEvent =
            original.copy(
                id = "ci38457511",
                properties =
                    original.properties.copy(
                        ids = ",ci38457511,",
                        time = Instant.fromEpochMilliseconds(1562383193040),
                    ),
            )
        val revised =
            original.copy(
                id = "official20110311054624120_30",
                properties =
                    original.properties.copy(
                        ids = ",usc0001xgp,official20110311054624120_30,usp000hvnu,",
                        updated = Instant.fromEpochMilliseconds(200),
                        magnitude = 9.1,
                    ),
            )
        var response = collection.copy(features = listOf(original, priorEvent))
        var offline = false
        client {
            if (offline) throw IOException("offline while observing the database")
            respond(
                json.encodeToString(response),
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
            .use { client ->
                val repository = repository()
                repository.getEarthquakesFlow().distinctUntilChanged().test {
                    assertEquals(emptyList(), awaitItem())
                    refresh(client)
                    assertEquals(listOf(priorEvent.id, original.id), awaitItem().map { it.id })
                    response = collection.copy(features = listOf(revised, original, revised))
                    refresh(client)
                    val history = awaitItem()
                    assertEquals(listOf(priorEvent.id, revised.id), history.map { it.id })
                    for (id in listOf(original.id, revised.id, "usp000hvnu")) {
                        assertEquals(
                            revised.toDatabaseModel(),
                            repository.getEarthquakeFlow(id).first(),
                        )
                    }
                    offline = true
                    refresh(client, expectedFailure = true)
                    assertEquals(history, repository.getEarthquakesFlow().first())
                    val newest =
                        priorEvent.copy(
                            id = "new-event",
                            properties =
                                priorEvent.properties.copy(
                                    ids = ",new-event,",
                                    time = priorEvent.properties.time + 1.milliseconds,
                                ),
                        )
                    offline = false
                    response = collection.copy(features = listOf(newest))
                    refresh(client)
                    assertEquals(
                        listOf(newest.id, priorEvent.id, revised.id),
                        awaitItem().map { it.id },
                    )
                    cancelAndIgnoreRemainingEvents()
                }
            }
    }

    @Test
    fun rollingRefreshesRetainDeduplicatedHistoryAfterOfflineReopen() = runTest {
        val name = "earthquake-history-${UUID.randomUUID()}.db"
        val initial =
            json.decodeFromString<EarthquakeCollectionNetworkModel>(
                EarthquakeJsonFixtures.read("earthquakes-summary.json")
            )
        val original = initial.features.first()
        val revised =
            original.copy(
                properties =
                    original.properties.copy(
                        updated = original.properties.updated + 100.milliseconds,
                        magnitude = 5.5,
                    )
            )
        val added =
            original.copy(
                id = "new-event",
                properties =
                    original.properties.copy(
                        ids = ",new-event,",
                        time = original.properties.time + 1000.milliseconds,
                    ),
            )
        var response = initial
        var networkRequests = 0
        try {
            database.close()
            database = Room.databaseBuilder(context, EarthquakeDatabase::class.java, name).build()
            client {
                networkRequests++
                respond(
                    json.encodeToString(response),
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
                .use { client ->
                    val repository = repository()
                    repository.getEarthquakesFlow().test {
                        assertEquals(emptyList(), awaitItem())
                        refresh(client)
                        assertEquals(2, awaitItem().size)
                        response = initial.copy(features = listOf(revised, added, original, added))
                        refresh(client)
                        val retained = awaitItem()
                        assertEquals(
                            listOf("new-event", "us-test", "us-incomplete"),
                            retained.map { it.id },
                        )
                        assertEquals(5.5, retained.single { it.id == "us-test" }.magnitude)
                        response = initial.copy(features = emptyList())
                        refresh(client)
                        assertEquals(retained, repository.getEarthquakesFlow().first())
                        cancelAndIgnoreRemainingEvents()
                    }
                }
            assertEquals(3, networkRequests)
            database.close()
            database = Room.databaseBuilder(context, EarthquakeDatabase::class.java, name).build()
            client {
                networkRequests++
                throw IOException("offline after restart")
            }
                .use { client ->
                    val repository = repository()
                    val retained = repository.getEarthquakesFlow().first()
                    assertEquals(
                        listOf("new-event", "us-test", "us-incomplete"),
                        retained.map { it.id },
                    )
                    assertEquals(
                        5.5,
                        repository.getEarthquakeFlow("alias-test").first()!!.magnitude,
                    )
                    assertEquals(3, networkRequests)
                    refresh(client, expectedFailure = true)
                    assertEquals(retained, repository.getEarthquakesFlow().first())
                }
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    private fun earthquake(
        id: String = "test",
        updated: Instant = Instant.fromEpochMilliseconds(100),
        time: Instant = Instant.fromEpochMilliseconds(100),
        magnitude: Double? = 4.2,
        aliases: String = ",$id,",
    ): EarthquakeDatabaseModel {
        return EarthquakeNetworkModel(
                id = id,
                type = "Feature",
                properties =
                    EarthquakePropertiesNetworkModel(
                        time = time,
                        updated = updated,
                        magnitude = magnitude,
                        ids = aliases,
                    ),
                geometry = EarthquakeGeometryNetworkModel("Point", listOf(-120.0, 40.0, -1.0)),
            )
            .toDatabaseModel()
    }

    private fun repository(): EarthquakeRepository {
        return EarthquakeRepository(database.earthquakeDao(), database.monitoringDao())
    }

    private suspend fun refresh(
        client: HttpClient,
        expectedFailure: Boolean = false,
    ) {
        val settings =
            EarthquakeSettingsRepository(
                object : DataStore<Preferences> {
                    override val data = flowOf(emptyPreferences())

                    override suspend fun updateData(
                        transform: suspend (Preferences) -> Preferences
                    ): Preferences = error("Read-only settings")
                }
            )
        val refresher =
            EarthquakeRefreshRepository(
                EarthquakeApi(client),
                database,
                database.earthquakeDao(),
                database.monitoringDao(),
                settings,
                EarthquakeNotifications(context),
                Dispatchers.IO,
            )
        val factory =
            object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ): ListenableWorker =
                    EarthquakeRefreshWorker(
                        appContext,
                        workerParameters,
                        refresher,
                        EarthquakeMonitoringFixtures.connection(),
                    )
            }
        val result = runCatching {
            TestListenableWorkerBuilder<EarthquakeRefreshWorker>(context)
                .setWorkerFactory(factory)
                .build()
                .doWork()
        }
        assertEquals(expectedFailure, result.isFailure)
        if (!expectedFailure) assertEquals(ListenableWorker.Result.success(), result.getOrThrow())
    }

    private fun client(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
    ): HttpClient {
        return HttpClient(MockEngine(handler)) {
            defaultRequest { url("https://earthquake.usgs.gov/") }
            install(Resources)
            install(ContentNegotiation) { json(json) }
        }
    }

    private fun MockRequestHandleScope.respondFixture(name: String): HttpResponseData {
        return respond(
            EarthquakeJsonFixtures.read(name),
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )
    }
}
