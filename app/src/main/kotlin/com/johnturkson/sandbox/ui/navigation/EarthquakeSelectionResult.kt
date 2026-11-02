package com.johnturkson.sandbox.ui.navigation

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class EarthquakeSelectionResult(val earthquakeId: String?, val revision: Long)
