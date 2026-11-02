package com.johnturkson.sandbox.ui

data class EarthquakeNavigationActions(
    val expanded: Boolean,
    val isListPanelVisible: Boolean,
    val notificationsVisible: Boolean,
    val onViewSelected: (EarthquakeView) -> Unit,
    val onListPanelVisibilityChanged: (Boolean) -> Unit,
    val onNotificationsClick: () -> Unit,
    val onNotificationsClose: () -> Unit,
    val onClusterVisibilityChanged: (Boolean) -> Unit,
    val onMapSelection: (String?) -> EarthquakeSelectionResult,
    val onListSelection: (String?) -> EarthquakeSelectionResult,
    val pendingMapSelection: EarthquakeSelectionResult?,
    val pendingListSelection: EarthquakeSelectionResult?,
    val onMapSelectionConsumed: (EarthquakeSelectionResult) -> Unit,
    val onListSelectionConsumed: (EarthquakeSelectionResult) -> Unit,
)
