package io.github.dimitrysaf.provenio.shell.screens.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.github.dimitrysaf.provenio.shell.components.LoadingSpinner
import io.github.dimitrysaf.provenio.shell.theme.typeScale

@Composable
internal fun OpeningOverlay(
    artwork: String?,
    logo: String?,
    title: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    metrics: PlayerLayoutMetrics = PlayerLayoutMetrics.fromWidth(0.dp),
    statusLines: List<String> = emptyList(),
    backdropOnly: Boolean = false,
) {
    val contentAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 700, delayMillis = 400, easing = LinearEasing),
        label = "openingOverlayContentAlpha",
    )
    val pulse = rememberInfiniteTransition(label = "openingOverlayContentPulse")
    val contentScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "openingOverlayContentScale",
    )
    var logoLoadError by remember(logo) { mutableStateOf(false) }
    val logoUrl = logo?.takeIf { it.isNotBlank() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        val logoWidth = minOf(320.dp, maxWidth - 48.dp)
        val logoHeight = minOf(180.dp, maxHeight * 0.4f)
        val titleFontSize = if (maxWidth < 600.dp) 30.sp else 42.sp
        if (artwork != null) {
            AsyncImage(
                model = artwork,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopEnd,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to Color.Black.copy(alpha = 0.3f),
                                0.35f to Color.Black.copy(alpha = 0.6f),
                                0.7f to Color.Black.copy(alpha = 0.8f),
                                1f to Color.Black.copy(alpha = 0.9f),
                            ),
                        ),
                    ),
            )
        }

        // Behind the player's own controls only the artwork shows.
        if (!backdropOnly) Box(
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    alpha = contentAlpha
                    scaleX = contentScale
                    scaleY = contentScale
                },
            contentAlignment = Alignment.Center,
        ) {
            if (logoUrl != null && !logoLoadError) {
                AsyncImage(
                    model = logoUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .width(logoWidth)
                        .height(logoHeight),
                    contentScale = ContentScale.Fit,
                    onError = { logoLoadError = true },
                )
            } else if (!title.isNullOrBlank()) {
                Text(
                    text = title,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    style = MaterialTheme.typeScale.displayMd.copy(
                        fontSize = titleFontSize,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            } else {
                LoadingSpinner(
                    color = Color.White,
                    modifier = Modifier.size(54.dp),
                )
            }
        }

        // Same top row as the controls: back at the start, status at the end.
        if (!backdropOnly) Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .playerFrameInsets(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                .padding(metrics.horizontalPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            PlayerOverlayBackButton(onClick = onBack, metrics = metrics)
            PlayerStatusColumn(lines = statusLines)
        }
    }
}

// The player frame: only what the display actually covers, nothing reserved for gestures.
@Composable
internal fun Modifier.playerFrameInsets(sides: WindowInsetsSides): Modifier =
    windowInsetsPadding(WindowInsets.safeDrawing.only(sides))
