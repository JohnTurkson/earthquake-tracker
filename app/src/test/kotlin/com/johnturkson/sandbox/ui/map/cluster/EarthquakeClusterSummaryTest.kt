package com.johnturkson.sandbox.ui.map.cluster

import com.johnturkson.sandbox.ui.event.EarthquakeMapItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EarthquakeClusterSummaryTest {
    @Test
    fun maximumIgnoresMissingAndNonFiniteMagnitudes() {
        val items =
            listOf(null, Double.NaN, Double.POSITIVE_INFINITY, -1.2, 0.0, 4.8).mapIndexed {
                index,
                magnitude ->
                EarthquakeMapItem("$index", 0.0, 0.0, "Event", magnitude = magnitude)
            }
        assertEquals(4.8, items.maximumMagnitude())
        assertEquals(-1.2, items.takeLast(3).take(1).maximumMagnitude())
        assertNull(items.take(3).maximumMagnitude())
        assertNull(emptyList<EarthquakeMapItem>().maximumMagnitude())
    }
}
