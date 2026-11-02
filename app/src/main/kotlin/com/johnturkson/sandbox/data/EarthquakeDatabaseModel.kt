package com.johnturkson.sandbox.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

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

@Entity(
    tableName = "earthquake_details",
    foreignKeys =
        [
            ForeignKey(
                entity = EarthquakeDatabaseModel::class,
                parentColumns = ["id"],
                childColumns = ["earthquakeId"],
                onDelete = ForeignKey.CASCADE,
            )
        ],
)
data class EarthquakeDetailDatabaseModel(
    @PrimaryKey val earthquakeId: String,
    val updated: Long,
    val productsJson: String,
)

data class EarthquakeWithDetailDatabaseModel(
    @Embedded val earthquake: EarthquakeDatabaseModel,
    @Relation(parentColumn = "id", entityColumn = "earthquakeId")
    val detail: EarthquakeDetailDatabaseModel?,
)

data class EarthquakeDetail(
    val earthquake: EarthquakeDatabaseModel,
    val products: Map<String, List<EarthquakeProductNetworkModel>>?,
    val detailUpdated: Long?,
) {
    val isDetailCurrent: Boolean
        get() = detailUpdated == earthquake.updated
}
