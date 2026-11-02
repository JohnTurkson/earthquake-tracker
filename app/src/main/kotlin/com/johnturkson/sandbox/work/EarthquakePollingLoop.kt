package com.johnturkson.sandbox.work

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull

class EarthquakePollingLoop(
    private val intervalMillis: Long = 60_000,
    private val elapsedMillis: () -> Long = { System.nanoTime() / 1_000_000 },
) {
    suspend fun run(
        online: StateFlow<Boolean>,
        onPaused: suspend () -> Unit,
        onTick: suspend () -> Unit,
    ) {
        while (currentCoroutineContext().isActive) {
            if (!online.value) {
                onPaused()
                online.first { it }
            }
            val started = elapsedMillis()
            onTick()
            if (!online.value) continue
            val waitMillis = (intervalMillis - (elapsedMillis() - started)).coerceAtLeast(1_000)
            withTimeoutOrNull(waitMillis) { online.first { !it } }
        }
    }
}
