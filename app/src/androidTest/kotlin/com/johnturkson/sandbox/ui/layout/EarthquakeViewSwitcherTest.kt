package com.johnturkson.sandbox.ui.layout

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.johnturkson.sandbox.theme.EarthquakeTheme
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

class EarthquakeViewSwitcherTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun explorerCentersSwitcherAndKeepsBellAtTheLogicalEndAcrossLayouts() {
        var expanded by mutableStateOf(false)
        var panelVisible by mutableStateOf(true)
        var direction by mutableStateOf(LayoutDirection.Ltr)
        var notificationClicks = 0
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                EarthquakeTheme(dynamicColor = false) {
                    EarthquakeControls(
                        items = emptyList(),
                        selectedItemId = null,
                        selectedView = EarthquakeView.Map,
                        expanded = expanded,
                        onItemClick = {},
                        onClearSelection = {},
                        onViewSelected = {},
                        isListPanelVisible = panelVisible,
                        onNotificationsClick = { notificationClicks++ },
                    )
                }
            }
        }

        for (layoutDirection in LayoutDirection.entries) {
            rule.runOnIdle {
                direction = layoutDirection
                expanded = false
            }
            val root = rule.onRoot().fetchSemanticsNode().boundsInRoot
            val map =
                rule
                    .onNode(hasText("Map") or hasContentDescription("Map"))
                    .fetchSemanticsNode()
                    .boundsInRoot
            val list =
                rule
                    .onNode(hasText("List") or hasContentDescription("List"))
                    .fetchSemanticsNode()
                    .boundsInRoot
            val bell =
                rule
                    .onNodeWithContentDescription("Earthquake alerts")
                    .fetchSemanticsNode()
                    .boundsInRoot
            assertEquals(
                root.center.x,
                (minOf(map.left, list.left) + maxOf(map.right, list.right)) / 2,
                1f,
            )
            assertTrue(
                if (layoutDirection == LayoutDirection.Ltr) {
                    list.right < bell.left
                } else {
                    list.left > bell.right
                }
            )
            for ((isExpanded, isPanelVisible) in
                listOf(false to true, true to true, true to false)) {
                rule.runOnIdle {
                    expanded = isExpanded
                    panelVisible = isPanelVisible
                }
                val bounds =
                    rule
                        .onNodeWithContentDescription("Earthquake alerts")
                        .assertIsDisplayed()
                        .fetchSemanticsNode()
                        .boundsInRoot
                val margin =
                    with(rule.density) {
                        (if (expanded) {
                                8.dp
                            } else {
                                16.dp
                            })
                            .toPx()
                    }
                if (layoutDirection == LayoutDirection.Ltr) {
                    assertEquals(root.right - margin, bounds.right, 1f)
                } else {
                    assertEquals(root.left + margin, bounds.left, 1f)
                }
                rule.onNodeWithContentDescription("Earthquake alerts").performClick()
            }
        }
        rule.runOnIdle { assertEquals(6, notificationClicks) }
    }

    @Test
    fun resizingUsesNamedIconsAndKeepsSelection() {
        var width by mutableStateOf(280.dp)
        var selectedView by mutableStateOf(EarthquakeView.Map)
        rule.setContent {
            EarthquakeTheme(dynamicColor = false) {
                Box(Modifier.width(width)) {
                    EarthquakeViewSwitcher(selectedView, { selectedView = it })
                }
            }
        }

        rule.onNodeWithText("Map").assertIsSelected()
        rule.onNodeWithText("List").assertIsDisplayed()
        rule.runOnIdle { width = 160.dp }
        rule.onNodeWithText("Map").assertDoesNotExist()
        rule.onNodeWithText("List").assertDoesNotExist()
        rule.onNodeWithContentDescription("Map").assertIsSelected()
        rule.onNodeWithContentDescription("List").assertIsDisplayed().performClick()
        rule.onNodeWithContentDescription("List").assertIsSelected()
        rule.runOnIdle {
            assertEquals(EarthquakeView.List, selectedView)
            width = 280.dp
        }
        rule.onNodeWithText("List").assertIsSelected()
        rule.onNodeWithText("Map").performClick()
        rule.runOnIdle { assertEquals(EarthquakeView.Map, selectedView) }
    }
}
