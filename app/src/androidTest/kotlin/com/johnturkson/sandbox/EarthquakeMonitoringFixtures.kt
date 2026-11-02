package com.johnturkson.sandbox

import com.johnturkson.sandbox.data.EarthquakeGeometryNetworkModel
import com.johnturkson.sandbox.data.EarthquakeNetworkModel
import com.johnturkson.sandbox.data.EarthquakePropertiesNetworkModel
import com.johnturkson.sandbox.data.toDatabaseModel
import kotlin.time.Instant

internal object EarthquakeMonitoringFixtures {
    fun item(
        id: String = "one",
        time: Instant = Instant.fromEpochMilliseconds(100_000),
        updated: Instant = time,
        magnitude: Double? = 6.0,
    ) =
        EarthquakeNetworkModel(
                id,
                "Feature",
                EarthquakePropertiesNetworkModel(
                    time,
                    updated,
                    magnitude,
                    place = "Pacific",
                    type = "earthquake",
                ),
                EarthquakeGeometryNetworkModel("Point", listOf(2.0, 1.0, 10.0)),
            )
            .toDatabaseModel()
}
