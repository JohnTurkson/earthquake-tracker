package com.johnturkson.sandbox.data.repository

import com.johnturkson.sandbox.data.network.EarthquakeConnection
import com.johnturkson.sandbox.data.network.EarthquakeGeometryNetworkModel
import com.johnturkson.sandbox.data.network.EarthquakeNetworkModel
import com.johnturkson.sandbox.data.network.EarthquakePropertiesNetworkModel
import kotlin.time.Instant

internal object EarthquakeMonitoringFixtures {
    fun connection(online: Boolean = true): EarthquakeConnection =
        object : EarthquakeConnection {
            override fun isOnline() = online
        }

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
