package io.github.dimitrysaf.provenio.shell.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/**
 * The page's backdrop: the title's artwork across the whole screen, blurred into frosted glass
 * behind everything else.
 *
 * The page colour is laid over it as a gradient rather than a flat sheet. Near the top, around
 * the hero, it is thin enough for the artwork's colour and light to come through; further down,
 * where the overview and the sections' text sit, it thickens so the text stays readable over
 * any image.
 */
@Composable
internal fun DetailBackdrop(
    backdropUrl: String?,
    visible: Boolean,
) {
    if (!visible || backdropUrl.isNullOrBlank()) return
    val background = MaterialTheme.colorScheme.background
    AsyncImage(
        model = backdropUrl,
        contentDescription = null,
        modifier = Modifier
            .fillMaxSize()
            .blur(BackdropBlurRadius),
        contentScale = ContentScale.Crop,
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to background.copy(alpha = 0.45f),
                    0.45f to background.copy(alpha = 0.62f),
                    1f to background.copy(alpha = 0.80f),
                ),
            ),
    )
}

private val BackdropBlurRadius = 40.dp
