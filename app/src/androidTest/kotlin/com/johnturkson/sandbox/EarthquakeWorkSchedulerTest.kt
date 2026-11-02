package com.johnturkson.sandbox

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
import com.johnturkson.sandbox.data.EarthquakeConnection
import com.johnturkson.sandbox.data.EarthquakeNotifications
import com.johnturkson.sandbox.work.EarthquakeMonitoringWorker
import com.johnturkson.sandbox.work.EarthquakeRefreshWorker
import com.johnturkson.sandbox.work.EarthquakeWorkScheduler
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test

class EarthquakeWorkSchedulerTest {
    @Test
    fun duplicateRefreshRequestsKeepExistingQueuedWork() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = WorkManager.getInstance(context)
        val scheduler = EarthquakeWorkScheduler(manager)
        manager.cancelUniqueWork(EarthquakeWorkScheduler.RefreshWork).await()
        val request =
            OneTimeWorkRequestBuilder<EarthquakeRefreshWorker>()
                .setInitialDelay(1.days.toJavaDuration())
                .build()
        try {
            manager
                .enqueueUniqueWork(
                    EarthquakeWorkScheduler.RefreshWork,
                    ExistingWorkPolicy.KEEP,
                    request,
                )
                .await()
            repeat(3) { scheduler.refresh().await() }
            val infos = manager.getWorkInfosForUniqueWork(EarthquakeWorkScheduler.RefreshWork).get()
            assertEquals(listOf(request.id), infos.map { it.id })
            assertEquals(WorkInfo.State.ENQUEUED, infos.single().state)
        } finally {
            manager.cancelUniqueWork(EarthquakeWorkScheduler.RefreshWork).await()
        }
    }

    @Test
    fun nextRefreshReplacesOldResultAndReportsFailureThroughWorkInfo() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertOffline(context)
        val manager = WorkManager.getInstance(context)
        val scheduler = EarthquakeWorkScheduler(manager)
        manager.cancelUniqueWork(EarthquakeWorkScheduler.RefreshWork).await()
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
            manager.cancelUniqueWork(EarthquakeWorkScheduler.RefreshWork).await()
        }
    }

    @Test
    fun stoppingMonitoringLeavesSharedRefreshQueued() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = WorkManager.getInstance(context)
        val scheduler = EarthquakeWorkScheduler(manager)
        manager.cancelUniqueWork(EarthquakeWorkScheduler.RefreshWork).await()
        val request =
            OneTimeWorkRequestBuilder<EarthquakeRefreshWorker>()
                .setInitialDelay(1.days.toJavaDuration())
                .build()
        val worker = monitoringWorker(context, scheduler)
        try {
            manager
                .enqueueUniqueWork(
                    EarthquakeWorkScheduler.RefreshWork,
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
                manager.getWorkInfosForUniqueWork(EarthquakeWorkScheduler.RefreshWork).get().map {
                    it.id
                },
            )
        } finally {
            manager.cancelUniqueWork(EarthquakeWorkScheduler.RefreshWork).await()
        }
    }

    @Test
    fun monitoringTickQueuesTheSharedRefreshWorker() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertOffline(context)
        val manager = WorkManager.getInstance(context)
        val scheduler = EarthquakeWorkScheduler(manager)
        manager.cancelUniqueWork(EarthquakeWorkScheduler.RefreshWork).await()
        val monitoring =
            async(start = CoroutineStart.UNDISPATCHED) {
                monitoringWorker(context, scheduler).doWork()
            }
        try {
            val info =
                withTimeout(45.seconds) {
                    scheduler.refreshWork.filterNotNull().first {
                        it.state == WorkInfo.State.FAILED
                    }
                }
            assertEquals(WorkInfo.State.FAILED, info.state)
            assertFalse(monitoring.isCompleted)
        } finally {
            monitoring.cancelAndJoin()
            manager.cancelUniqueWork(EarthquakeWorkScheduler.RefreshWork).await()
        }
    }

    private fun monitoringWorker(
        context: Context,
        scheduler: EarthquakeWorkScheduler,
    ): EarthquakeMonitoringWorker {
        val connection =
            object : EarthquakeConnection {
                override val online = MutableStateFlow(true)

                override fun isOnline() = online.value
            }
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
                        connection,
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
