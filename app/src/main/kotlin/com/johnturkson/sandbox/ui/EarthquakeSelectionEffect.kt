package com.johnturkson.sandbox.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.runtime.result.LocalResultEventBus
import androidx.navigation3.runtime.result.ResultEffect

@Composable
internal fun EarthquakeSelectionEffect(
    resultKey: String,
    pending: EarthquakeSelectionResult?,
    onResult: (EarthquakeSelectionResult) -> Unit,
    onConsumed: (EarthquakeSelectionResult) -> Unit,
) {
    val bus = LocalResultEventBus.current
    ResultEffect<EarthquakeSelectionResult>(resultKey) {
        onResult(it)
        onConsumed(it)
    }
    LaunchedEffect(pending) {
        if (pending != null) {
            bus.sendResult(resultKey, pending)
        }
    }
}
