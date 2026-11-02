package com.johnturkson.sandbox.ui.details.reports

import android.graphics.Bitmap
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import coil3.decode.DataSource
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.johnturkson.sandbox.theme.EarthquakeTheme
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class EarthquakeReportImageTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun reopeningAnImageUsesMemoryThenDiskAcrossLoaders(): Unit = runBlocking {
        val calls = AtomicInteger()
        val bytes = imageBytes()
        val client =
            HttpClient(
                MockEngine {
                    calls.incrementAndGet()
                    respond(bytes, headers = headersOf(HttpHeaders.ContentType, "image/png"))
                }
            )
        val loader = earthquakeReportImageLoader(composeRule.activity, client)
        val reopenedLoader = earthquakeReportImageLoader(composeRule.activity, client)
        val url = "https://earthquake.usgs.gov/fixture-${UUID.randomUUID()}.png"
        val request = ImageRequest.Builder(composeRule.activity).data(url).build()
        try {
            assertEquals(
                DataSource.NETWORK,
                assertIs<SuccessResult>(loader.execute(request)).dataSource,
            )
            assertEquals(
                DataSource.MEMORY_CACHE,
                assertIs<SuccessResult>(loader.execute(request)).dataSource,
            )
            loader.shutdown()
            assertEquals(
                DataSource.DISK,
                assertIs<SuccessResult>(reopenedLoader.execute(request)).dataSource,
            )
            assertEquals(1, calls.get())
        } finally {
            loader.shutdown()
            reopenedLoader.shutdown()
            loader.diskCache?.remove(url)
            client.close()
        }
    }

    @Test
    fun failedImageCanRetryReopenFromCachesAndOpenItsSource() {
        val calls = AtomicInteger()
        val bytes = imageBytes()
        val client =
            HttpClient(
                MockEngine {
                    if (calls.incrementAndGet() == 1) {
                        respond("Unavailable", HttpStatusCode.ServiceUnavailable)
                    } else {
                        respond(bytes, headers = headersOf(HttpHeaders.ContentType, "image/png"))
                    }
                }
            )
        val loader = earthquakeReportImageLoader(composeRule.activity, client)
        val url = "https://earthquake.usgs.gov/fixture-${UUID.randomUUID()}.png"
        val visible = mutableStateOf(true)
        var opened: String? = null
        try {
            composeRule.setContent {
                EarthquakeTheme {
                    if (visible.value) {
                        EarthquakeReportImage(
                            url,
                            "Published USGS report image",
                            { opened = it },
                            imageLoader = loader,
                        )
                    }
                }
            }
            composeRule.waitUntil(5_000) { composeRule.onNodeWithText("Retry map").isDisplayed() }
            composeRule.onNodeWithText("Retry map").performClick()
            composeRule.waitUntil(5_000) {
                calls.get() == 2 &&
                    composeRule
                        .onNodeWithContentDescription("Published USGS report image")
                        .isDisplayed()
            }
            composeRule
                .onNodeWithContentDescription("Published USGS report image")
                .assertIsDisplayed()
            repeat(2) { attempt ->
                composeRule.runOnIdle { visible.value = false }
                composeRule
                    .onNodeWithContentDescription("Published USGS report image")
                    .assertDoesNotExist()
                if (attempt == 1) {
                    loader.memoryCache?.clear()
                }
                composeRule.runOnIdle { visible.value = true }
                composeRule.waitUntil(5_000) {
                    composeRule
                        .onNodeWithContentDescription("Published USGS report image")
                        .isDisplayed()
                }
                assertEquals(2, calls.get())
            }
            composeRule.onNodeWithText("Open full-size USGS image").performClick()
            assertEquals(url, opened)
        } finally {
            loader.shutdown()
            loader.diskCache?.remove(url)
            client.close()
        }
    }

    private fun imageBytes(): ByteArray {
        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        return try {
            bitmap.eraseColor(Color.BLUE)
            ByteArrayOutputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                output.toByteArray()
            }
        } finally {
            bitmap.recycle()
        }
    }
}
