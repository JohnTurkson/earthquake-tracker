package com.johnturkson.sandbox

import com.johnturkson.sandbox.data.EarthquakeAlertPolicy
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class EarthquakeAlertPolicyTest {
    private val policy = EarthquakeAlertPolicy()
    private val item =
        EarthquakeMonitoringFixtures.item(id = "one", time = 100_000, updated = 100_000)

    @Test
    fun thresholdAndFreshnessBoundaries() {
        assertTrue(policy.qualifies(item, 6, 100_000))
        assertTrue(policy.qualifies(item, 6, 160_000))
        assertFalse(policy.qualifies(item, 6, 160_001))
        assertFalse(policy.qualifies(item, 6, 99_999))
        assertFalse(policy.qualifies(item.copy(magnitude = 5.9), 6, 100_000))
        assertFalse(policy.qualifies(item.copy(magnitude = null), 6, 100_000))
        assertTrue(policy.qualifies(item.copy(magnitude = 5.9), 5, 100_000))
    }

    @Test
    fun revisionsQualifyOnlyWhileTheEventIsFresh() {
        assertFalse(policy.qualifies(item.copy(magnitude = 5.9), 6, 110_000))
        assertTrue(policy.qualifies(item.copy(magnitude = 6.1, updated = 120_000), 6, 120_000))
        assertFalse(policy.qualifies(item.copy(updated = 200_000), 6, 200_000))
    }
}
