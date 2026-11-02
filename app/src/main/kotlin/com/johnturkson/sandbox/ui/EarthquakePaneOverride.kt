package com.johnturkson.sandbox.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveComponentOverrideApi
import androidx.compose.material3.adaptive.layout.AnimatedPaneOverride
import androidx.compose.material3.adaptive.layout.AnimatedPaneOverrideScope
import androidx.compose.material3.adaptive.layout.AnimatedPaneScope
import androidx.compose.material3.adaptive.layout.LocalAnimatedPaneOverride
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.PaneMotion
import androidx.compose.material3.adaptive.layout.PaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneScaffoldValue
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldRole
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.unit.toRect
import com.johnturkson.sandbox.R

@OptIn(ExperimentalMaterial3AdaptiveComponentOverrideApi::class)
@Composable
fun EarthquakePaneOverrides(
    supportingPaneTitle: String? = null,
    content: @Composable () -> Unit,
) {
    val original = LocalAnimatedPaneOverride.current
    val override =
        remember(original, supportingPaneTitle) {
            EarthquakePaneOverride(original, supportingPaneTitle)
        }
    CompositionLocalProvider(LocalAnimatedPaneOverride provides override, content = content)
}

@OptIn(
    ExperimentalMaterial3AdaptiveApi::class,
    ExperimentalMaterial3AdaptiveComponentOverrideApi::class,
)
private class EarthquakePaneOverride(
    private val defaultOverride: AnimatedPaneOverride,
    private val supportingPaneTitle: String?,
) : AnimatedPaneOverride {
    @Composable
    override fun <
        Role : PaneScaffoldRole,
        ScaffoldValue : PaneScaffoldValue<Role>,
    > AnimatedPaneOverrideScope<Role, ScaffoldValue>.AnimatedPane() {
        if (
            scope.scaffoldStateTransition.targetState[scope.paneRole] !is PaneAdaptedValue.Levitated
        ) {
            with(defaultOverride) { this@AnimatedPane.AnimatedPane() }
            return
        }
        val title =
            if (scope.paneRole == SupportingPaneScaffoldRole.Extra) {
                stringResource(R.string.earthquake_alerts)
            } else {
                supportingPaneTitle ?: stringResource(R.string.recent_earthquakes)
            }
        val intBoundsSpec = boundsAnimationSpec
        val boundsSpec =
            remember(intBoundsSpec) {
                object : FiniteAnimationSpec<Rect> {
                    override fun <V : AnimationVector> vectorize(
                        converter: TwoWayConverter<Rect, V>
                    ) =
                        intBoundsSpec.vectorize(
                            TwoWayConverter(
                                convertToVector = { converter.convertToVector(it.toRect()) },
                                convertFromVector = {
                                    converter.convertFromVector(it).roundToIntRect()
                                },
                            )
                        )
                }
            }
        val paneModifier =
            modifier
                .then(
                    if (scope.paneMotion == PaneMotion.AnimateBounds) {
                        Modifier.animateBounds(scope, boundsTransform = { _, _ -> boundsSpec })
                    } else {
                        Modifier
                    }
                )
                .focusRequester(scope.focusRequesters.getValue(scope.paneRole))
                .focusGroup()
                .focusProperties {
                    if (!scope.isInteractable) {
                        canFocus = false
                        onEnter = { cancelFocusChange() }
                    }
                }
                .then(
                    if (scope.isInteractable) {
                        Modifier.semantics {
                            isTraversalGroup = true
                            paneTitle = title
                        }
                    } else {
                        Modifier.clearAndSetSemantics {}
                    }
                )
        scope.scaffoldStateTransition.AnimatedVisibility(
            visible = { it[scope.paneRole] != PaneAdaptedValue.Hidden },
            modifier = paneModifier,
            enter = enterTransition,
            exit = exitTransition,
        ) {
            scope.saveableStateHolder.SaveableStateProvider(scope.paneRole.toString()) {
                Box { content(AnimatedPaneScope.create(this@AnimatedVisibility)) }
            }
        }
    }
}
