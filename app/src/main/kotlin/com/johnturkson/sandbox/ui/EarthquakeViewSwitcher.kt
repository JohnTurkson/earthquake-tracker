package com.johnturkson.sandbox.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.johnturkson.sandbox.R

@Composable
fun EarthquakeViewSwitcher(
    selectedView: EarthquakeView,
    onViewSelected: (EarthquakeView) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
    ) {
        SingleChoiceSegmentedButtonRow(Modifier.padding(4.dp)) {
            EarthquakeView.entries.forEachIndexed { index, view ->
                SegmentedButton(
                    selected = selectedView == view,
                    onClick = { onViewSelected(view) },
                    shape = SegmentedButtonDefaults.itemShape(index, EarthquakeView.entries.size),
                    border = BorderStroke(0.dp, Color.Transparent),
                    icon = {
                        Icon(
                            imageVector =
                                if (view == EarthquakeView.Map) Icons.Outlined.Map
                                else Icons.AutoMirrored.Outlined.List,
                            contentDescription = null,
                        )
                    },
                    label = {
                        Text(
                            stringResource(
                                if (view == EarthquakeView.Map) R.string.map_view
                                else R.string.list_view
                            )
                        )
                    },
                )
            }
        }
    }
}
