package com.johnturkson.sandbox.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.plugins.resources.get
import io.ktor.http.HttpStatusCode
import javax.inject.Inject

class EarthquakeApi @Inject constructor(private val client: HttpClient) {
    suspend fun getEarthquakes(
        feed: EarthquakeFeed = EarthquakeFeed.ALL_HOUR
    ): EarthquakeCollectionNetworkModel {
        return client.get(EarthquakeResources.Summary(feed = feed)) { expectSuccess = true }.body()
    }

    suspend fun getEarthquake(id: String): EarthquakeNetworkModel {
        require(id.isNotBlank() && id.all { it.isLetterOrDigit() || it == '-' || it == '_' })
        return client
            .get(EarthquakeResources.Detail(file = "$id.geojson")) { expectSuccess = true }
            .body()
    }

    suspend fun queryEarthquakes(
        query: EarthquakeCatalogResources.Query
    ): EarthquakeCollectionNetworkModel? {
        val response = client.get(query) { expectSuccess = true }
        return if (response.status == HttpStatusCode.NoContent) null else response.body()
    }
}
