package com.johnturkson.sandbox.data

fun EarthquakeNetworkModel.toDatabaseModel(): EarthquakeDatabaseModel {
    require(id.isNotBlank())
    require(type == "Feature")
    require(geometry.type == "Point")
    require(geometry.coordinates.size == 3 && geometry.coordinates.all { it.isFinite() })
    val (longitude, latitude, depth) = geometry.coordinates
    require(longitude in -180.0..180.0 && latitude in -90.0..90.0)
    return with(properties) {
        EarthquakeDatabaseModel(
            id = id,
            time = time,
            updated = updated,
            longitude = longitude,
            latitude = latitude,
            depth = depth,
            magnitude = magnitude,
            place = place,
            tz = tz,
            url = url,
            detail = detail,
            felt = felt,
            cdi = cdi,
            mmi = mmi,
            alert = alert,
            status = status,
            tsunami = tsunami,
            significance = significance,
            network = network,
            code = code,
            ids = ids,
            sources = sources,
            types = types,
            stationCount = stationCount,
            minimumDistance = minimumDistance,
            rms = rms,
            gap = gap,
            magnitudeType = magnitudeType,
            type = type,
            title = title,
        )
    }
}

internal fun EarthquakeDatabaseModel.aliasIds(): List<String> {
    return (listOf(id) + ids.orEmpty().split(',').map { it.trim() })
        .filter { it.isNotEmpty() }
        .distinct()
}
