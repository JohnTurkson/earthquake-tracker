package com.johnturkson.sandbox.data.network

internal object EarthquakeJsonFixtures {
    fun read(name: String): String =
        checkNotNull(javaClass.getResource("/$name")) { "Missing earthquake fixture: $name" }
            .readText()
}
