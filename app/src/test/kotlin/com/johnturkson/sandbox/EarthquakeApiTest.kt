package com.johnturkson.sandbox

import com.johnturkson.sandbox.data.EarthquakeApi
import com.johnturkson.sandbox.data.EarthquakeCatalogResources
import com.johnturkson.sandbox.data.EarthquakeFeed
import com.johnturkson.sandbox.data.EarthquakeNetworkModel
import com.johnturkson.sandbox.data.toDatabaseModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.resources.Resources
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.Test

class EarthquakeApiTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun requestsSummaryAndDecodesNullableMeasurementsAndUnknownFields() = runTest {
        client { request ->
            assertEquals(URLProtocol.HTTPS, request.url.protocol)
            assertEquals("earthquake.usgs.gov", request.url.host)
            assertEquals(HttpMethod.Get, request.method)
            assertEquals("/earthquakes/feed/v1.0/summary/all_hour.geojson", request.url.encodedPath)
            respondFixture("earthquakes-summary.json")
        }
            .use { client ->
                val response = EarthquakeApi(client).getEarthquakes()
                assertEquals(2, response.metadata.count)
                assertEquals(
                    Instant.fromEpochMilliseconds(1700000000123L),
                    response.metadata.generated,
                )
                val earthquake = response.features.first().toDatabaseModel()
                assertEquals("us-test", earthquake.id)
                assertEquals(Instant.fromEpochMilliseconds(1699999000123L), earthquake.time)
                assertEquals(Instant.fromEpochMilliseconds(1700000000001L), earthquake.updated)
                assertEquals(-123.5, earthquake.longitude)
                assertEquals(45.5, earthquake.latitude)
                assertEquals(-0.4, earthquake.depth)
                assertEquals(4.2, earthquake.magnitude)
                assertEquals("earthquake", earthquake.type)
                assertEquals(23, earthquake.stationCount)
                assertEquals(271, earthquake.significance)
                val incomplete = response.features.last().toDatabaseModel()
                assertNull(incomplete.magnitude)
                assertNull(incomplete.place)
                assertNull(incomplete.magnitudeType)
            }
    }

    @Test
    fun supportsEveryDocumentedSummaryFeed() = runTest {
        val paths = mutableListOf<String>()
        client { request ->
            paths += request.url.encodedPath.substringAfterLast('/')
            respondFixture("earthquakes-empty.json")
        }
            .use { client ->
                for (feed in EarthquakeFeed.entries) {
                    EarthquakeApi(client).getEarthquakes(feed)
                }
            }
        val expected =
            listOf("hour", "day", "week", "month").flatMap { window ->
                listOf("all", "1.0", "2.5", "4.5", "significant").map { "${it}_$window.geojson" }
            }
        assertEquals(expected, paths)
    }

    @Test
    fun decodesEmptyFeedWithoutBoundingBox() = runTest {
        client { respondFixture("earthquakes-empty.json") }
            .use { client ->
                val response = EarthquakeApi(client).getEarthquakes()
                assertTrue(response.features.isEmpty())
                assertNull(response.bbox)
            }
    }

    @Test
    fun requestsDetailAndIgnoresUnusedProducts() = runTest {
        client { request ->
            assertEquals("/earthquakes/feed/v1.0/detail/us-test.geojson", request.url.encodedPath)
            respondFixture("earthquake-detail.json")
        }
            .use { client ->
                val response = EarthquakeApi(client).getEarthquake("us-test")
                assertNull(response.properties.detail)
                assertEquals("us-test", response.id)
                assertEquals(4.2, response.properties.magnitude)
                assertEquals(
                    Instant.fromEpochMilliseconds(1700000000001L),
                    response.properties.updated,
                )
            }
    }

    @Test
    fun encodesCatalogUpdateFiltersAndPagination() = runTest {
        client { request ->
            assertEquals("/fdsnws/event/1/query", request.url.encodedPath)
            assertEquals("geojson", request.url.parameters["format"])
            assertEquals("2020-01-01T00:00:00Z", request.url.parameters["starttime"])
            assertEquals("2023-11-14T22:13:20Z", request.url.parameters["updatedafter"])
            assertEquals("2023-11-15T00:00:00Z", request.url.parameters["endtime"])
            assertEquals("10", request.url.parameters["limit"])
            assertEquals("11", request.url.parameters["offset"])
            respondFixture("earthquakes-summary.json")
        }
            .use { client ->
                val response =
                    EarthquakeApi(client)
                        .queryEarthquakes(
                            EarthquakeCatalogResources.Query(
                                startTime = Instant.parse("2020-01-01T00:00:00Z"),
                                endTime = Instant.parse("2023-11-15T00:00:00Z"),
                                updatedAfter = Instant.parse("2023-11-14T22:13:20+00:00"),
                                limit = 10,
                                offset = 11,
                            )
                        )
                assertEquals(2, response!!.features.size)
            }
    }

    @Test
    fun handlesCatalogNoContentWithoutDecodingAnEmptyBody() = runTest {
        client { respond("", HttpStatusCode.NoContent) }
            .use { client ->
                assertNull(
                    EarthquakeApi(client).queryEarthquakes(EarthquakeCatalogResources.Query())
                )
            }
    }

    @Test
    fun propagatesNotFoundAndServerFailures() = runTest {
        client { respond("not found", HttpStatusCode.NotFound) }
            .use { client ->
                assertFailsWith<ClientRequestException> {
                    EarthquakeApi(client).getEarthquake("missing")
                }
            }
        client { respond("unavailable", HttpStatusCode.ServiceUnavailable) }
            .use { client ->
                assertFailsWith<ServerResponseException> { EarthquakeApi(client).getEarthquakes() }
            }
    }

    @Test
    fun rejectsMalformedJson() = runTest {
        client {
            respond("{", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
            .use { client ->
                assertFailsWith<io.ktor.serialization.JsonConvertException> {
                    EarthquakeApi(client).getEarthquakes()
                }
            }
    }

    @Test
    fun requiresIdentityAndRevisionFields() {
        assertFailsWith<SerializationException> {
            json.decodeFromString<EarthquakeNetworkModel>(
                """{"id":"x","type":"Feature","properties":{"time":1},"geometry":{"type":"Point","coordinates":[0,0,0]}}"""
            )
        }
    }

    @Test
    fun rejectsInvalidCoordinatesBeforeStorage() {
        val valid =
            json.decodeFromString<EarthquakeNetworkModel>(
                EarthquakeJsonFixtures.read("earthquake-detail.json")
            )
        for (coordinates in
            listOf(listOf(1.0), listOf(0.0, 91.0, 0.0), listOf(Double.NaN, 0.0, 0.0))) {
            assertFailsWith<IllegalArgumentException> {
                valid
                    .copy(geometry = valid.geometry.copy(coordinates = coordinates))
                    .toDatabaseModel()
            }
        }
        assertFailsWith<IllegalArgumentException> {
            valid.copy(geometry = valid.geometry.copy(type = "LineString")).toDatabaseModel()
        }
    }

    @Test
    fun validatesRequestArgumentsBeforeSending() = runTest {
        client { error("Invalid requests must not reach the engine") }
            .use { client ->
                for (id in listOf("", "../test", "test?format=json")) {
                    assertFailsWith<IllegalArgumentException> {
                        EarthquakeApi(client).getEarthquake(id)
                    }
                }
            }
        assertFailsWith<IllegalArgumentException> {
            EarthquakeCatalogResources.Query(limit = 20001)
        }
        assertFailsWith<IllegalArgumentException> { EarthquakeCatalogResources.Query(offset = 0) }
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
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
        )
    }
}
