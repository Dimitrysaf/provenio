package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlin.math.max
import kotlin.math.min

/**
 * Width-to-height ratios of artwork the app has loaded, shared so every screen picks the same
 * picture. Each URL has its own state, so a load only redraws the images showing that URL.
 */
object ArtworkRatios {
    private val ratios = HashMap<String, MutableState<Float?>>()

    private fun stateOf(url: String): MutableState<Float?> = ratios.getOrPut(url) { mutableStateOf(null) }

    /** The measured ratio of [url], [Failed] when it could not load, or null before it has loaded. */
    fun ratioOf(url: String): Float? = stateOf(url).value

    /** Remembers the ratio [url] loaded with, or that it failed when [ratio] is null. */
    fun record(url: String, ratio: Float?) {
        val value = ratio?.takeIf { it.isFinite() && it > 0f } ?: Failed
        val state = stateOf(url)
        if (state.value != value) state.value = value
    }

    const val Failed = -1f
}

/** Which candidate to show and whether it has to be fitted whole because none matches the slot. */
internal data class ArtworkChoice(
    val url: String,
    val letterbox: Boolean,
    val pending: Boolean,
)

/**
 * Picks the first of [urls] whose shape suits a slot of [slotRatio]. One not loaded yet is tried
 * before any after it; when every candidate is the wrong shape, the closest one is fitted whole.
 */
internal fun chooseArtwork(
    urls: List<String>,
    slotRatio: Float?,
    ratioOf: (String) -> Float?,
): ArtworkChoice? {
    if (urls.isEmpty()) return null
    if (slotRatio == null) return ArtworkChoice(urls.first(), letterbox = false, pending = false)
    for (url in urls) {
        val ratio = ratioOf(url) ?: return ArtworkChoice(url, letterbox = false, pending = true)
        if (ratio != ArtworkRatios.Failed && !artworkShapeMismatched(ratio, slotRatio)) {
            return ArtworkChoice(url, letterbox = false, pending = false)
        }
    }
    val closest = urls
        .filter { ratioOf(it) != ArtworkRatios.Failed }
        .minByOrNull { url -> ratioOf(url)?.let { shapeDistance(it, slotRatio) } ?: Float.MAX_VALUE }
        ?: return null
    return ArtworkChoice(closest, letterbox = true, pending = false)
}

/** True when an image of [imageRatio] would look wrong cropped into a slot of [slotRatio]. */
internal fun artworkShapeMismatched(imageRatio: Float, slotRatio: Float): Boolean {
    val factor = shapeDistance(imageRatio, slotRatio)
    val orientationsDiffer = (imageRatio < 1f) != (slotRatio < 1f)
    return (orientationsDiffer && factor > OrientationTolerance) || factor > MaxCropFactor
}

private fun shapeDistance(imageRatio: Float, slotRatio: Float): Float =
    max(imageRatio, slotRatio) / min(imageRatio, slotRatio)

/**
 * Shows the first of [candidates] whose real shape suits the space it fills, so a wide slot gets a
 * wide picture and a tall slot a poster whenever one exists. Candidates go best first. When none
 * fits, the closest is shown whole over [letterboxColor].
 */
@Composable
fun ShapedArtworkImage(
    candidates: List<String?>,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    colorFilter: ColorFilter? = null,
    letterboxColor: Color = Color.Black,
    showSkeleton: Boolean = false,
) {
    val urls = remember(candidates) {
        candidates.mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }.distinct()
    }
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val slotRatio = if (maxWidth.value > 0f && maxHeight.value > 0f && maxHeight.value.isFinite() && maxWidth.value.isFinite()) {
            maxWidth.value / maxHeight.value
        } else {
            null
        }
        val choice = chooseArtwork(urls, slotRatio, ArtworkRatios::ratioOf)
        val skeletonAlpha by animateFloatAsState(
            targetValue = if (showSkeleton && (choice == null && urls.isNotEmpty() || choice?.pending == true)) 1f else 0f,
            animationSpec = tween(durationMillis = 260),
            label = "artwork_skeleton",
        )
        if (choice != null && choice.letterbox) {
            Box(modifier = Modifier.fillMaxSize().background(letterboxColor))
        }
        if (choice != null) AsyncImage(
            model = choice.url,
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = if (choice.pending) 0f else 1f },
            contentScale = if (choice.letterbox) ContentScale.Fit else ContentScale.Crop,
            alignment = if (choice.letterbox) Alignment.Center else alignment,
            colorFilter = colorFilter,
            onSuccess = { state ->
                val size = state.painter.intrinsicSize
                val ratio = size.takeIf { it.isSpecified && it.width > 0f && it.height > 0f }?.let { it.width / it.height }
                ArtworkRatios.record(choice.url, ratio ?: slotRatio)
            },
            onError = { ArtworkRatios.record(choice.url, null) },
        )
        if (skeletonAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = skeletonAlpha }
                    .skeleton(RectangleShape),
            )
        }
    }
}

/**
 * The candidate that suits a slot of [slotRatio], for places that draw the chosen picture more
 * than once. Unmeasured candidates are loaded out of sight first, so this is null until one is known.
 */
@Composable
internal fun rememberShapedArtwork(candidates: List<String?>, slotRatio: Float): ArtworkChoice? {
    val urls = remember(candidates) {
        candidates.mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }.distinct()
    }
    val choice = chooseArtwork(urls, slotRatio, ArtworkRatios::ratioOf)
    if (choice?.pending == true) {
        AsyncImage(
            model = choice.url,
            contentDescription = null,
            modifier = Modifier
                .size(ProbeSize)
                .graphicsLayer { alpha = 0f },
            contentScale = ContentScale.Fit,
            onSuccess = { state ->
                val size = state.painter.intrinsicSize
                val ratio = size.takeIf { it.isSpecified && it.width > 0f && it.height > 0f }?.let { it.width / it.height }
                ArtworkRatios.record(choice.url, ratio ?: slotRatio)
            },
            onError = { ArtworkRatios.record(choice.url, null) },
        )
    }
    return choice?.takeUnless { it.pending }
}

private val ProbeSize = 96.dp

private const val OrientationTolerance = 1.35f
private const val MaxCropFactor = 2.2f
