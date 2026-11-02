package com.johnturkson.sandbox.data

import io.ktor.client.call.NoTransformationFoundException
import io.ktor.serialization.JsonConvertException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException

class EarthquakeRefreshCoordinator(
    private val isOnline: () -> Boolean,
    private val fetch: suspend () -> List<EarthquakeDatabaseModel>,
    private val save: suspend (List<EarthquakeDatabaseModel>, Long) -> Unit,
    private val nowMillis: () -> Long,
    private val afterSave: suspend (List<EarthquakeDatabaseModel>, Long) -> Unit = { _, _ -> },
) {
    enum class Result {
        Idle,
        Running,
        Success,
        Offline,
        NetworkError,
        DataError,
        StorageError,
    }

    private val mutex = Mutex()
    @Volatile private var generation = 0L
    private val mutableStatus = MutableStateFlow(Result.Idle)
    val status = mutableStatus.asStateFlow()

    suspend fun refresh(): Result {
        val requestedGeneration = generation
        return mutex.withLock {
            if (generation != requestedGeneration) return@withLock mutableStatus.value
            if (!isOnline()) return@withLock finish(Result.Offline)
            mutableStatus.value = Result.Running
            try {
                val items =
                    try {
                        fetch()
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: JsonConvertException) {
                        return@withLock finish(if (isOnline()) Result.DataError else Result.Offline)
                    } catch (_: NoTransformationFoundException) {
                        return@withLock finish(if (isOnline()) Result.DataError else Result.Offline)
                    } catch (_: SerializationException) {
                        return@withLock finish(if (isOnline()) Result.DataError else Result.Offline)
                    } catch (_: IllegalArgumentException) {
                        return@withLock finish(if (isOnline()) Result.DataError else Result.Offline)
                    } catch (_: Exception) {
                        return@withLock finish(
                            if (isOnline()) Result.NetworkError else Result.Offline
                        )
                    }
                val savedAt = nowMillis()
                try {
                    save(items, savedAt)
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    return@withLock finish(Result.StorageError)
                }
                try {
                    afterSave(items, savedAt)
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {}
                finish(Result.Success)
            } finally {
                if (mutableStatus.value == Result.Running) mutableStatus.value = Result.Idle
            }
        }
    }

    private fun finish(result: Result): Result {
        generation++
        mutableStatus.value = result
        return result
    }
}
