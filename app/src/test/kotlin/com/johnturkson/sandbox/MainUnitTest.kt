package com.johnturkson.sandbox

import app.cash.turbine.test
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class MainUnitTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun test() {
        assertTrue { true }
    }

    @Test fun suspendTest() = runTest { emptyFlow<String>().test { awaitComplete() } }

    @Test
    fun apiTest() = runTest {
        val mockEngine = MockEngine { respond("response") }
        val client = HttpClient(mockEngine)
        val response = client.get("").bodyAsText()
        assertTrue { response.isNotBlank() }
    }
}
