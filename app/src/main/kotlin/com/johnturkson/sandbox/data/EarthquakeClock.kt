package com.johnturkson.sandbox.data

import javax.inject.Inject

class EarthquakeClock @Inject constructor() {
    fun nowMillis(): Long = System.currentTimeMillis()
}
