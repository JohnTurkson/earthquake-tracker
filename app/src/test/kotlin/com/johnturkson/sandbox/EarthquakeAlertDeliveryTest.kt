package com.johnturkson.sandbox

import com.johnturkson.sandbox.data.EarthquakeAlertDelivery
import com.johnturkson.sandbox.data.EarthquakeSettings
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Test

class EarthquakeAlertDeliveryTest {
    @Test
    fun deduplicatesAcrossPollsAndNewDispatcherInstances() = runTest {
        val delivered = mutableSetOf<String>()
        val posted = mutableListOf<String>()
        fun dispatcher() =
            EarthquakeAlertDelivery(
                { it in delivered },
                {
                    posted += it.id
                    true
                },
                { delivered += it.eventId },
            )
        val item =
            EarthquakeMonitoringFixtures.item(
                id = "one",
                time = Instant.fromEpochMilliseconds(100_000),
                updated = Instant.fromEpochMilliseconds(100_000),
            )
        dispatcher()
            .deliver(
                listOf(item),
                EarthquakeSettings(alertsEnabled = true),
                Instant.fromEpochMilliseconds(100_000),
            )
        dispatcher()
            .deliver(
                listOf(item, item.copy(id = "two")),
                EarthquakeSettings(alertsEnabled = true),
                Instant.fromEpochMilliseconds(110_000),
            )
        assertEquals(listOf("one", "two"), posted)
    }

    @Test
    fun preferenceAndPostingFailuresDoNotMarkDelivery() = runTest {
        val delivered = mutableSetOf<String>()
        var allowed = false
        var attempts = 0
        val dispatcher =
            EarthquakeAlertDelivery(
                { it in delivered },
                {
                    attempts++
                    allowed
                },
                { delivered += it.eventId },
            )
        val item =
            EarthquakeMonitoringFixtures.item(
                id = "one",
                time = Instant.fromEpochMilliseconds(100_000),
                updated = Instant.fromEpochMilliseconds(100_000),
            )
        dispatcher.deliver(
            listOf(item),
            EarthquakeSettings(),
            Instant.fromEpochMilliseconds(100_000),
        )
        assertEquals(0, attempts)
        dispatcher.deliver(
            listOf(item),
            EarthquakeSettings(alertsEnabled = true),
            Instant.fromEpochMilliseconds(100_000),
        )
        assertEquals(emptySet(), delivered)
        allowed = true
        dispatcher.deliver(
            listOf(item),
            EarthquakeSettings(alertsEnabled = true),
            Instant.fromEpochMilliseconds(100_000),
        )
        assertEquals(setOf("one"), delivered)
    }
}
