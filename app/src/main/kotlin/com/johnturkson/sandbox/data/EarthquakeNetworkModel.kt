package com.johnturkson.sandbox.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class EarthquakeCollectionNetworkModel(
    val type: String,
    val metadata: EarthquakeMetadataNetworkModel,
    val features: List<EarthquakeNetworkModel>,
    val bbox: List<Double>? = null,
)

@Serializable
data class EarthquakeMetadataNetworkModel(
    val generated: Long,
    val url: String,
    val title: String,
    val status: Int,
    val api: String,
    val count: Int,
)

@Serializable
data class EarthquakeNetworkModel(
    override val id: String,
    val type: String,
    val properties: EarthquakePropertiesNetworkModel,
    val geometry: EarthquakeGeometryNetworkModel,
) : Earthquake

@Serializable
data class EarthquakePropertiesNetworkModel(
    val time: Long,
    val updated: Long,
    @SerialName("mag") val magnitude: Double? = null,
    val place: String? = null,
    val tz: Int? = null,
    val url: String? = null,
    val detail: String? = null,
    val felt: Int? = null,
    val cdi: Double? = null,
    val mmi: Double? = null,
    val alert: String? = null,
    val status: String? = null,
    val tsunami: Int? = null,
    @SerialName("sig") val significance: Int? = null,
    @SerialName("net") val network: String? = null,
    val code: String? = null,
    val ids: String? = null,
    val sources: String? = null,
    val types: String? = null,
    @SerialName("nst") val stationCount: Int? = null,
    @SerialName("dmin") val minimumDistance: Double? = null,
    val rms: Double? = null,
    val gap: Double? = null,
    @SerialName("magType") val magnitudeType: String? = null,
    val type: String? = null,
    val title: String? = null,
    val products: Map<String, List<EarthquakeProductNetworkModel>>? = null,
)

@Serializable
data class EarthquakeGeometryNetworkModel(val type: String, val coordinates: List<Double>)

@Serializable
data class EarthquakeProductNetworkModel(
    val id: String,
    val type: String,
    val code: String,
    val source: String,
    val updateTime: Long,
    val status: String,
    val properties: JsonObject,
    val preferredWeight: Long,
    val contents: Map<String, EarthquakeContentNetworkModel>,
    val indexTime: Long? = null,
    @SerialName("indexid") val indexId: Long? = null,
)

@Serializable
data class EarthquakeContentNetworkModel(
    val contentType: String,
    val lastModified: Long,
    val length: Long,
    val url: String? = null,
    val bytes: String? = null,
    val sha256: String? = null,
)
