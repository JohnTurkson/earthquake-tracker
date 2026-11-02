package com.johnturkson.sandbox

import com.johnturkson.sandbox.data.ItemNetworkModel
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Test

class KtorSerializationTest {
    @Test
    fun decodesNetworkModelWithUnknownFields() = runTest {
        val engine = MockEngine { request ->
            assertEquals("/items/item-1", request.url.encodedPath)
            respond(
                content = """{"id":"item-1","extra":"ignored"}""",
                headers =
                    headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }

        HttpClient(engine) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            }
            .use { client ->
                val item = client.get("https://example.test/items/item-1").body<ItemNetworkModel>()
                assertEquals(ItemNetworkModel(id = "item-1"), item)
            }
    }
}
