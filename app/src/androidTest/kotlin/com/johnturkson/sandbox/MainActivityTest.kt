package com.johnturkson.sandbox

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.lifecycle.Lifecycle
import com.johnturkson.sandbox.app.MainActivity
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test

class MainActivityTest {
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun mainActivityStartsAndRecreates() {
        composeRule.onRoot().assertExists()
        assertEquals(Lifecycle.State.RESUMED, composeRule.activityRule.scenario.state)

        composeRule.activityRule.scenario.recreate()

        composeRule.onRoot().assertExists()
        assertEquals(Lifecycle.State.RESUMED, composeRule.activityRule.scenario.state)
    }
}
