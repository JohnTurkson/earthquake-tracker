package com.johnturkson.sandbox

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.StateRestorationTester
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class MainComposeTest {
    @Test
    fun uiTest() = runComposeUiTest { setContent {} }

    @Test
    fun uiTestRestoration() = runComposeUiTest {
        val stateRestorationTester = StateRestorationTester(this)
        stateRestorationTester.setContent {}

        stateRestorationTester.emulateSaveAndRestore()
    }
}
