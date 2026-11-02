package com.johnturkson.sandbox

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.cash.turbine.test
import com.johnturkson.sandbox.data.EarthquakeApi
import com.johnturkson.sandbox.data.EarthquakeCatalogResources
import com.johnturkson.sandbox.data.EarthquakeCollectionNetworkModel
import com.johnturkson.sandbox.data.EarthquakeDatabaseModel
import com.johnturkson.sandbox.data.EarthquakeDetailDatabaseModel
import com.johnturkson.sandbox.data.EarthquakeGeometryNetworkModel
import com.johnturkson.sandbox.data.EarthquakeNetworkModel
import com.johnturkson.sandbox.data.EarthquakePropertiesNetworkModel
import com.johnturkson.sandbox.data.EarthquakeRepository
import com.johnturkson.sandbox.data.MainDatabase
import com.johnturkson.sandbox.data.toDatabaseModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ServerResponseException
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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
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
    private lateinit var database: MainDatabase

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(context, MainDatabase::class.java).build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun insertsRevisesAndIgnoresOlderAndEqualResponses() = runTest {
        val dao = database.earthquakeDao()
        dao.mergeEarthquakes(listOf(earthquake(updated = 100), earthquake(id = "other")))
        val revised = earthquake(updated = 200, magnitude = 5.5)
        dao.mergeEarthquakes(listOf(revised))
        dao.mergeEarthquakes(listOf(earthquake(updated = 150), earthquake(updated = 200)))
        assertEquals(revised, dao.getEarthquakeFlow("test").first())
        assertEquals(2, dao.getEarthquakesFlow().first().size)
    }

    @Test
    fun queriesByAliasTimeAndMagnitudeWithDeterministicOrdering() = runTest {
        val dao = database.earthquakeDao()
        dao.mergeEarthquakes(
            listOf(
                earthquake(id = "b", time = 200),
                earthquake(id = "a", time = 200, aliases = ",a,alias-a,"),
                earthquake(id = "old", time = 100),
                earthquake(id = "unknown", time = 250, magnitude = null),
            )
        )
        assertEquals("a", dao.getEarthquakeFlow("alias-a").first()!!.id)
        assertNull(dao.getEarthquakeFlow("missing").first())
        assertEquals(
            listOf("unknown", "a", "b", "old"),
            dao.getEarthquakesFlow().first().map { it.id },
        )
        assertEquals(listOf("a", "b"), dao.getEarthquakesFlow(200, 250).first().map { it.id })
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
            dao.mergeEarthquakes(listOf(earthquake(updated = 100)))
            assertEquals(100L, awaitItem()!!.updated)
            dao.mergeEarthquakes(listOf(earthquake(updated = 200)))
            assertEquals(200L, awaitItem()!!.updated)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun filteredListFlowEmitsAsRevisionsEnterAndLeaveTheQuery() = runTest {
        val dao = database.earthquakeDao()
        dao.getEarthquakesFlow(startTime = 100, endTime = 300, minimumMagnitude = 4.0).test {
            assertEquals(emptyList(), awaitItem())
            dao.mergeEarthquakes(listOf(earthquake(updated = 100, time = 200)))
            assertEquals(listOf("test"), awaitItem().map { it.id })
            dao.mergeEarthquakes(listOf(earthquake(updated = 200, time = 200, magnitude = 3.0)))
            assertEquals(emptyList(), awaitItem())
            dao.mergeEarthquakes(listOf(earthquake(updated = 300, time = 200, magnitude = 5.0)))
            assertEquals(5.0, awaitItem().single().magnitude)
            dao.mergeEarthquakes(listOf(earthquake(updated = 400, time = 300)))
            assertEquals(emptyList(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun repositoryDetailFlowEmitsSummaryProductsAndPreferredIdChanges() = runTest {
        var response = "earthquakes-summary.json"
        client { respondFixture(response) }
            .use { client ->
                val repository = repository(client)
                repository.getEarthquakeDetailFlow("alias-test").test {
                    assertNull(awaitItem())
                    repository.refreshEarthquakes()
                    val summary = awaitItem()!!
                    assertEquals("us-test", summary.earthquake.id)
                    assertNull(summary.products)
                    assertFalse(summary.isDetailCurrent)
                    response = "earthquake-detail.json"
                    repository.refreshEarthquake("us-test")
                    val detail = awaitItem()!!
                    assertTrue(detail.products!!.containsKey("origin"))
                    assertTrue(detail.isDetailCurrent)
                    database
                        .earthquakeDao()
                        .mergeEarthquakes(
                            listOf(
                                detail.earthquake.copy(
                                    id = "new-preferred-id",
                                    ids = ",new-preferred-id,us-test,alias-test,",
                                    updated = detail.earthquake.updated + 1,
                                )
                            )
                        )
                    val revised = awaitItem()!!
                    assertEquals("new-preferred-id", revised.earthquake.id)
                    assertEquals(detail.products, revised.products)
                    assertFalse(revised.isDetailCurrent)
                    cancelAndIgnoreRemainingEvents()
                }
            }
    }

    @Test
    fun preservesDetailAcrossSummaryRefreshesAndRejectsStaleDetail() = runTest {
        val dao = database.earthquakeDao()
        val original = earthquake(updated = 100)
        dao.mergeEarthquake(original, EarthquakeDetailDatabaseModel("test", 100, "{}"))
        dao.mergeEarthquakes(listOf(earthquake(updated = 200)))
        dao.mergeEarthquake(
            earthquake(updated = 50),
            EarthquakeDetailDatabaseModel("test", 50, "{\"stale\":[]}"),
        )
        val cached = dao.getEarthquakeDetailFlow("test").first()!!
        assertEquals(200L, cached.earthquake.updated)
        assertEquals(100L, cached.detail!!.updated)
        assertEquals("{}", cached.detail.productsJson)
        dao.mergeEarthquake(
            earthquake(updated = 200),
            EarthquakeDetailDatabaseModel("test", 200, "{\"new\":[]}"),
        )
        assertEquals(200L, dao.getEarthquakeDetailFlow("test").first()!!.detail!!.updated)
    }

    @Test
    fun mergesAssociatedIdsAndPreservesAliasesAndDetailWhenPreferredIdChanges() = runTest {
        val dao = database.earthquakeDao()
        dao.mergeEarthquake(
            earthquake(id = "old", updated = 100, aliases = ",old,old-alias,"),
            EarthquakeDetailDatabaseModel("old", 100, "{}"),
        )
        dao.mergeEarthquakes(listOf(earthquake(id = "new", updated = 200, aliases = ",new,old,")))
        dao.mergeEarthquakes(listOf(earthquake(id = "old", updated = 50)))
        assertEquals(listOf("new"), dao.getEarthquakesFlow().first().map { it.id })
        for (id in listOf("old", "new", "old-alias")) {
            assertEquals("new", dao.getEarthquakeFlow(id).first()!!.id)
            assertEquals("{}", dao.getEarthquakeDetailFlow(id).first()!!.detail!!.productsJson)
        }
    }

    @Test
    fun collapsesPreviouslySeparateRowsWhenUsgsAssociatesThem() = runTest {
        val dao = database.earthquakeDao()
        dao.mergeEarthquakes(
            listOf(
                earthquake(id = "first", updated = 100),
                earthquake(id = "second", updated = 200),
            )
        )
        dao.mergeEarthquake(
            earthquake(id = "second", updated = 200),
            EarthquakeDetailDatabaseModel("second", 200, "{}"),
        )
        dao.mergeEarthquakes(
            listOf(earthquake(id = "first", updated = 300, aliases = ",first,second,"))
        )
        assertEquals(1, dao.getEarthquakesFlow().first().size)
        assertEquals("first", dao.getEarthquakeFlow("second").first()!!.id)
        assertEquals(200L, dao.getEarthquakeDetailFlow("first").first()!!.detail!!.updated)
    }

    @Test
    fun concurrentRefreshesCannotReplaceNewerDataWithOlderData() = runTest {
        val dao = database.earthquakeDao()
        listOf(200L, 100L, 400L, 300L)
            .map { revision ->
                async(Dispatchers.Default) {
                    dao.mergeEarthquakes(listOf(earthquake(updated = revision)))
                }
            }
            .awaitAll()
        assertEquals(400L, dao.getEarthquakeFlow("test").first()!!.updated)
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
    fun repositoryRefreshesSummaryAndDetailAndKeepsCacheOnNetworkFailure() = runTest {
        var response = "earthquakes-summary.json"
        var unavailable = false
        client {
            if (unavailable) respond("unavailable", HttpStatusCode.ServiceUnavailable)
            else respondFixture(response)
        }
            .use { client ->
                val repository = repository(client)
                repository.refreshEarthquakes()
                assertEquals(2, repository.getEarthquakesFlow().first().size)
                response = "earthquake-detail.json"
                repository.refreshEarthquake("us-test")
                val detail = repository.getEarthquakeDetailFlow("alias-test").first()!!
                assertTrue(detail.isDetailCurrent)
                assertTrue(detail.products!!.containsKey("origin"))
                assertTrue(detail.earthquake.detail!!.endsWith("us-test.geojson"))
                response = "earthquakes-empty.json"
                repository.refreshEarthquakes()
                assertEquals(2, repository.getEarthquakesFlow().first().size)
                unavailable = true
                assertFailsWith<ServerResponseException> { repository.refreshEarthquakes() }
                assertEquals(detail, repository.getEarthquakeDetailFlow("us-test").first())
            }
    }

    @Test
    fun repositoryMarksPreservedDetailAsStaleAfterNewerSummaryArrives() = runTest {
        client { respondFixture("earthquake-detail.json") }
            .use { client ->
                val repository = repository(client)
                repository.refreshEarthquake("us-test")
                val stored = repository.getEarthquakeFlow("us-test").first()!!
                database
                    .earthquakeDao()
                    .mergeEarthquakes(listOf(stored.copy(updated = stored.updated + 1)))
                val detail = repository.getEarthquakeDetailFlow("us-test").first()!!
                assertFalse(detail.isDetailCurrent)
                assertTrue(detail.products!!.isNotEmpty())
            }
    }

    @Test
    fun repositoryDoesNotWritePartOfAnInvalidResponseOrEraseCacheOnNoContent() = runTest {
        val collection = json.parseToJsonElement(fixture("earthquakes-summary.json")).jsonObject
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
                assertFailsWith<IllegalArgumentException> {
                    repository(client).refreshEarthquakes()
                }
                assertEquals(
                    listOf("test"),
                    database.earthquakeDao().getEarthquakesFlow().first().map { it.id },
                )
            }
        client { respond("", HttpStatusCode.NoContent) }
            .use { client ->
                repository(client).refreshQuery(EarthquakeCatalogResources.Query())
                assertEquals(1, database.earthquakeDao().getEarthquakesFlow().first().size)
            }
    }

    @Test
    fun historicalAliasesRetainAllObservedEventsThroughOfflineRefreshWhileCollecting() = runTest {
        val collection =
            json.decodeFromString<EarthquakeCollectionNetworkModel>(
                fixture("earthquakes-summary.json")
            )
        val original =
            collection.features.first().let {
                it.copy(
                    id = "usc0001xgp",
                    properties =
                        it.properties.copy(
                            ids = ",usc0001xgp,",
                            time = 1299822384120,
                            updated = 100,
                        ),
                )
            }
        val priorEvent =
            original.copy(
                id = "ci38457511",
                properties = original.properties.copy(ids = ",ci38457511,", time = 1562383193040),
            )
        val revised =
            original.copy(
                id = "official20110311054624120_30",
                properties =
                    original.properties.copy(
                        ids = ",usc0001xgp,official20110311054624120_30,usp000hvnu,",
                        updated = 200,
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
                val repository = repository(client)
                repository.getEarthquakesFlow().distinctUntilChanged().test {
                    assertEquals(emptyList(), awaitItem())
                    repository.refreshEarthquakes()
                    assertEquals(listOf(priorEvent.id, original.id), awaitItem().map { it.id })
                    response = collection.copy(features = listOf(revised, original, revised))
                    repository.refreshEarthquakes()
                    val history = awaitItem()
                    assertEquals(listOf(priorEvent.id, revised.id), history.map { it.id })
                    for (id in listOf(original.id, revised.id, "usp000hvnu")) {
                        assertEquals(
                            revised.toDatabaseModel(),
                            repository.getEarthquakeFlow(id).first(),
                        )
                    }
                    offline = true
                    assertFailsWith<IOException> { repository.refreshEarthquakes() }
                    assertEquals(history, repository.getEarthquakesFlow().first())
                    val newest =
                        priorEvent.copy(
                            id = "new-event",
                            properties =
                                priorEvent.properties.copy(
                                    ids = ",new-event,",
                                    time = priorEvent.properties.time + 1,
                                ),
                        )
                    offline = false
                    response = collection.copy(features = listOf(newest))
                    repository.refreshEarthquakes()
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
                fixture("earthquakes-summary.json")
            )
        val original = initial.features.first()
        val revised =
            original.copy(
                properties =
                    original.properties.copy(
                        updated = original.properties.updated + 100,
                        magnitude = 5.5,
                    )
            )
        val added =
            original.copy(
                id = "new-event",
                properties =
                    original.properties.copy(
                        ids = ",new-event,",
                        time = original.properties.time + 1000,
                    ),
            )
        var response = initial
        var networkRequests = 0
        try {
            database.close()
            database = Room.databaseBuilder(context, MainDatabase::class.java, name).build()
            client {
                networkRequests++
                respond(
                    json.encodeToString(response),
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
                .use { client ->
                    val repository = repository(client)
                    repository.getEarthquakesFlow().test {
                        assertEquals(emptyList(), awaitItem())
                        repository.refreshEarthquakes()
                        assertEquals(2, awaitItem().size)
                        response = initial.copy(features = listOf(revised, added, original, added))
                        repository.refreshEarthquakes()
                        val retained = awaitItem()
                        assertEquals(
                            listOf("new-event", "us-test", "us-incomplete"),
                            retained.map { it.id },
                        )
                        assertEquals(5.5, retained.single { it.id == "us-test" }.magnitude)
                        response = initial.copy(features = emptyList())
                        repository.refreshEarthquakes()
                        assertEquals(retained, repository.getEarthquakesFlow().first())
                        cancelAndIgnoreRemainingEvents()
                    }
                }
            assertEquals(3, networkRequests)
            database.close()
            database = Room.databaseBuilder(context, MainDatabase::class.java, name).build()
            client {
                networkRequests++
                throw IOException("offline after restart")
            }
                .use { client ->
                    val repository = repository(client)
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
                    assertFailsWith<IOException> { repository.refreshEarthquakes() }
                    assertEquals(retained, repository.getEarthquakesFlow().first())
                }
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun readsPersistedSummaryAndProductsAfterReopeningWithoutNetwork() = runTest {
        val name = "earthquake-offline-test.db"
        context.deleteDatabase(name)
        try {
            database.close()
            database = Room.databaseBuilder(context, MainDatabase::class.java, name).build()
            client { respondFixture("earthquake-detail.json") }
                .use { client -> repository(client).refreshEarthquake("us-test") }
            database.close()
            database = Room.databaseBuilder(context, MainDatabase::class.java, name).build()
            client { throw IOException("offline") }
                .use { client ->
                    val repository = repository(client)
                    assertEquals("us-test", repository.getEarthquakeFlow("alias-test").first()!!.id)
                    assertTrue(
                        repository
                            .getEarthquakeDetailFlow("us-test")
                            .first()!!
                            .products!!
                            .containsKey("origin")
                    )
                    assertFailsWith<IOException> { repository.refreshEarthquake("us-test") }
                    assertEquals(1, repository.getEarthquakesFlow().first().size)
                }
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    private fun earthquake(
        id: String = "test",
        updated: Long = 100,
        time: Long = 100,
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

    private fun repository(client: HttpClient): EarthquakeRepository {
        return EarthquakeRepository(
            EarthquakeApi(client),
            database.earthquakeDao(),
            json,
            Dispatchers.IO,
        )
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
            fixture(name),
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )
    }

    private fun fixture(name: String): String {
        return InstrumentationRegistry.getInstrumentation()
            .context
            .assets
            .open(name)
            .bufferedReader()
            .use { it.readText() }
    }
}
