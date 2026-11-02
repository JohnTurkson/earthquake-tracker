package com.johnturkson.sandbox

internal object EarthquakeJsonFixtures {
    fun read(name: String): String =
        checkNotNull(javaClass.getResource("/$name")) { "Missing earthquake fixture: $name" }
            .readText()
}
