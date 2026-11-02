package com.johnturkson.sandbox

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.johnturkson.sandbox.theme.EarthquakeTheme
import com.johnturkson.sandbox.ui.EarthquakeEventSnackbar
import com.johnturkson.sandbox.ui.EarthquakeList
import com.johnturkson.sandbox.ui.EarthquakeListRow
import com.johnturkson.sandbox.ui.EarthquakeMapItem

class EarthquakeComponentScreenshotTest {
    @PreviewTest
    @Preview(widthDp = 384, heightDp = 640, locale = "en-US")
    @Preview(
        widthDp = 384,
        heightDp = 640,
        locale = "en-US",
        uiMode = Configuration.UI_MODE_NIGHT_YES,
    )
    @Preview(widthDp = 320, heightDp = 720, locale = "en-US", fontScale = 1.5f)
    @Composable
    fun list(@PreviewParameter(EarthquakeListCaseProvider::class) case: EarthquakeListCase) {
        val items =
            listOf(
                    EarthquakeMapItem(
                        id = "shima",
                        latitude = 33.822,
                        longitude = 136.9854,
                        title = "M6.0 - 58 km SSE of Shima, Japan",
                        snippet = "Oct 31, 2026 · 10:15 PM",
                    ),
                    EarthquakeMapItem("alaska", 51.6994, -172.7931, "M4.5 - Atka, Alaska"),
                )
                .take(case.itemCount)
        EarthquakeTheme(dynamicColor = false) {
            Surface {
                EarthquakeList(
                    items = items,
                    selectedItemId = items.firstOrNull()?.id,
                    onItemClick = {},
                    isSupportingPane = case.isSupportingPane,
                    showTitle = case.showTitle,
                )
            }
        }
    }

    @PreviewTest
    @Preview(widthDp = 384, locale = "en-US")
    @Preview(widthDp = 384, locale = "en-US", uiMode = Configuration.UI_MODE_NIGHT_YES)
    @Preview(widthDp = 320, locale = "en-US", fontScale = 1.5f)
    @Composable
    fun listRow(
        @PreviewParameter(EarthquakeListRowCaseProvider::class) case: EarthquakeListRowCase
    ) {
        EarthquakeTheme(dynamicColor = false) {
            Surface {
                EarthquakeListRow(
                    item =
                        EarthquakeMapItem(
                            id = "shima",
                            latitude = 33.822,
                            longitude = 136.9854,
                            title = "M6.0 - 58 km SSE of Shima, Japan",
                            snippet = if (case.hasTime) "Oct 31, 2026 · 10:15 PM" else null,
                        ),
                    isSelected = case.selected,
                    onClick = {},
                    showMapAction = case.showMapAction,
                )
            }
        }
    }

    @PreviewTest
    @Preview(widthDp = 384, locale = "en-US")
    @Preview(widthDp = 384, locale = "en-US", uiMode = Configuration.UI_MODE_NIGHT_YES)
    @Preview(widthDp = 320, locale = "en-US", fontScale = 1.5f)
    @Composable
    fun unavailableEventSnackbar() {
        val eventMessage = stringResource(R.string.event_unavailable)
        val data =
            object : SnackbarData {
                override val visuals =
                    object : SnackbarVisuals {
                        override val message = eventMessage
                        override val actionLabel: String? = null
                        override val withDismissAction = false
                        override val duration = SnackbarDuration.Short
                    }

                override fun dismiss() = Unit

                override fun performAction() = Unit
            }
        EarthquakeTheme(dynamicColor = false) {
            Box(Modifier.padding(12.dp)) { EarthquakeEventSnackbar(data) }
        }
    }
}

data class EarthquakeListCase(
    val itemCount: Int,
    val isSupportingPane: Boolean,
    val showTitle: Boolean,
)

class EarthquakeListCaseProvider : PreviewParameterProvider<EarthquakeListCase> {
    override val values = sequence {
        for (itemCount in 0..2) {
            for (isSupportingPane in listOf(false, true)) {
                for (showTitle in listOf(false, true)) {
                    yield(EarthquakeListCase(itemCount, isSupportingPane, showTitle))
                }
            }
        }
    }
}

data class EarthquakeListRowCase(
    val selected: Boolean,
    val hasTime: Boolean,
    val showMapAction: Boolean,
)

class EarthquakeListRowCaseProvider : PreviewParameterProvider<EarthquakeListRowCase> {
    override val values = sequence {
        for (selected in listOf(false, true)) {
            for (hasTime in listOf(false, true)) {
                for (showMapAction in listOf(false, true)) {
                    yield(EarthquakeListRowCase(selected, hasTime, showMapAction))
                }
            }
        }
    }
}
