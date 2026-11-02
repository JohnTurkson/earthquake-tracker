package com.johnturkson.sandbox.data

import com.johnturkson.sandbox.annotations.IoDispatcher
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class EarthquakeRepository
@Inject
constructor(
    private val api: EarthquakeApi,
    private val dao: EarthquakeDao,
    private val json: Json,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    fun getEarthquakesFlow(
        startTime: Long = Long.MIN_VALUE,
        endTime: Long = Long.MAX_VALUE,
        minimumMagnitude: Double? = null,
    ): Flow<List<EarthquakeDatabaseModel>> {
        return dao.getEarthquakesFlow(startTime, endTime, minimumMagnitude)
    }

    fun getEarthquakeFlow(id: String): Flow<EarthquakeDatabaseModel?> {
        return dao.getEarthquakeFlow(id)
    }

    fun getEarthquakeDetailFlow(id: String): Flow<EarthquakeDetail?> {
        return dao.getEarthquakeDetailFlow(id)
            .map { cached ->
                cached?.let {
                    EarthquakeDetail(
                        earthquake = it.earthquake,
                        products =
                            it.detail?.let { detail -> json.decodeFromString(detail.productsJson) },
                        detailUpdated = it.detail?.updated,
                    )
                }
            }
            .flowOn(ioDispatcher)
    }

    suspend fun refreshEarthquakes(feed: EarthquakeFeed = EarthquakeFeed.ALL_HOUR) {
        withContext(ioDispatcher) { storeCollection(api.getEarthquakes(feed)) }
    }

    suspend fun refreshEarthquake(id: String) {
        withContext(ioDispatcher) {
            val response = api.getEarthquake(id)
            val earthquake = response.toDatabaseModel()
            val products = requireNotNull(response.properties.products)
            dao.mergeEarthquake(
                earthquake,
                EarthquakeDetailDatabaseModel(
                    earthquakeId = earthquake.id,
                    updated = earthquake.updated,
                    productsJson = json.encodeToString(products),
                ),
            )
        }
    }

    suspend fun refreshQuery(query: EarthquakeCatalogResources.Query) {
        withContext(ioDispatcher) { api.queryEarthquakes(query)?.let { storeCollection(it) } }
    }

    private suspend fun storeCollection(response: EarthquakeCollectionNetworkModel) {
        require(response.type == "FeatureCollection")
        require(response.metadata.status == 200)
        dao.mergeEarthquakes(response.features.map { it.toDatabaseModel() })
    }
}
