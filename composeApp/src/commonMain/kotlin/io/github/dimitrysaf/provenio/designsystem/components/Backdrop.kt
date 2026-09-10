package io.github.dimitrysaf.provenio.designsystem.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import io.github.dimitrysaf.provenio.designsystem.layout.WindowSizeClass
import io.github.dimitrysaf.provenio.designsystem.layout.windowSizeClassOf
import io.github.dimitrysaf.provenio.designsystem.theme.MotionTokens

/**
 * A backdrop never takes more than this much of the window, so whatever follows it lands
 * whole rather than clipped. Without the bound a wide window asks for more height than the
 * window has, and the artwork becomes the whole screen.
 */
private const val BackdropViewportFraction = 0.50f

/**
 * How tall a backdrop should be in a window this size.
 *
 * The shape follows the window rather than being clamped into it: portrait on a phone,
 * squarer on a small tablet, cinematic once there is real width. A single ratio cannot do
 * all three — asking a portrait ratio to fill a landscape window is what made the hero
 * taller than the screen.
 *
 * [viewportHeight] has to come from the caller: inside a lazy list the vertical space is
 * unbounded, so nothing here can measure the window on its own.
 */
fun backdropHeightFor(width: Dp, viewportHeight: Dp): Dp {
    // On a phone the aspect decides, not the cap — the window is tall enough that the
    // fraction never bites — so making the backdrop shorter there means a wider ratio.
    val aspect = when (windowSizeClassOf(width)) {
        WindowSizeClass.Compact -> 1.05f
        WindowSizeClass.Medium -> 1.5f
        else -> 16f / 9f
    }
    return minOf(width / aspect, viewportHeight * BackdropViewportFraction)
}

/**
 * Wide artwork that dissolves into the page, with [content] laid over it.
 *
 * The fade resolves to the page's own background well before the bottom edge, so anything
 * drawn at the foot of it sits on a real surface and can use scheme colours rather than a
 * fixed white that would ignore the wallpaper palette.
 *
 * [topScrim] darkens the first fifth for a control floating over the artwork — a back
 * button, typically — which would otherwise be invisible against a bright still.
 */
@Composable
fun Backdrop(
    url: String?,
    height: Dp,
    modifier: Modifier = Modifier,
    topScrim: Boolean = false,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(modifier = modifier.fillMaxWidth().height(height)) {
        BackdropArtwork(url)
        Box(modifier = Modifier.fillMaxSize().background(backdropBrush(topScrim)))
        content()
    }
}

/**
 * `scrim` is the role for darkening media, so a scheme override carries through the fade
 * rather than being overridden by a hardcoded black.
 */
@Composable
private fun backdropBrush(topScrim: Boolean): Brush {
    val scrim = MaterialTheme.colorScheme.scrim
    val background = MaterialTheme.colorScheme.background
    val stops = buildList {
        if (topScrim) {
            add(0f to scrim.copy(alpha = 0.45f))
            add(0.18f to Color.Transparent)
        } else {
            add(0f to Color.Transparent)
        }
        add(0.30f to scrim.copy(alpha = 0.35f))
        add(0.52f to background.copy(alpha = 0.92f))
        add(0.62f to background)
        add(1f to background)
    }
    return Brush.verticalGradient(colorStops = stops.toTypedArray())
}

/**
 * A filled container that breathes while the image is on its way, then hands over to the
 * artwork on a fade. A spinner over half a screen of colour reads as an error; a container
 * that is visibly waiting does not.
 */
@Composable
private fun BackdropArtwork(url: String?) {
    var loaded by remember(url) { mutableStateOf(false) }

    val pulse = rememberInfiniteTransition(label = "artworkPulse")
    val placeholderAlpha by pulse.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = MotionTokens.DurationLong2,
                easing = MotionTokens.Standard,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "artworkPulseAlpha",
    )

    val artworkAlpha by animateFloatAsState(
        targetValue = if (loaded) 1f else 0f,
        animationSpec = tween(
            durationMillis = MotionTokens.DurationMedium2,
            easing = MotionTokens.Standard,
        ),
        label = "artworkFade",
    )

    Box(modifier = Modifier.fillMaxSize()) {
        if (!loaded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHighest
                            .copy(alpha = placeholderAlpha),
                    ),
            )
        }
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = artworkAlpha,
                onState = { state -> loaded = state is AsyncImagePainter.State.Success },
            )
        }
    }
}
