package com.johnturkson.sandbox.ui

import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer
import androidx.savedstate.compose.serialization.serializers.SnapshotStateListSerializer
import com.johnturkson.sandbox.annotations.DefaultDispatcher
import com.johnturkson.sandbox.data.MainRepository
import com.johnturkson.sandbox.ui.MainViewModel.Factory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@HiltViewModel(assistedFactory = Factory::class)
class MainViewModel @AssistedInject constructor(
    @Assisted override val navKey: Destination.MainRoute,
    private val savedStateHandle: SavedStateHandle,
    private val mainRepository: MainRepository,
    private val frameClock: MonotonicFrameClock,
    private val timeSource: TimeSource,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel(), MainState {
    // val timers by savedStateHandle.saved(SnapshotStateListSerializer()) {
    //     mutableStateListOf<MutableTimer>()
    // }
    val timers: List<Timer> field = savedStateHandle.saved(
        serializer = SnapshotStateListSerializer<MutableTimer>(),
    ) {
        mutableStateListOf()
    }.getValue(this, this::timers)
    private val runningTimers by derivedStateOf { timers.filter { timer -> timer.isRunning } }
    private var timerJob: Job? = null

    fun addTimer() {
        val timer = MutableTimer(
            id = Uuid.random(),
            duration = 5.seconds + 500.milliseconds,
            isRunning = false,
        )
        timers += timer
    }

    fun toggleAllTimers() {
        if (runningTimers.isNotEmpty()) {
            timers.forEach { timer -> timer.isRunning = false }
        } else {
            timers.forEach { timer -> timer.isRunning = true }
        }
    }

    fun toggleTimer(timer: Timer) {
        if (timer is MutableTimer) timer.isRunning = !timer.isRunning
    }

    fun startTimerUpdates() {
        if (timerJob != null) return
        timerJob = viewModelScope.launch {
            withContext(defaultDispatcher) {
                var now = timeSource.markNow()
                while (coroutineContext.isActive) {
                    frameClock.withFrameNanos { _ ->
                        val elapsed = now.elapsedNow()
                        now += elapsed
                        // TODO move into background
                        Snapshot.withMutableSnapshot {
                            runningTimers.forEach { timer ->
                                val delta = timer.duration - elapsed
                                timer.duration = delta.coerceAtLeast(Duration.ZERO)
                            }
                        }
                    }
                }
            }
        }
    }

    fun pauseTimerUpdates() {
        timerJob?.cancel()
    }

    @AssistedFactory
    interface Factory {
        fun create(navKey: Destination.MainRoute): MainViewModel
    }
}

// TODO remodel timer paradigm to keep track of an explicit endtime when active
//  and render current time vs end time (via updates in withFrameNanos in UI for example)
//  this way viewmodel may not need to update it continually within function
@Stable
interface Timer {
    val id: Uuid
    val duration: Duration
    val isRunning: Boolean
}

@Serializable
class MutableTimer private constructor(
    override val id: Uuid,
    @Serializable(with = MutableStateSerializer::class)
    private val durationState: MutableState<Duration>,
    @Serializable(with = MutableStateSerializer::class)
    private val isRunningState: MutableState<Boolean>,
) : Timer {
    constructor(id: Uuid, duration: Duration, isRunning: Boolean) : this(
        id = id,
        durationState = mutableStateOf(duration),
        isRunningState = mutableStateOf(isRunning),
    )

    override var duration by durationState
    override var isRunning by isRunningState
}
