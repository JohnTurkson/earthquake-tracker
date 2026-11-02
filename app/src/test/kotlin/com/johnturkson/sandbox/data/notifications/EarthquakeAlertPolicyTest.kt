package com.johnturkson.sandbox.data.notifications

import com.johnturkson.sandbox.data.repository.EarthquakeMonitoringFixtures
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Instant
import org.junit.Test

class EarthquakeAlertPolicyTest {
    private val policy = EarthquakeAlertPolicy()
    private val item =
        EarthquakeMonitoringFixtures.item(
            id = "one",
            time = Instant.fromEpochMilliseconds(100_000),
            updated = Instant.fromEpochMilliseconds(100_000),
        )

    @Test
    fun freshnessRejectsEvenSubMillisecondFutureAndExpiredEvents() {
        assertTrue(policy.qualifies(item, 6, item.time + 1.minutes))
        assertFalse(policy.qualifies(item, 6, item.time + 1.minutes + 1.nanoseconds))
        assertFalse(policy.qualifies(item, 6, item.time - 1.nanoseconds))
    }

    @Test
    fun thresholdAndFreshnessBoundaries() {
        assertTrue(policy.qualifies(item, 6, Instant.fromEpochMilliseconds(100_000)))
        assertTrue(policy.qualifies(item, 6, Instant.fromEpochMilliseconds(160_000)))
        assertFalse(policy.qualifies(item, 6, Instant.fromEpochMilliseconds(160_001)))
        assertFalse(policy.qualifies(item, 6, Instant.fromEpochMilliseconds(99_999)))
        assertFalse(
            policy.qualifies(item.copy(magnitude = 5.9), 6, Instant.fromEpochMilliseconds(100_000))
        )
        assertFalse(
            policy.qualifies(item.copy(magnitude = null), 6, Instant.fromEpochMilliseconds(100_000))
        )
        assertTrue(
            policy.qualifies(item.copy(magnitude = 5.9), 5, Instant.fromEpochMilliseconds(100_000))
        )
    }

    @Test
    fun revisionsQualifyOnlyWhileTheEventIsFresh() {
        assertFalse(
            policy.qualifies(item.copy(magnitude = 5.9), 6, Instant.fromEpochMilliseconds(110_000))
        )
        assertTrue(
            policy.qualifies(
                item.copy(magnitude = 6.1, updated = Instant.fromEpochMilliseconds(120_000)),
                6,
                Instant.fromEpochMilliseconds(120_000),
            )
        )
        assertFalse(
            policy.qualifies(
                item.copy(updated = Instant.fromEpochMilliseconds(200_000)),
                6,
                Instant.fromEpochMilliseconds(200_000),
            )
        )
    }

    @Test
    fun otherSeismicEventTypesDoNotProduceEarthquakeAlerts() {
        assertFalse(
            policy.qualifies(
                item.copy(type = "quarry blast"),
                6,
                Instant.fromEpochMilliseconds(100_000),
            )
        )
        assertFalse(
            policy.qualifies(
                item.copy(type = "explosion"),
                6,
                Instant.fromEpochMilliseconds(100_000),
            )
        )
        assertFalse(
            policy.qualifies(item.copy(type = null), 6, Instant.fromEpochMilliseconds(100_000))
        )
    }
}
