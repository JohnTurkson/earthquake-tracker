package com.johnturkson.sandbox.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.johnturkson.sandbox.data.EarthquakeConnection
import com.johnturkson.sandbox.data.EarthquakeNotifications
import com.johnturkson.sandbox.data.EarthquakeRefreshResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

@HiltWorker
class EarthquakeMonitoringWorker
@AssistedInject
constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val scheduler: EarthquakeWorkScheduler,
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
                                    EarthquakeRefreshResult.Offline.name
                            )
                        )
                        setForeground(notifications.foreground(id, offline = true))
                    },
                    onTick = {
                        setProgress(workDataOf(EarthquakeWorkScheduler.ResultKey to "Running"))
                        setForeground(notifications.foreground(id, refreshing = true))
                        val work = scheduler.refreshAndAwait()
                        val result = work.outputData.getString(EarthquakeWorkScheduler.ResultKey)
                        setProgress(work.outputData)
                        setForeground(
                            notifications.foreground(
                                id,
                                offline = result == EarthquakeRefreshResult.Offline.name,
                                failed =
                                    work.state != WorkInfo.State.SUCCEEDED &&
                                        result != EarthquakeRefreshResult.Offline.name,
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
