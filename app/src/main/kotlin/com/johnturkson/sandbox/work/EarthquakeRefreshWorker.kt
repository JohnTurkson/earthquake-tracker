package com.johnturkson.sandbox.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.johnturkson.sandbox.data.EarthquakeRefreshResult
import com.johnturkson.sandbox.data.EarthquakeRefresher
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class EarthquakeRefreshWorker
@AssistedInject
constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val repository: EarthquakeRefresher,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val result = repository.refresh()
        val output = workDataOf(EarthquakeWorkScheduler.ResultKey to result.name)
        return if (result == EarthquakeRefreshResult.Success) Result.success(output)
        else Result.failure(output)
    }
}
