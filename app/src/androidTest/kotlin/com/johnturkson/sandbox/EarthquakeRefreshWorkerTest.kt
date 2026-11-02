package com.johnturkson.sandbox

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.johnturkson.sandbox.data.EarthquakeRefreshResult
import com.johnturkson.sandbox.data.EarthquakeRefresher
import com.johnturkson.sandbox.work.EarthquakeRefreshWorker
import com.johnturkson.sandbox.work.EarthquakeWorkScheduler
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Test

class EarthquakeRefreshWorkerTest {
    @Test
    fun workerReturnsTypedFailureForEveryRefreshError() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        listOf(
                EarthquakeRefreshResult.Offline,
                EarthquakeRefreshResult.NetworkError,
                EarthquakeRefreshResult.DataError,
                EarthquakeRefreshResult.StorageError,
                EarthquakeRefreshResult.Success,
            )
            .forEach { result ->
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
                                    override suspend fun refresh() = result
                                },
                            )
                    }
                val worker =
                    TestListenableWorkerBuilder<EarthquakeRefreshWorker>(context)
                        .setWorkerFactory(factory)
                        .build()
                val output = worker.doWork()
                val data =
                    androidx.work.workDataOf(EarthquakeWorkScheduler.ResultKey to result.name)
                assertEquals(
                    if (result == EarthquakeRefreshResult.Success)
                        ListenableWorker.Result.success(data)
                    else ListenableWorker.Result.failure(data),
                    output,
                )
            }
    }
}
