package com.johnturkson.sandbox.ui.list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.johnturkson.sandbox.R
import com.johnturkson.sandbox.ui.event.EarthquakeMapItem
import com.johnturkson.sandbox.ui.monitoring.EarthquakeMonitoringState

@Composable
fun EarthquakeListPanel(
    items: List<EarthquakeMapItem>,
    selectedItemId: String?,
    onItemClick: (EarthquakeMapItem) -> Unit,
    onMinimize: () -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    monitoring: EarthquakeMonitoringState? = null,
    onRefresh: () -> Unit = {},
    showMinimizeAction: Boolean = true,
    onMoreDetails: ((EarthquakeMapItem) -> Unit)? = null,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
    ) {
        Column {
            Row(
                Modifier.padding(start = 28.dp, end = 12.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.recent_earthquakes),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                )
                if (showMinimizeAction) {
                    IconButton(onClick = { onMinimize() }) {
                        Icon(
                            Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                            contentDescription = stringResource(R.string.minimize_earthquake_list),
                        )
                    }
                }
            }
            EarthquakeList(
                items = items,
                selectedItemId = selectedItemId,
                onItemClick = onItemClick,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                listState = listState,
                isSupportingPane = true,
                showTitle = false,
                monitoring = monitoring,
                onRefresh = onRefresh,
                onMoreDetails = onMoreDetails,
            )
        }
    }
}
