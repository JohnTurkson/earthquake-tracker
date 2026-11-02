package com.johnturkson.sandbox.ui

data class EarthquakePreviewState(
    override val navKey: Destination.EarthquakeRoute = Destination.EarthquakeRoute,
    override val mapItems: List<EarthquakeMapItem> = EarthquakeMapFixtures.items,
    override val selectedMapItemId: String? = null,
) : EarthquakeState
