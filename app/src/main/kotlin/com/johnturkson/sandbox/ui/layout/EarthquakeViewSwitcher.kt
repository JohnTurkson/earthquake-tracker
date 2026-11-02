package com.johnturkson.sandbox.ui.layout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.johnturkson.sandbox.R

@Composable
fun EarthquakeViewSwitcher(
    selectedView: EarthquakeView,
    onViewSelected: (EarthquakeView) -> Unit,
    modifier: Modifier = Modifier,
) {
    val labels =
        EarthquakeView.entries.map { view ->
            stringResource(
                if (view == EarthquakeView.Map) {
                    R.string.map_view
                } else {
                    R.string.list_view
                }
            )
        }
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelLarge
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
    ) {
        BoxWithConstraints {
            val labelWidth = labels.maxOf { label ->
                with(density) {
                    textMeasurer.measure(label, labelStyle, softWrap = false).size.width.toDp()
                }
            }
            val buttonPadding = SegmentedButtonDefaults.ContentPadding
            val labeledButtonWidth =
                labelWidth +
                    32.dp +
                    buttonPadding.calculateLeftPadding(layoutDirection) +
                    buttonPadding.calculateRightPadding(layoutDirection)
            val showLabels = maxWidth >= labeledButtonWidth * labels.size
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth(), space = 0.dp) {
                EarthquakeView.entries.forEachIndexed { index, view ->
                    val imageVector =
                        if (view == EarthquakeView.Map) {
                            Icons.Outlined.Map
                        } else {
                            Icons.AutoMirrored.Outlined.List
                        }
                    SegmentedButton(
                        selected = selectedView == view,
                        onClick = { onViewSelected(view) },
                        shape =
                            SegmentedButtonDefaults.itemShape(index, EarthquakeView.entries.size),
                        modifier = Modifier.heightIn(min = 48.dp),
                        border = BorderStroke(0.dp, Color.Transparent),
                        icon = {
                            if (showLabels) {
                                Icon(imageVector, contentDescription = null)
                            }
                        },
                        label = {
                            if (showLabels) {
                                Text(labels[index], maxLines = 1)
                            } else {
                                Icon(imageVector, contentDescription = labels[index])
                            }
                        },
                    )
                }
            }
        }
    }
}
