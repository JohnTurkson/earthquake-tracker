package com.johnturkson.sandbox.work

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull

class EarthquakePollingLoop(
    private val interval: Duration = 1.minutes,
    private val timeSource: TimeSource = TimeSource.Monotonic,
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
            val started = timeSource.markNow()
            onTick()
            if (!online.value) continue
            val wait = (interval - started.elapsedNow()).coerceAtLeast(1.seconds)
            withTimeoutOrNull(wait) { online.first { !it } }
        }
    }
}
