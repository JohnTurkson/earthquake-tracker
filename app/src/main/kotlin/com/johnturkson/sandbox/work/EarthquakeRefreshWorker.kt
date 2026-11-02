package com.johnturkson.sandbox.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.johnturkson.sandbox.data.EarthquakeRefresher
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

@HiltWorker
class EarthquakeRefreshWorker
@AssistedInject
constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val repository: EarthquakeRefresher,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        repository.refresh()
        try {
            repository.publishAlerts()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {}
        return Result.success()
    }
}
