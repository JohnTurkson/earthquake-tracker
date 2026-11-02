package com.johnturkson.sandbox.ui

import com.johnturkson.sandbox.data.EarthquakeLocation

data class EarthquakePreviewState(
    override val navKey: Destination.EarthquakeRoute = Destination.EarthquakeRoute,
    override val mapItems: List<EarthquakeMapItem> = EarthquakeMapFixtures.items,
    override val selectedMapItemId: String? = null,
    override val isListPanelVisible: Boolean = true,
    override val selectedView: EarthquakeView = EarthquakeView.Map,
    override val initialMapLocation: EarthquakeLocation? = EarthquakeLocation.Default,
    override val userLocation: EarthquakeLocation? = null,
) : EarthquakeState
