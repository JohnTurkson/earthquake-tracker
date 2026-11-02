package com.johnturkson.sandbox.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.johnturkson.sandbox.R
import java.text.NumberFormat

@Composable
fun EarthquakeDetailsScreen(
    navKey: Destination.EarthquakeDetailsRoute,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    viewModel: EarthquakeDetailsViewModel =
        hiltViewModel<EarthquakeDetailsViewModel, EarthquakeDetailsViewModel.Factory>(
            creationCallback = { it.create(navKey) }
        ),
) {
    val uriHandler = LocalUriHandler.current
    EarthquakeDetailsScreen(
        state = viewModel,
        onClose = onClose,
        modifier = modifier,
        expanded = expanded,
        onOpenSource = uriHandler::openUri,
    )
}

@Composable
fun EarthquakeDetailsScreen(
    state: EarthquakeDetailsState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    onOpenSource: (String) -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
) {
    Box(
        modifier
            .fillMaxSize()
            .then(
                if (expanded) {
                    Modifier
                } else {
                    Modifier.background(MaterialTheme.colorScheme.surface)
                }
            )
    ) {
        val safePadding = WindowInsets.safeDrawing.asPaddingValues()
        val layoutDirection = LocalLayoutDirection.current
        val cardModifier =
            Modifier.fillMaxSize()
                .padding(
                    start = safePadding.calculateStartPadding(layoutDirection),
                    end = safePadding.calculateEndPadding(layoutDirection),
                    top =
                        if (expanded) {
                            0.dp
                        } else {
                            safePadding.calculateTopPadding()
                        },
                    bottom = safePadding.calculateBottomPadding(),
                )
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top =
                        if (expanded) {
                            16.dp
                        } else {
                            12.dp
                        },
                    bottom = 16.dp,
                )
        val item = state.item
        if (item != null) {
            EarthquakeDetailsCard(
                item = item,
                updatedTime =
                    earthquakeEventTime(item.mapItem.copy(eventTime = item.updated)).orEmpty(),
                onClose = onClose,
                modifier = cardModifier,
                onOpenSource = onOpenSource,
                listState = listState,
            )
        } else {
            Surface(cardModifier, shape = RoundedCornerShape(24.dp), shadowElevation = 6.dp) {
                Column {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.earthquake_details),
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        IconButton(onClick = onClose) {
                            Icon(
                                Icons.Outlined.Close,
                                stringResource(R.string.close_earthquake_details),
                            )
                        }
                    }
                    Column(
                        Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator()
                        }
                        Text(
                            stringResource(
                                if (state.isLoading) {
                                    R.string.loading_earthquake_details
                                } else {
                                    R.string.event_unavailable
                                }
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EarthquakeDetailsCard(
    item: EarthquakeDetailsItem,
    updatedTime: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenSource: (String) -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
    initialFeltExpanded: Boolean = false,
    initialShakingExpanded: Boolean = false,
) {
    var feltExpanded by rememberSaveable(item.mapItem.id) { mutableStateOf(initialFeltExpanded) }
    var shakingExpanded by
        rememberSaveable(item.mapItem.id) { mutableStateOf(initialShakingExpanded) }
    val unavailable = stringResource(R.string.earthquake_detail_unavailable)
    val locale = LocalConfiguration.current.locales[0]
    val magnitude = item.mapItem.magnitude
    val magnitudeValue =
        magnitude?.let { String.format(locale, "%.1f", it) }
            ?: stringResource(R.string.unknown_magnitude)
    val magnitudeText =
        if (magnitude != null) {
            stringResource(R.string.earthquake_magnitude_value, magnitudeValue)
        } else {
            magnitudeValue
        }
    val status =
        when (item.status) {
            "reviewed" -> stringResource(R.string.earthquake_status_reviewed)
            "automatic" -> stringResource(R.string.earthquake_status_automatic)
            null -> unavailable
            else -> item.status
        }
    val reports = item.felt?.let { NumberFormat.getIntegerInstance(locale).format(it) }
    Surface(
        modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.earthquake_details),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Outlined.Close, stringResource(R.string.close_earthquake_details))
                }
            }
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                state = listState,
                contentPadding =
                    PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            magnitudeText,
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Text(
                            item.place ?: stringResource(R.string.earthquake_location_unavailable),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        earthquakeEventTime(item.mapItem)?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                item {
                    EarthquakeDetailMetrics(
                        listOf(
                            stringResource(R.string.earthquake_depth) to
                                stringResource(R.string.earthquake_depth_km, item.depth),
                            stringResource(R.string.earthquake_usgs_status) to status,
                            stringResource(R.string.earthquake_felt_reports) to
                                (reports ?: unavailable),
                        )
                    )
                }
                item { HorizontalDivider() }
                item {
                    Text(
                        stringResource(R.string.earthquake_event_information),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                item {
                    EarthquakeDetailValue(
                        stringResource(R.string.earthquake_magnitude_label),
                        listOfNotNull(magnitudeValue, item.magnitudeType).joinToString(" "),
                    )
                }
                item {
                    EarthquakeDetailValue(
                        stringResource(R.string.earthquake_coordinates_label),
                        stringResource(
                            R.string.earthquake_coordinate_values,
                            item.mapItem.latitude,
                            item.mapItem.longitude,
                        ),
                    )
                }
                item {
                    EarthquakeDetailValue(
                        stringResource(R.string.earthquake_source_updated),
                        updatedTime,
                    )
                }
                item {
                    EarthquakeDetailValue(
                        stringResource(R.string.earthquake_source),
                        item.network?.let {
                            stringResource(R.string.earthquake_usgs_network, it.uppercase(locale))
                        } ?: unavailable,
                    )
                }
                item { HorizontalDivider() }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            stringResource(R.string.earthquake_maps_reports),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            stringResource(R.string.earthquake_usgs_additional_information),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                item.shakingMapUrl?.let { url ->
                    item {
                        EarthquakeReportSection(
                            title = stringResource(R.string.earthquake_shaking_map),
                            summary = stringResource(R.string.earthquake_shakemap_available),
                            imageUrl = url,
                            expanded = shakingExpanded,
                            onExpandedChange = { shakingExpanded = it },
                            onOpenSource = onOpenSource,
                        )
                    }
                }
                item {
                    EarthquakeReportSection(
                        title = stringResource(R.string.earthquake_dyfi),
                        summary =
                            reports?.let {
                                stringResource(R.string.earthquake_felt_report_count, it)
                            } ?: unavailable,
                        imageUrl = item.feltMapUrl,
                        onOpenSource = onOpenSource,
                        expanded = feltExpanded,
                        onExpandedChange = { feltExpanded = it },
                        icon = Icons.Outlined.PeopleOutline,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            EarthquakeDetailValue(
                                stringResource(R.string.earthquake_community_intensity),
                                item.communityIntensity
                                    ?.takeIf { it.isFinite() }
                                    ?.let { String.format(locale, "%.1f", it) } ?: unavailable,
                            )
                            Text(
                                stringResource(R.string.earthquake_dyfi_explanation),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                item.url
                    ?.takeIf { it.startsWith("https://earthquake.usgs.gov/") }
                    ?.let { url ->
                        item {
                            TextButton(onClick = { onOpenSource(url) }) {
                                Text(stringResource(R.string.open_usgs_earthquake))
                            }
                        }
                    }
            }
        }
    }
}

@Composable
private fun EarthquakeDetailMetrics(metrics: List<Pair<String, String>>) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val valueStyle =
        MaterialTheme.typography.titleLarge.copy(textDirection = TextDirection.ContentOrLtr)
    val minimumWidth = metrics.maxOf { (_, value) ->
        measurer.measure(value, valueStyle, softWrap = false).size.width
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columnWidth = (maxWidth - 24.dp) / 3
        if (columnWidth >= with(density) { minimumWidth.toDp() }) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                metrics.forEach { (label, value) ->
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(value, style = valueStyle)
                        Text(
                            label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                metrics.forEach { (label, value) ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            label,
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(value, style = valueStyle)
                    }
                }
            }
        }
    }
}

@Composable
private fun EarthquakeDetailValue(label: String, value: String) {
    val labelStyle = MaterialTheme.typography.bodyMedium
    val labelWidth = rememberTextMeasurer().measure(label, labelStyle, softWrap = false).size.width
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if ((maxWidth - 16.dp) / 2 >= with(density) { labelWidth.toDp() }) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    label,
                    Modifier.weight(1f),
                    style = labelStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    value,
                    Modifier.weight(1f),
                    style =
                        MaterialTheme.typography.bodyMedium.copy(
                            textDirection = TextDirection.ContentOrLtr
                        ),
                    textAlign = TextAlign.End,
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(label, style = labelStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    value,
                    style =
                        MaterialTheme.typography.bodyMedium.copy(
                            textDirection = TextDirection.ContentOrLtr
                        ),
                )
            }
        }
    }
}
