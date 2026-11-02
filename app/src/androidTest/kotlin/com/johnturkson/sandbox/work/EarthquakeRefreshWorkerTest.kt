package com.johnturkson.sandbox.work

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.johnturkson.sandbox.data.repository.EarthquakeMonitoringFixtures
import com.johnturkson.sandbox.data.repository.EarthquakeRefresher
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import org.junit.Test

class EarthquakeRefreshWorkerTest {
    @Test
    fun offlineCheckFailsBeforeFetchingOrPublishingAlerts() = runBlocking {
        val operations = mutableListOf<String>()
        val worker = worker({ operations += "save" }, { operations += "alerts" }, online = false)
        assertEquals(ListenableWorker.Result.failure(), worker.doWork())
        assertEquals(emptyList(), operations)
    }

    @Test
    fun successCommitsDataBeforePublishingAlerts() = runBlocking {
        val operations = mutableListOf<String>()
        val worker = worker({ operations += "save" }, { operations += "alerts" })
        assertEquals(ListenableWorker.Result.success(), worker.doWork())
        assertEquals(listOf("save", "alerts"), operations)
    }

    @Test
    fun refreshExceptionsEscapeForWorkManagerToMarkFailed() = runBlocking {
        listOf(
                IOException(),
                SerializationException(),
                IllegalArgumentException(),
                IllegalStateException(),
            )
            .forEach { failure ->
                var alerts = 0
                val worker = worker({ throw failure }, { alerts++ })
                val actual = assertFailsWith<Exception> { worker.doWork() }
                assertSame(failure, actual)
                assertEquals(0, alerts)
            }
    }

    @Test
    fun alertFailureKeepsSuccessfulRefresh() = runBlocking {
        val worker = worker(onAlerts = { throw SecurityException("Notifications unavailable") })
        assertEquals(ListenableWorker.Result.success(), worker.doWork())
    }

    @Test
    fun cancellationPropagatesFromRefreshAndAlertDelivery() =
        runBlocking<Unit> {
            assertFailsWith<CancellationException> {
                worker(onRefresh = { throw CancellationException() }).doWork()
            }
            assertFailsWith<CancellationException> {
                worker(onAlerts = { throw CancellationException() }).doWork()
            }
        }

    private fun worker(
        onRefresh: suspend () -> Unit = {},
        onAlerts: suspend () -> Unit = {},
        online: Boolean = true,
    ): EarthquakeRefreshWorker {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
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
                        object : EarthquakeRefresher {
                            override suspend fun refresh() = onRefresh()

                            override suspend fun publishAlerts() = onAlerts()
                        },
                        EarthquakeMonitoringFixtures.connection(online),
                    )
            }
        return TestListenableWorkerBuilder<EarthquakeRefreshWorker>(context)
            .setWorkerFactory(factory)
            .build()
    }
}
