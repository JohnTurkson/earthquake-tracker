package com.johnturkson.sandbox

import com.johnturkson.sandbox.data.EarthquakeGeometryNetworkModel
import com.johnturkson.sandbox.data.EarthquakeNetworkModel
import com.johnturkson.sandbox.data.EarthquakePropertiesNetworkModel
import com.johnturkson.sandbox.data.toDatabaseModel

internal object EarthquakeMonitoringFixtures {
    fun item(
        id: String = "one",
        time: Long = 100_000,
        updated: Long = time,
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
