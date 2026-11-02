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
import com.johnturkson.sandbox.theme.SandboxTheme
import com.johnturkson.sandbox.ui.Destination.MainRoute

@Composable
fun MainContent(modifier: Modifier = Modifier) {
    SandboxTheme {
        val backStack = rememberNavBackStack(MainRoute)
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
                entryProvider { entry<MainRoute> { navKey -> MainScreen(navKey = navKey) } },
        )
    }
}
