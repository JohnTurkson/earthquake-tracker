package com.johnturkson.sandbox

import android.app.Notification
import android.app.NotificationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import com.johnturkson.sandbox.app.EarthquakeActivity
import com.johnturkson.sandbox.data.EarthquakeNotifications
import com.johnturkson.sandbox.work.EarthquakeMonitor
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test

class EarthquakeWorkIntegrationTest {
    @Test
    fun realOfflineRefreshFailsWhileForegroundSessionContinuesUntilStopped() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val manager = WorkManager.getInstance(context)
        val scheduler = EarthquakeMonitor(manager)
        val network = context.getSystemService(ConnectivityManager::class.java)
        val notifications = EarthquakeNotifications(context)
        val systemNotifications = context.getSystemService(NotificationManager::class.java)
        val alert =
            EarthquakeMonitoringFixtures.item(
                id = "stop-test-${UUID.randomUUID()}",
                time = Clock.System.now(),
            )
        try {
            assertFalse(
                network
                    .getNetworkCapabilities(network.activeNetwork)
                    ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true,
                "Run the device suite with Wi-Fi and mobile data disabled under the shared reservation",
            )
            ActivityScenario.launch(EarthquakeActivity::class.java).use {
                scheduler.refresh().await()
                withTimeout(45.seconds) {
                    manager.getWorkInfosForUniqueWorkFlow(EarthquakeMonitor.RefreshWork).first {
                        infos ->
                        infos.any { info -> info.state == WorkInfo.State.FAILED }
                    }
                }
                scheduler.start()
                scheduler.start()
                val running =
                    withTimeout(45.seconds) {
                            manager
                                .getWorkInfosForUniqueWorkFlow(EarthquakeMonitor.MonitoringWork)
                                .first { infos ->
                                    infos.any { info -> info.state == WorkInfo.State.RUNNING }
                                }
                        }
                        .filter { info -> !info.state.isFinished }
                assertEquals(1, running.size)
                val id = running.single().id
                val capability = notifications.capabilities()
                if (
                    capability.permissionGranted &&
                        capability.appEnabled &&
                        capability.monitoringEnabled
                ) {
                    withTimeout(5.seconds) {
                        while (
                            systemNotifications.activeNotifications.none {
                                it.id == EarthquakeNotifications.MonitoringId &&
                                    it.notification.extras.getString(Notification.EXTRA_TITLE) ==
                                        context.getString(R.string.monitor_notification_title)
                            }
                        ) delay(50.milliseconds)
                    }
                }
                val posted = notifications.postAlert(alert)
                if (posted) {
                    withTimeout(5.seconds) {
                        while (
                            systemNotifications.activeNotifications.none { it.tag == alert.id }
                        ) delay(50.milliseconds)
                    }
                }
                val notification = notifications.foreground(id).notification
                notification.actions
                    .single { action -> action.title == "Stop monitoring" }
                    .actionIntent
                    .send()
                val stopped =
                    withTimeout(45.seconds) {
                        manager.getWorkInfoByIdFlow(id).first { info ->
                            info?.state == WorkInfo.State.CANCELLED
                        }
                    }
                assertEquals(WorkInfo.State.CANCELLED, stopped?.state)
                withTimeout(5.seconds) {
                    while (
                        systemNotifications.activeNotifications.any {
                            it.id == EarthquakeNotifications.MonitoringId
                        }
                    ) delay(50.milliseconds)
                }
                if (posted)
                    assertTrue(systemNotifications.activeNotifications.any { it.tag == alert.id })
                assertEquals(
                    0,
                    manager
                        .getWorkInfosForUniqueWork(EarthquakeMonitor.MonitoringWork)
                        .get()
                        .count { info -> !info.state.isFinished },
                )
            }
        } finally {
            scheduler.stop().await()
            systemNotifications.cancel(alert.id, EarthquakeNotifications.AlertId)
        }
    }
}
