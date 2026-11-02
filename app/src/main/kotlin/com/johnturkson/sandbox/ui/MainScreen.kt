package com.johnturkson.sandbox.ui

import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.navigation3.runtime.result.LocalResultEventBus

@Composable
fun MainScreen(
    navKey: Destination.MainRoute,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel<MainViewModel, MainViewModel.Factory>(
        creationCallback = { factory -> factory.create(navKey) },
    ),
) {
    LifecycleStartEffect(Unit) {
        viewModel.startTimerUpdates()
        onStopOrDispose {
            viewModel.pauseTimerUpdates()
        }
    }

    val navigationResults = LocalResultEventBus.current
    Scaffold(
        modifier = modifier,
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    if (viewModel.timers.isNotEmpty()) {
                        Button(
                            onClick = viewModel::toggleAllTimers,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Toggle all timers")
                        }
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    Button(onClick = { viewModel.addTimer() }) {
                        Text("+1")
                    }
                    Button(onClick = { repeat(10) { viewModel.addTimer() } }) {
                        Text("+10")
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    navigationResults.sendResult(
                        "addTimerDetails",
                        Unit,
                    )
                },
            ) {
                Icon(
                    Icons.Default.Add,
                    "Start new timer",
                )
            }
        },
    ) { innerPadding ->
        val lazyListState = rememberLazyListState()
        val f=  snapshotFlow {  lazyListState.layoutInfo.visibleItemsInfo }
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            state = lazyListState,
            contentPadding = PaddingValues(bottom = 72.dp),
        ) {
            // TODO default state
            items(viewModel.timers) { timer ->
                Timer(timer, viewModel::toggleTimer)
            }
        }
    }
}

@Composable
fun Timer(
    timer: Timer,
    onClick: (Timer) -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = LocalLocale.current
    val formatter = remember(locale) {
        MeasureFormat.getInstance(locale.platformLocale, MeasureFormat.FormatWidth.NUMERIC)
    }
    val formattedTime by remember(timer, formatter) {
        derivedStateOf {
            timer.duration.toComponents { hours, minutes, seconds, _ ->
                formatter.formatMeasures(
                    Measure(hours, MeasureUnit.HOUR),
                    Measure(minutes, MeasureUnit.MINUTE),
                    Measure(seconds, MeasureUnit.SECOND),
                )
            }
        }
    }

    Surface(onClick = { onClick(timer) }, modifier = modifier) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(formattedTime)
            Spacer(Modifier.weight(1f))
            Switch(checked = timer.isRunning, null)
        }
    }
}

// @Preview
// @Composable
// private fun MainScreenPreview() {
//     val state = MainPreviewState
//     MainScreen(navKey = state.navKey, state = state)
// }
