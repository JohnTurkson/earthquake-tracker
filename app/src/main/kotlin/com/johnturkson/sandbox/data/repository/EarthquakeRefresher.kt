package com.johnturkson.sandbox.data.repository

interface EarthquakeRefresher {
    suspend fun refresh()

    suspend fun publishAlerts()
}
