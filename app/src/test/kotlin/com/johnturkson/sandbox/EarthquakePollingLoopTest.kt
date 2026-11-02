package com.johnturkson.sandbox

import com.johnturkson.sandbox.work.EarthquakePollingLoop
import kotlin.test.assertEquals
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class EarthquakePollingLoopTest {
    @Test
    fun pollsEveryMinuteAndCancellationStopsFutureChecks() = runTest {
        var checks = 0
        val job = backgroundScope.launch {
            EarthquakePollingLoop(timeSource = testScheduler.timeSource)
                .run(MutableStateFlow(true), {}, { checks++ })
        }
        runCurrent()
        assertEquals(1, checks)
        advanceTimeBy(59_999)
        assertEquals(1, checks)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(2, checks)
        job.cancel()
        job.join()
        advanceTimeBy(180_000)
        assertEquals(2, checks)
    }

    @Test
    fun pausesOfflineAndChecksImmediatelyOnReconnect() = runTest {
        val online = MutableStateFlow(false)
        var checks = 0
        var pauses = 0
        backgroundScope.launch {
            EarthquakePollingLoop(timeSource = testScheduler.timeSource)
                .run(online, { pauses++ }, { checks++ })
        }
        runCurrent()
        advanceTimeBy(180_000)
        assertEquals(0, checks)
        online.value = true
        runCurrent()
        assertEquals(1, checks)
        advanceTimeBy(10_000)
        online.value = false
        runCurrent()
        assertEquals(2, pauses)
        online.value = true
        runCurrent()
        assertEquals(2, checks)
    }

    @Test
    fun slowRequestsNeverOverlapOrProduceCatchUpBursts() = runTest {
        var checks = 0
        var active = 0
        var maximumActive = 0
        backgroundScope.launch {
            EarthquakePollingLoop(timeSource = testScheduler.timeSource)
                .run(
                    MutableStateFlow(true),
                    {},
                    {
                        checks++
                        active++
                        maximumActive = maxOf(maximumActive, active)
                        delay(90_000)
                        active--
                    },
                )
        }
        runCurrent()
        advanceTimeBy(90_000)
        runCurrent()
        assertEquals(1, checks)
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(2, checks)
        assertEquals(1, maximumActive)
    }
}
