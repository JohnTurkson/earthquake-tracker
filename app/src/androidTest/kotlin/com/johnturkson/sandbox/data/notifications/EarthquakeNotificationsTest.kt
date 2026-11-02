package com.johnturkson.sandbox.data.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.test.platform.app.InstrumentationRegistry
import com.johnturkson.sandbox.data.repository.EarthquakeMonitoringFixtures
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
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
            before.getValue(EarthquakeNotifications.MONITORING_CHANNEL).importance,
        )
        assertEquals(
            NotificationManager.IMPORTANCE_HIGH,
            before.getValue(EarthquakeNotifications.ALERTS_CHANNEL).importance,
        )
        assertNull(before.getValue(EarthquakeNotifications.MONITORING_CHANNEL).sound)
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
        val second = first.copy(id = "${first.id}-second", magnitude = null, place = null)
        try {
            assertEquals(wasGranted, notifications.capabilities().permissionGranted)
            val allowed =
                wasGranted &&
                    notifications.capabilities().appEnabled &&
                    notifications.capabilities().alertsEnabled
            assertEquals(allowed, notifications.postAlert(first))
            assertEquals(allowed, notifications.postAlert(second))
            val expected =
                if (allowed) {
                    2
                } else {
                    0
                }
            val posted =
                withTimeout(5.seconds) {
                    var active =
                        manager.activeNotifications.filter {
                            it.tag == first.id || it.tag == second.id
                        }
                    while (active.size != expected) {
                        delay(50.milliseconds)
                        active =
                            manager.activeNotifications.filter {
                                it.tag == first.id || it.tag == second.id
                            }
                    }
                    active
                }
            assertEquals(expected, posted.size)
            if (allowed) {
                val titles = posted.associate {
                    it.tag to it.notification.extras.getString(Notification.EXTRA_TITLE)
                }
                assertEquals("M6.0 – Pacific", titles[first.id])
                assertEquals("Unknown Magnitude – Unknown location", titles[second.id])
                assertTrue(
                    posted.all {
                        it.notification.channelId == EarthquakeNotifications.ALERTS_CHANNEL
                    }
                )
                assertNotEquals(
                    posted[0].notification.contentIntent,
                    posted[1].notification.contentIntent,
                )
            }
            val foreground = notifications.foreground(UUID.randomUUID())
            assertEquals(EarthquakeNotifications.MONITORING_ID, foreground.notificationId)
            assertEquals(
                EarthquakeNotifications.MONITORING_CHANNEL,
                foreground.notification.channelId,
            )
            assertTrue(foreground.notification.actions.any { it.title == "Stop monitoring" })
            if (!allowed) {
                assertFalse(notifications.previewAlert())
            }
            assertEquals(
                EarthquakeNotifications.MONITORING_CHANNEL,
                notifications.foreground(UUID.randomUUID()).notification.channelId,
            )
        } finally {
            manager.cancel(first.id, EarthquakeNotifications.ALERT_ID)
            manager.cancel(second.id, EarthquakeNotifications.ALERT_ID)
        }
    }
}
