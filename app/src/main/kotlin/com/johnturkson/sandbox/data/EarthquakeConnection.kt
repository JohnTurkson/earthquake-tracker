package com.johnturkson.sandbox.data

import kotlinx.coroutines.flow.StateFlow

interface EarthquakeConnection {
    val online: StateFlow<Boolean>

    fun isOnline(): Boolean
}
