package com.johnturkson.sandbox.ui.details.reports

import android.content.res.Configuration
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.ColorImage
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImagePainter
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import coil3.request.ErrorResult
import com.android.tools.screenshot.PreviewTest
import com.johnturkson.sandbox.theme.EarthquakeTheme
import java.io.IOException

@OptIn(ExperimentalCoilApi::class)
class EarthquakeReportScreenshotTest {
    @PreviewTest
    @Preview(widthDp = 360, locale = "en-US")
    @Preview(widthDp = 360, locale = "en-US", uiMode = Configuration.UI_MODE_NIGHT_YES)
    @Preview(widthDp = 288, fontScale = 1.5f, locale = "en-US")
    @Composable
    fun LoadingImage() {
        Image { _, _ -> AsyncImagePainter.State.Loading(null) }
    }

    @PreviewTest
    @Preview(widthDp = 360, locale = "en-US")
    @Preview(widthDp = 360, locale = "en-US", uiMode = Configuration.UI_MODE_NIGHT_YES)
    @Preview(widthDp = 288, fontScale = 1.5f, locale = "en-US")
    @Composable
    fun FailedImage() {
        Image { _, request ->
            AsyncImagePainter.State.Error(
                null,
                ErrorResult(null, request, IOException("Fixture image failure")),
            )
        }
    }

    @PreviewTest
    @Preview(widthDp = 360, locale = "en-US")
    @Preview(widthDp = 360, locale = "en-US", uiMode = Configuration.UI_MODE_NIGHT_YES)
    @Preview(widthDp = 288, fontScale = 1.5f, locale = "en-US")
    @Composable
    fun LoadedImage() {
        Image(AsyncImagePreviewHandler { ColorImage(android.graphics.Color.LTGRAY, 640, 480) })
    }

    @PreviewTest
    @Preview(widthDp = 360, locale = "en-US")
    @Preview(widthDp = 288, fontScale = 1.5f, locale = "en-US")
    @Composable
    fun CollapsedReport() {
        EarthquakeTheme(dynamicColor = false) {
            Surface {
                EarthquakeReportSection(
                    "Did You Feel It?",
                    "128 felt reports",
                    {},
                    false,
                    {},
                    Modifier.padding(16.dp),
                )
            }
        }
    }

    @Composable
    private fun Image(handler: AsyncImagePreviewHandler) {
        CompositionLocalProvider(LocalAsyncImagePreviewHandler provides handler) {
            EarthquakeTheme(dynamicColor = false) {
                Surface {
                    EarthquakeReportImage(
                        "https://earthquake.usgs.gov/fixture.png",
                        "Deterministic report image fixture",
                        {},
                        Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}
