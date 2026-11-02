package com.johnturkson.sandbox

import android.Manifest
import android.app.NotificationManager
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.test.platform.app.InstrumentationRegistry
import com.johnturkson.sandbox.data.EarthquakeNotifications
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test

class EarthquakeNotificationsTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val manager = context.getSystemService(NotificationManager::class.java)

    @Test
    fun channelsHaveSeparateImportanceAndRegistrationIsIdempotent() {
        val notifications = EarthquakeNotifications(context)
        notifications.registerChannels()
        val before = manager.notificationChannels.associateBy { it.id }
        notifications.registerChannels()
        assertEquals(before, manager.notificationChannels.associateBy { it.id })
        assertEquals(
            NotificationManager.IMPORTANCE_LOW,
            before.getValue(EarthquakeNotifications.MonitoringChannel).importance,
        )
        assertEquals(
            NotificationManager.IMPORTANCE_HIGH,
            before.getValue(EarthquakeNotifications.AlertsChannel).importance,
        )
        assertEquals(null, before.getValue(EarthquakeNotifications.MonitoringChannel).sound)
    }

    @Test
    fun permissionCapabilityMatchesAndroidAndControlsPosting() = runBlocking {
        val notifications = EarthquakeNotifications(context)
        val wasGranted =
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        val now = Clock.System.now()
        val first =
            EarthquakeMonitoringFixtures.item(
                id = "notification-test-${UUID.randomUUID()}",
                time = now,
                updated = now,
            )
        val second = first.copy(id = "${first.id}-second")
        try {
            assertEquals(wasGranted, notifications.capabilities().permissionGranted)
            val allowed =
                wasGranted &&
                    notifications.capabilities().appEnabled &&
                    notifications.capabilities().alertsEnabled
            assertEquals(allowed, notifications.postAlert(first))
            assertEquals(allowed, notifications.postAlert(second))
            val expected = if (allowed) 2 else 0
            val posted =
                withTimeout(5_000) {
                    var active =
                        manager.activeNotifications.filter {
                            it.tag == first.id || it.tag == second.id
                        }
                    while (active.size != expected) {
                        delay(50)
                        active =
                            manager.activeNotifications.filter {
                                it.tag == first.id || it.tag == second.id
                            }
                    }
                    active
                }
            assertEquals(expected, posted.size)
            if (allowed) {
                assertTrue(
                    posted.all {
                        it.notification.channelId == EarthquakeNotifications.AlertsChannel
                    }
                )
                assertNotEquals(
                    posted[0].notification.contentIntent,
                    posted[1].notification.contentIntent,
                )
            }
            val foreground = notifications.foreground(UUID.randomUUID(), offline = true)
            assertEquals(EarthquakeNotifications.MonitoringId, foreground.notificationId)
            assertEquals(
                EarthquakeNotifications.MonitoringChannel,
                foreground.notification.channelId,
            )
            assertTrue(foreground.notification.actions.any { it.title == "Stop monitoring" })
            if (!allowed) assertFalse(notifications.previewAlert())
            assertEquals(
                EarthquakeNotifications.MonitoringChannel,
                notifications.foreground(UUID.randomUUID()).notification.channelId,
            )
        } finally {
            manager.cancel(first.id, EarthquakeNotifications.AlertId)
            manager.cancel(second.id, EarthquakeNotifications.AlertId)
        }
    }
}
