package com.johnturkson.sandbox.data

import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EarthquakeCollectionNetworkModel(
    val type: String,
    val metadata: EarthquakeMetadataNetworkModel,
    val features: List<EarthquakeNetworkModel>,
    val bbox: List<Double>? = null,
)

@Serializable
data class EarthquakeMetadataNetworkModel(
    @Serializable(with = EarthquakeEpochMillisecondsSerializer::class) val generated: Instant,
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
    @Serializable(with = EarthquakeEpochMillisecondsSerializer::class) val time: Instant,
    @Serializable(with = EarthquakeEpochMillisecondsSerializer::class) val updated: Instant,
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
)

@Serializable
data class EarthquakeGeometryNetworkModel(val type: String, val coordinates: List<Double>)
