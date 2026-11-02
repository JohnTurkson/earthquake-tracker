package com.johnturkson.sandbox

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import com.google.android.gms.maps.MapView
import com.johnturkson.sandbox.app.EarthquakeActivity
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test

class EarthquakeActivityTest {
    @get:Rule val composeRule = createAndroidComposeRule<EarthquakeActivity>()

    @Test
    fun earthquakeActivityStartsAndRecreates() {
        onView(isAssignableFrom(MapView::class.java)).check(matches(isDisplayed()))
        assertEquals(Lifecycle.State.RESUMED, composeRule.activityRule.scenario.state)

        composeRule.activityRule.scenario.recreate()

        onView(isAssignableFrom(MapView::class.java)).check(matches(isDisplayed()))
        assertEquals(Lifecycle.State.RESUMED, composeRule.activityRule.scenario.state)
    }
}
