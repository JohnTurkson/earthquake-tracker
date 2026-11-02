package com.johnturkson.sandbox.ui

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

internal data class EarthquakeClusterItem(val item: EarthquakeMapItem) : ClusterItem {
    override val position: LatLng = LatLng(item.latitude, item.longitude)
    override val title: String = item.title
    override val snippet: String? = item.snippet
    override val zIndex: Float = 0f
}
