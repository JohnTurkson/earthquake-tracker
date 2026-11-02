package com.johnturkson.sandbox.ui.details.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import com.johnturkson.sandbox.R

@Composable
fun EarthquakeReportImage(
    url: String,
    description: String,
    onOpenSource: (String) -> Unit,
    modifier: Modifier = Modifier,
    imageLoader: ImageLoader = SingletonImageLoader.get(LocalContext.current),
) {
    var attempt by remember(url) { mutableIntStateOf(0) }
    val context = LocalContext.current
    val request =
        remember(context, url, attempt) {
            ImageRequest.Builder(context).data(url).build()
        }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        key(attempt) {
            SubcomposeAsyncImage(
                model = request,
                imageLoader = imageLoader,
                contentDescription = description,
                modifier = Modifier.fillMaxWidth().height(280.dp),
                contentScale = ContentScale.Fit,
                loading = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                        Text(
                            stringResource(R.string.earthquake_report_image_loading),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                },
                error = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            stringResource(R.string.earthquake_report_image_error),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        TextButton(onClick = { attempt++ }) {
                            Text(stringResource(R.string.earthquake_report_image_retry))
                        }
                    }
                },
            )
        }
        TextButton(onClick = { onOpenSource(url) }) {
            Text(stringResource(R.string.earthquake_report_image_open))
        }
    }
}
