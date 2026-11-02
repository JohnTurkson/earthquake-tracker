package com.johnturkson.sandbox.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.johnturkson.sandbox.data.EarthquakeConnection
import com.johnturkson.sandbox.data.EarthquakeNotifications
import com.johnturkson.sandbox.data.EarthquakeRefreshCoordinator
import com.johnturkson.sandbox.data.EarthquakeRefresher
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

@HiltWorker
class EarthquakeMonitoringWorker
@AssistedInject
constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val repository: EarthquakeRefresher,
    private val connectivity: EarthquakeConnection,
    private val notifications: EarthquakeNotifications,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        try {
            setForeground(notifications.foreground(id))
            EarthquakePollingLoop()
                .run(
                    online = connectivity.online,
                    onPaused = {
                        setProgress(
                            workDataOf(
                                EarthquakeWorkScheduler.ResultKey to
                                    EarthquakeRefreshCoordinator.Result.Offline.name
                            )
                        )
                        setForeground(notifications.foreground(id, offline = true))
                    },
                    onTick = {
                        setProgress(
                            workDataOf(
                                EarthquakeWorkScheduler.ResultKey to
                                    EarthquakeRefreshCoordinator.Result.Running.name
                            )
                        )
                        setForeground(notifications.foreground(id, refreshing = true))
                        val result = repository.refresh()
                        setProgress(workDataOf(EarthquakeWorkScheduler.ResultKey to result.name))
                        setForeground(
                            notifications.foreground(
                                id,
                                offline = result == EarthquakeRefreshCoordinator.Result.Offline,
                                failed =
                                    result in
                                        setOf(
                                            EarthquakeRefreshCoordinator.Result.NetworkError,
                                            EarthquakeRefreshCoordinator.Result.DataError,
                                            EarthquakeRefreshCoordinator.Result.StorageError,
                                        ),
                            )
                        )
                    },
                )
            return Result.success()
        } catch (error: CancellationException) {
            throw error
        } catch (_: IllegalStateException) {
            return Result.failure(
                workDataOf(EarthquakeWorkScheduler.ResultKey to "StartNotAllowed")
            )
        } catch (_: SecurityException) {
            return Result.failure(
                workDataOf(EarthquakeWorkScheduler.ResultKey to "StartNotAllowed")
            )
        }
    }
}
