package com.johnturkson.sandbox

import com.johnturkson.sandbox.data.EarthquakeDatabaseModel
import com.johnturkson.sandbox.data.EarthquakeRefreshCoordinator
import java.io.IOException
import kotlin.test.assertEquals
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class EarthquakeRefreshCoordinatorTest {
    private val item = EarthquakeMonitoringFixtures.item(id = "one", time = 100, updated = 100)

    @Test
    fun offlineFailsWithoutNetworkOrStorageCalls() = runTest {
        val coordinator =
            EarthquakeRefreshCoordinator(
                { false },
                { error("fetch") },
                { _, _ -> error("save") },
                { 200 },
            )
        assertEquals(EarthquakeRefreshCoordinator.Result.Offline, coordinator.refresh())
    }

    @Test
    fun overlappingRefreshesCoalesceAndSaveBeforeAlerting() = runTest {
        val response = CompletableDeferred<List<EarthquakeDatabaseModel>>()
        val operations = mutableListOf<String>()
        val coordinator =
            EarthquakeRefreshCoordinator(
                { true },
                {
                    operations += "fetch"
                    response.await()
                },
                { items, time ->
                    assertEquals(listOf(item), items)
                    assertEquals(200, time)
                    operations += "save"
                },
                { 200 },
                { _, _ -> operations += "alert" },
            )
        val first = async { coordinator.refresh() }
        val second = async { coordinator.refresh() }
        runCurrent()
        assertEquals(EarthquakeRefreshCoordinator.Result.Running, coordinator.status.value)
        response.complete(listOf(item))
        assertEquals(
            listOf(
                EarthquakeRefreshCoordinator.Result.Success,
                EarthquakeRefreshCoordinator.Result.Success,
            ),
            awaitAll(first, second),
        )
        assertEquals(listOf("fetch", "save", "alert"), operations)
        coordinator.refresh()
        assertEquals(2, operations.count { it == "fetch" })
    }

    @Test
    fun networkAndDecodeFailuresLeaveCacheUntouched() = runTest {
        listOf(
                IOException() to EarthquakeRefreshCoordinator.Result.NetworkError,
                SerializationException() to EarthquakeRefreshCoordinator.Result.DataError,
            )
            .forEach { (failure, expected) ->
                val coordinator =
                    EarthquakeRefreshCoordinator(
                        { true },
                        { throw failure },
                        { _, _ -> error("must retain cache") },
                        { 200 },
                    )
                assertEquals(expected, coordinator.refresh())
            }
    }

    @Test
    fun networkLossDuringFetchIsOffline() = runTest {
        var online = true
        val coordinator =
            EarthquakeRefreshCoordinator(
                { online },
                {
                    online = false
                    throw IOException()
                },
                { _, _ -> error("save") },
                { 200 },
            )
        assertEquals(EarthquakeRefreshCoordinator.Result.Offline, coordinator.refresh())
    }

    @Test
    fun storageFailureDoesNotPublishAlerts() = runTest {
        val coordinator =
            EarthquakeRefreshCoordinator(
                { true },
                { listOf(item) },
                { _, _ -> throw IOException() },
                { 200 },
                { _, _ -> error("alert") },
            )
        assertEquals(EarthquakeRefreshCoordinator.Result.StorageError, coordinator.refresh())
    }

    @Test
    fun notificationFailureDoesNotUndoSuccessfulRefresh() = runTest {
        var saved = false
        val coordinator =
            EarthquakeRefreshCoordinator(
                { true },
                { listOf(item) },
                { _, _ -> saved = true },
                { 200 },
                { _, _ -> throw SecurityException() },
            )
        assertEquals(EarthquakeRefreshCoordinator.Result.Success, coordinator.refresh())
        assertEquals(true, saved)
    }

    @Test
    fun cancellationReleasesCoordinatorForNextRefresh() = runTest {
        var fetches = 0
        val response = CompletableDeferred<List<EarthquakeDatabaseModel>>()
        val coordinator =
            EarthquakeRefreshCoordinator(
                { true },
                { if (++fetches == 1) response.await() else listOf(item) },
                { _, _ -> },
                { 200 },
            )
        val first = async { coordinator.refresh() }
        runCurrent()
        first.cancel()
        first.join()
        assertEquals(EarthquakeRefreshCoordinator.Result.Idle, coordinator.status.value)
        assertEquals(EarthquakeRefreshCoordinator.Result.Success, coordinator.refresh())
    }
}
