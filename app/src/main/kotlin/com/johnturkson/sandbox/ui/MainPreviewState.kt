package com.johnturkson.sandbox.ui

data class MainPreviewState(
    override val navKey: Destination.MainRoute = Destination.MainRoute,
    override val mapItems: List<EarthquakeMapItem> = EarthquakeMapFixtures.items,
    override val selectedMapItemId: String? = null,
) : MainState
