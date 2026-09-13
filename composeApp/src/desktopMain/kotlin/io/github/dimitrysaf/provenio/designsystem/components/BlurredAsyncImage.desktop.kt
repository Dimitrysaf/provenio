package io.github.dimitrysaf.provenio.designsystem.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/** Desktop's blur is Skia-backed and has no API-level gate to work around. */
@Composable
actual fun BlurredAsyncImage(url: String, modifier: Modifier, contentScale: ContentScale) {
    AsyncImage(
        model = url,
        contentDescription = null,
        modifier = modifier.blur(BlurRadius),
        contentScale = contentScale,
    )
}

private val BlurRadius = 18.dp
