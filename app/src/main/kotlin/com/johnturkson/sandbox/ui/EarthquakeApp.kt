package com.johnturkson.sandbox.ui

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.result.rememberResultEventBusNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.johnturkson.sandbox.theme.EarthquakeTheme
import com.johnturkson.sandbox.ui.Destination.EarthquakeRoute

@Composable
fun EarthquakeApp(
    modifier: Modifier = Modifier,
    notificationEventId: String? = null,
    onNotificationEventConsumed: () -> Unit = {},
) {
    EarthquakeTheme {
        val backStack = rememberNavBackStack(EarthquakeRoute)
        NavDisplay(
            backStack = backStack,
            modifier = modifier,
            entryDecorators =
                listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                    rememberResultEventBusNavEntryDecorator(),
                ),
            transitionSpec = {
                ContentTransform(
                    EnterTransition.None,
                    ExitTransition.None,
                )
            },
            popTransitionSpec = {
                ContentTransform(
                    EnterTransition.None,
                    ExitTransition.None,
                )
            },
            predictivePopTransitionSpec = {
                ContentTransform(
                    EnterTransition.None,
                    ExitTransition.None,
                )
            },
            entryProvider =
                entryProvider {
                    entry<EarthquakeRoute> { navKey ->
                        EarthquakeScreen(
                            navKey = navKey,
                            notificationEventId = notificationEventId,
                            onNotificationEventConsumed = onNotificationEventConsumed,
                        )
                    }
                },
        )
    }
}
