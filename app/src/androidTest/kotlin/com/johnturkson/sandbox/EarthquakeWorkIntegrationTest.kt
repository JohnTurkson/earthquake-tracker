package com.johnturkson.sandbox

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.johnturkson.sandbox.app.EarthquakeActivity
import com.johnturkson.sandbox.data.EarthquakeNotifications
import com.johnturkson.sandbox.work.EarthquakeWorkScheduler
import kotlin.test.assertEquals
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test

class EarthquakeWorkIntegrationTest {
    @Test
    fun realOfflineRefreshFailsAndForegroundSessionPausesUntilStopped() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        fun shell(command: String): String =
            instrumentation.uiAutomation.executeShellCommand(command).use { descriptor ->
                android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor)
                    .bufferedReader()
                    .use { it.readText().trim() }
            }
        val wifi = shell("settings get global wifi_on")
        val mobile = shell("settings get global mobile_data")
        val manager = WorkManager.getInstance(context)
        val scheduler = EarthquakeWorkScheduler(manager)
        val network = context.getSystemService(ConnectivityManager::class.java)
        val notifications = EarthquakeNotifications(context)
        try {
            shell("svc wifi disable")
            shell("svc data disable")
            withTimeout(15_000) {
                while (
                    network
                        .getNetworkCapabilities(network.activeNetwork)
                        ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
                ) delay(50)
            }
            ActivityScenario.launch(EarthquakeActivity::class.java).use {
                withTimeout(15_000) {
                    manager
                        .getWorkInfosForUniqueWorkFlow(EarthquakeWorkScheduler.RefreshWork)
                        .first { infos ->
                            infos.any { info ->
                                info.state == WorkInfo.State.FAILED &&
                                    info.outputData.getString(EarthquakeWorkScheduler.ResultKey) ==
                                        "Offline"
                            }
                        }
                }
                scheduler.startMonitoring()
                scheduler.startMonitoring()
                val running =
                    withTimeout(15_000) {
                            manager
                                .getWorkInfosForUniqueWorkFlow(
                                    EarthquakeWorkScheduler.MonitoringWork
                                )
                                .first { infos ->
                                    infos.any { info ->
                                        info.state == WorkInfo.State.RUNNING &&
                                            info.progress.getString(
                                                EarthquakeWorkScheduler.ResultKey
                                            ) == "Offline"
                                    }
                                }
                        }
                        .filter { info -> !info.state.isFinished }
                assertEquals(1, running.size)
                val id = running.single().id
                val notification = notifications.foreground(id, offline = true).notification
                notification.actions
                    .single { action -> action.title == "Stop monitoring" }
                    .actionIntent
                    .send()
                val stopped =
                    withTimeout(15_000) {
                        manager.getWorkInfoByIdFlow(id).first { info ->
                            info?.state == WorkInfo.State.CANCELLED
                        }
                    }
                assertEquals(WorkInfo.State.CANCELLED, stopped?.state)
                assertEquals(
                    0,
                    manager
                        .getWorkInfosForUniqueWork(EarthquakeWorkScheduler.MonitoringWork)
                        .get()
                        .count { info -> !info.state.isFinished },
                )
            }
        } finally {
            scheduler.stopMonitoring()
            shell("svc wifi ${if (wifi == "1") "enable" else "disable"}")
            shell("svc data ${if (mobile == "1") "enable" else "disable"}")
        }
    }
}
