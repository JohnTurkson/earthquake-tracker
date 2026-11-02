package com.johnturkson.sandbox.data

interface EarthquakeRefresher {
    suspend fun refresh(): EarthquakeRefreshResult
}
