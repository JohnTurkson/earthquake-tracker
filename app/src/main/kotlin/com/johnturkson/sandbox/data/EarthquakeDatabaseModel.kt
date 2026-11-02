package com.johnturkson.sandbox.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "earthquakes", indices = [Index("time"), Index("updated")])
data class EarthquakeDatabaseModel(
    @PrimaryKey override val id: String,
    val time: Long,
    val updated: Long,
    val longitude: Double,
    val latitude: Double,
    val depth: Double,
    val magnitude: Double?,
    val place: String?,
    val tz: Int?,
    val url: String?,
    val detail: String?,
    val felt: Int?,
    val cdi: Double?,
    val mmi: Double?,
    val alert: String?,
    val status: String?,
    val tsunami: Int?,
    val significance: Int?,
    val network: String?,
    val code: String?,
    val ids: String?,
    val sources: String?,
    val types: String?,
    val stationCount: Int?,
    val minimumDistance: Double?,
    val rms: Double?,
    val gap: Double?,
    val magnitudeType: String?,
    val type: String?,
    val title: String?,
) : Earthquake

@Entity(
    tableName = "earthquake_aliases",
    foreignKeys =
        [
            ForeignKey(
                entity = EarthquakeDatabaseModel::class,
                parentColumns = ["id"],
                childColumns = ["earthquakeId"],
                onDelete = ForeignKey.CASCADE,
            )
        ],
    indices = [Index("earthquakeId")],
)
data class EarthquakeAliasDatabaseModel(@PrimaryKey val id: String, val earthquakeId: String)
