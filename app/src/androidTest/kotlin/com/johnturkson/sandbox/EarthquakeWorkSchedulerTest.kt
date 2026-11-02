package com.johnturkson.sandbox

import android.app.Notification
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.concurrent.futures.CallbackToFutureAdapter
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
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test

class EarthquakeWorkSchedulerTest {
    @Test
    fun overlappingRequestsJoinExistingWorkAndObserveItsTerminalResult() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = WorkManager.getInstance(context)
        val scheduler = EarthquakeWorkScheduler(manager)
        manager.cancelUniqueWork(EarthquakeWorkScheduler.RefreshWork).await()
        val request =
            OneTimeWorkRequestBuilder<EarthquakeRefreshWorker>()
                .setInitialDelay(1, TimeUnit.DAYS)
                .build()
        try {
            manager
                .enqueueUniqueWork(
                    EarthquakeWorkScheduler.RefreshWork,
                    ExistingWorkPolicy.KEEP,
                    request,
                )
                .await()
            var runningCallbacks = 0
            val first =
                async(start = CoroutineStart.UNDISPATCHED) {
                    scheduler.refreshAndAwait { runningCallbacks++ }
                }
            val second =
                async(start = CoroutineStart.UNDISPATCHED) {
                    scheduler.refreshAndAwait { runningCallbacks++ }
                }
            scheduler.refresh().await()
            assertEquals(
                listOf(request.id),
                manager.getWorkInfosForUniqueWork(EarthquakeWorkScheduler.RefreshWork).get().map {
                    it.id
                },
            )
            assertFalse(first.isCompleted)
            assertFalse(second.isCompleted)
            assertEquals(0, runningCallbacks)
            manager.cancelWorkById(request.id).await()
            withTimeout(5_000) {
                listOf(first.await(), second.await()).forEach { info ->
                    assertEquals(request.id, info.id)
                    assertEquals(WorkInfo.State.CANCELLED, info.state)
                }
            }
        } finally {
            manager.cancelUniqueWork(EarthquakeWorkScheduler.RefreshWork).await()
        }
    }

    @Test
    fun nextRefreshReplacesOldResultAndReturnsOfflineOutput() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val network = context.getSystemService(ConnectivityManager::class.java)
        assertFalse(
            network
                .getNetworkCapabilities(network.activeNetwork)
                ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true,
            "Run with Wi-Fi and mobile data disabled under the shared emulator reservation",
        )
        val manager = WorkManager.getInstance(context)
        val scheduler = EarthquakeWorkScheduler(manager)
        manager.cancelUniqueWork(EarthquakeWorkScheduler.RefreshWork).await()
        try {
            val first = withTimeout(15_000) { scheduler.refreshAndAwait() }
            val second = withTimeout(15_000) { scheduler.refreshAndAwait() }
            assertNotEquals(first.id, second.id)
            listOf(first, second).forEach { info ->
                assertEquals(WorkInfo.State.FAILED, info.state)
                assertEquals(
                    "Offline",
                    info.outputData.getString(EarthquakeWorkScheduler.ResultKey),
                )
            }
            assertEquals(second, scheduler.refreshWork.first())
        } finally {
            manager.cancelUniqueWork(EarthquakeWorkScheduler.RefreshWork).await()
        }
    }

    @Test
    fun stoppingMonitoringWaitLeavesSharedRefreshQueued() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = WorkManager.getInstance(context)
        val scheduler = EarthquakeWorkScheduler(manager)
        manager.cancelUniqueWork(EarthquakeWorkScheduler.RefreshWork).await()
        val request =
            OneTimeWorkRequestBuilder<EarthquakeRefreshWorker>()
                .setInitialDelay(1, TimeUnit.DAYS)
                .build()
        val titles = mutableListOf<String?>()
        val worker =
            monitoringBuilder(context, scheduler)
                .setForegroundUpdater { _, _, foreground ->
                    titles += foreground.notification.extras.getString(Notification.EXTRA_TITLE)
                    CallbackToFutureAdapter.getFuture<Void> { completer ->
                        completer.set(null)
                        "Monitoring foreground"
                    }
                }
                .build()
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
            assertEquals(
                setOf(context.getString(R.string.monitor_notification_title)),
                titles.toSet(),
            )
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
    fun monitoringTickEnqueuesSharedRefreshAndReceivesItsOfflineResult() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val network = context.getSystemService(ConnectivityManager::class.java)
        assertFalse(
            network
                .getNetworkCapabilities(network.activeNetwork)
                ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true,
            "Run with Wi-Fi and mobile data disabled under the shared emulator reservation",
        )
        val manager = WorkManager.getInstance(context)
        val scheduler = EarthquakeWorkScheduler(manager)
        manager.cancelUniqueWork(EarthquakeWorkScheduler.RefreshWork).await()
        val observed = CompletableDeferred<Unit>()
        val worker =
            monitoringBuilder(context, scheduler)
                .setProgressUpdater { _, _, data ->
                    if (data.getString(EarthquakeWorkScheduler.ResultKey) == "Offline")
                        observed.complete(Unit)
                    CallbackToFutureAdapter.getFuture<Void> { completer ->
                        completer.set(null)
                        "Monitoring progress"
                    }
                }
                .build()
        val monitoring = async(start = CoroutineStart.UNDISPATCHED) { worker.doWork() }
        try {
            withTimeout(15_000) { observed.await() }
            val info = scheduler.refreshWork.first()!!
            assertEquals(WorkInfo.State.FAILED, info.state)
            assertEquals("Offline", info.outputData.getString(EarthquakeWorkScheduler.ResultKey))
            assertFalse(monitoring.isCompleted)
        } finally {
            monitoring.cancelAndJoin()
            manager.cancelUniqueWork(EarthquakeWorkScheduler.RefreshWork).await()
        }
    }

    private fun monitoringBuilder(
        context: Context,
        scheduler: EarthquakeWorkScheduler,
    ): TestListenableWorkerBuilder<EarthquakeMonitoringWorker> {
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
    }
}
