package com.johnturkson.sandbox.work

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.await
import androidx.work.testing.TestListenableWorkerBuilder
import com.johnturkson.sandbox.data.notifications.EarthquakeNotifications
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.withTimeout
import org.junit.Test

class EarthquakeMonitorTest {
    @Test
    fun duplicateRefreshRequestsKeepExistingQueuedWork() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = WorkManager.getInstance(context)
        val scheduler = EarthquakeMonitor(manager)
        manager.cancelUniqueWork(EarthquakeMonitor.RefreshWork).await()
        val request =
            OneTimeWorkRequestBuilder<EarthquakeRefreshWorker>()
                .setInitialDelay(1.days.toJavaDuration())
                .build()
        try {
            manager
                .enqueueUniqueWork(
                    EarthquakeMonitor.RefreshWork,
                    ExistingWorkPolicy.KEEP,
                    request,
                )
                .await()
            repeat(3) { scheduler.refresh().await() }
            val infos = manager.getWorkInfosForUniqueWork(EarthquakeMonitor.RefreshWork).get()
            assertEquals(listOf(request.id), infos.map { it.id })
            assertEquals(WorkInfo.State.ENQUEUED, infos.single().state)
        } finally {
            manager.cancelUniqueWork(EarthquakeMonitor.RefreshWork).await()
        }
    }

    @Test
    fun nextRefreshReplacesOldResultAndReportsFailureThroughWorkInfo() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertOffline(context)
        val manager = WorkManager.getInstance(context)
        val scheduler = EarthquakeMonitor(manager)
        manager.cancelUniqueWork(EarthquakeMonitor.RefreshWork).await()
        try {
            scheduler.refresh().await()
            val first =
                withTimeout(45.seconds) {
                    scheduler.refreshWork.filterNotNull().first { it.state.isFinished }
                }
            scheduler.refresh().await()
            val second =
                withTimeout(45.seconds) {
                    scheduler.refreshWork.filterNotNull().first { it.state.isFinished }
                }
            assertNotEquals(first.id, second.id)
            listOf(first, second).forEach { info ->
                assertEquals(WorkInfo.State.FAILED, info.state)
            }
            assertEquals(second, scheduler.refreshWork.first())
        } finally {
            manager.cancelUniqueWork(EarthquakeMonitor.RefreshWork).await()
        }
    }

    @Test
    fun stoppingMonitoringLeavesSharedRefreshQueued() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = WorkManager.getInstance(context)
        val scheduler = EarthquakeMonitor(manager)
        manager.cancelUniqueWork(EarthquakeMonitor.RefreshWork).await()
        val request =
            OneTimeWorkRequestBuilder<EarthquakeRefreshWorker>()
                .setInitialDelay(1.days.toJavaDuration())
                .build()
        val worker = monitoringWorker(context, scheduler)
        try {
            manager
                .enqueueUniqueWork(
                    EarthquakeMonitor.RefreshWork,
                    ExistingWorkPolicy.KEEP,
                    request,
                )
                .await()
            val monitoring = async(start = CoroutineStart.UNDISPATCHED) { worker.doWork() }
            scheduler.refresh().await()
            assertFalse(monitoring.isCompleted)
            monitoring.cancelAndJoin()
            assertEquals(WorkInfo.State.ENQUEUED, manager.getWorkInfoById(request.id).get()!!.state)
            assertEquals(
                listOf(request.id),
                manager.getWorkInfosForUniqueWork(EarthquakeMonitor.RefreshWork).get().map {
                    it.id
                },
            )
        } finally {
            manager.cancelUniqueWork(EarthquakeMonitor.RefreshWork).await()
        }
    }

    @Test
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun monitoringChecksEveryMinuteAfterFailureAndCancellationStopsChecks() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertOffline(context)
        val manager = WorkManager.getInstance(context)
        val scheduler = EarthquakeMonitor(manager)
        manager.cancelUniqueWork(EarthquakeMonitor.RefreshWork).await()
        val previous = scheduler.refreshWork.first()
        val dispatcher = StandardTestDispatcher()
        val monitoring = async(dispatcher) { monitoringWorker(context, scheduler).doWork() }
        dispatcher.scheduler.runCurrent()
        try {
            suspend fun nextFailure(previous: WorkInfo?) =
                withTimeout(45.seconds) {
                    scheduler.refreshWork.filterNotNull().first {
                        it.id != previous?.id && it.state == WorkInfo.State.FAILED
                    }
                }
            val first = nextFailure(previous)
            dispatcher.scheduler.runCurrent()
            dispatcher.scheduler.advanceTimeBy(59_999)
            dispatcher.scheduler.runCurrent()
            assertEquals(first.id, scheduler.refreshWork.first()?.id)
            dispatcher.scheduler.advanceTimeBy(1)
            dispatcher.scheduler.runCurrent()
            val second = nextFailure(first)
            assertNotEquals(first.id, second.id)
            assertFalse(monitoring.isCompleted)
            monitoring.cancel()
            dispatcher.scheduler.runCurrent()
            monitoring.join()
            dispatcher.scheduler.advanceTimeBy(180_000)
            dispatcher.scheduler.runCurrent()
            assertEquals(second.id, scheduler.refreshWork.first()?.id)
        } finally {
            monitoring.cancel()
            dispatcher.scheduler.runCurrent()
            monitoring.join()
            manager.cancelUniqueWork(EarthquakeMonitor.RefreshWork).await()
        }
    }

    private fun monitoringWorker(
        context: Context,
        scheduler: EarthquakeMonitor,
    ): EarthquakeMonitoringWorker {
        val factory =
            object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ): ListenableWorker =
                    EarthquakeMonitoringWorker(
                        appContext,
                        workerParameters,
                        scheduler,
                        EarthquakeNotifications(context),
                    )
            }
        return TestListenableWorkerBuilder<EarthquakeMonitoringWorker>(context)
            .setWorkerFactory(factory)
            .build()
    }

    private fun assertOffline(context: Context) {
        val network = context.getSystemService(ConnectivityManager::class.java)
        assertFalse(
            network
                .getNetworkCapabilities(network.activeNetwork)
                ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true,
            "Run with Wi-Fi and mobile data disabled under the shared emulator reservation",
        )
    }
}
