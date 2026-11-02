package com.johnturkson.sandbox.data

import io.ktor.resources.Resource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Resource("/earthquakes/feed/v1.0")
class EarthquakeResources {
    @Resource("summary/{feed}")
    data class Summary(
        val parent: EarthquakeResources = EarthquakeResources(),
        val feed: EarthquakeFeed = EarthquakeFeed.ALL_HOUR,
    )

    @Resource("detail/{file}")
    data class Detail(val parent: EarthquakeResources = EarthquakeResources(), val file: String)
}

@Serializable
enum class EarthquakeFeed {
    @SerialName("all_hour.geojson") ALL_HOUR,
    @SerialName("1.0_hour.geojson") MAGNITUDE_1_HOUR,
    @SerialName("2.5_hour.geojson") MAGNITUDE_2_5_HOUR,
    @SerialName("4.5_hour.geojson") MAGNITUDE_4_5_HOUR,
    @SerialName("significant_hour.geojson") SIGNIFICANT_HOUR,
    @SerialName("all_day.geojson") ALL_DAY,
    @SerialName("1.0_day.geojson") MAGNITUDE_1_DAY,
    @SerialName("2.5_day.geojson") MAGNITUDE_2_5_DAY,
    @SerialName("4.5_day.geojson") MAGNITUDE_4_5_DAY,
    @SerialName("significant_day.geojson") SIGNIFICANT_DAY,
    @SerialName("all_week.geojson") ALL_WEEK,
    @SerialName("1.0_week.geojson") MAGNITUDE_1_WEEK,
    @SerialName("2.5_week.geojson") MAGNITUDE_2_5_WEEK,
    @SerialName("4.5_week.geojson") MAGNITUDE_4_5_WEEK,
    @SerialName("significant_week.geojson") SIGNIFICANT_WEEK,
    @SerialName("all_month.geojson") ALL_MONTH,
    @SerialName("1.0_month.geojson") MAGNITUDE_1_MONTH,
    @SerialName("2.5_month.geojson") MAGNITUDE_2_5_MONTH,
    @SerialName("4.5_month.geojson") MAGNITUDE_4_5_MONTH,
    @SerialName("significant_month.geojson") SIGNIFICANT_MONTH,
}

@Resource("/fdsnws/event/1")
class EarthquakeCatalogResources {
    @Resource("query")
    data class Query(
        val parent: EarthquakeCatalogResources = EarthquakeCatalogResources(),
        @SerialName("starttime") val startTime: String? = null,
        @SerialName("endtime") val endTime: String? = null,
        @SerialName("updatedafter") val updatedAfter: String? = null,
        val limit: Int = 20000,
        val offset: Int = 1,
        val format: String = "geojson",
        val nodata: Int = 204,
    ) {
        init {
            require(limit in 1..20000)
            require(offset >= 1)
            require(format == "geojson")
            require(nodata == 204)
        }
    }
}
