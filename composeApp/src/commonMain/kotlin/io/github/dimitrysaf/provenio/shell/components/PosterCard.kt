package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlin.math.roundToInt
import io.github.dimitrysaf.provenio.core.settings.PosterCardStyleRepository
import io.github.dimitrysaf.provenio.core.settings.PosterCardStyleUiState
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.poster_logo_content_description
import org.jetbrains.compose.resources.stringResource

internal const val PosterLandscapeAspectRatio = 1.77f

/** A poster's width to height, 2:3, the same for cards, grids and their skeletons. */
internal const val PosterPortraitAspectRatio = 2f / 3f

private const val PosterLandscapeWidthScale = 180f / 110f

internal fun landscapePosterWidth(basePosterWidthDp: Int): Dp =
    (basePosterWidthDp * PosterLandscapeWidthScale).dp

internal fun landscapePosterHeightForWidth(width: Dp): Dp =
    (width.value / PosterLandscapeAspectRatio).dp

/** The width of the page area posters are laid out in, when a screen knows it; null means the window. */
val LocalPosterAreaWidth = compositionLocalOf<Dp?> { null }

/** Lays out [content] and tells the posters in it how wide it is, for dynamic card sizing. */
@Composable
fun ProvidePosterAreaWidth(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        CompositionLocalProvider(LocalPosterAreaWidth provides maxWidth) { content() }
    }
}

/**
 * The card style every screen draws with. With dynamic sizing on, the stored width is replaced by
 * one that fits the chosen number of cards across the page area, so every caller sees a plain
 * fixed size. A fixed size is kept unless a single card would be wider than the page area.
 */
@Composable
internal fun rememberPosterCardStyleUiState(): PosterCardStyleUiState {
    PosterCardStyleRepository.ensureLoaded()
    val uiState by PosterCardStyleRepository.uiState.collectAsState()
    val windowWidthDp = with(LocalDensity.current) {
        LocalWindowInfo.current.containerSize.width.toDp().value
    }
    val areaWidthDp = LocalPosterAreaWidth.current?.value?.takeIf { it.isFinite() && it > 0f } ?: windowWidthDp
    if (areaWidthDp <= 0f) return uiState
    val widthDp = if (uiState.dynamicSizeEnabled) {
        dynamicPosterWidthDp(
            areaWidthDp = areaWidthDp,
            cardsPerRow = uiState.cardsPerRow,
            landscape = uiState.catalogLandscapeModeEnabled,
        )
    } else {
        val widest = dynamicPosterWidthDp(
            areaWidthDp = areaWidthDp,
            cardsPerRow = 1,
            landscape = uiState.catalogLandscapeModeEnabled,
        )
        if (uiState.widthDp <= widest) return uiState
        widest
    }
    return uiState.copy(widthDp = widthDp, heightDp = widthDp * 3 / 2)
}

/** The width a single card may take up in the current page area: the area less its side margins. */
@Composable
internal fun rememberPosterAreaContentWidth(): Dp {
    val windowWidthDp = with(LocalDensity.current) {
        LocalWindowInfo.current.containerSize.width.toDp().value
    }
    val areaWidthDp = LocalPosterAreaWidth.current?.value?.takeIf { it.isFinite() && it > 0f } ?: windowWidthDp
    return (areaWidthDp - posterAreaMarginDp(areaWidthDp) * 2).coerceAtLeast(MinPosterWidthDp.toFloat()).dp
}

/**
 * The poster width that fits [cardsPerRow] cards across a page area [areaWidthDp] wide, after its
 * side margins and the gaps between cards, with a sliver of the next card showing so a horizontal
 * shelf still reads as one that scrolls; grids share out their width themselves. With [cardsPerRow]
 * at 0 the count follows the area: about three and a third posters across a phone, more rather than
 * bigger ones on wider screens. In [landscape] mode the count is of landscape cards.
 */
internal fun dynamicPosterWidthDp(areaWidthDp: Float, cardsPerRow: Int = 0, landscape: Boolean = false): Int {
    val content = (areaWidthDp - posterAreaMarginDp(areaWidthDp) * 2).coerceAtLeast(MinPosterWidthDp.toFloat())
    val spacing = ShelfGridSpacing.value
    val scale = if (landscape) PosterLandscapeWidthScale else 1f
    val columns = if (cardsPerRow > 0) cardsPerRow else autoPosterColumns(areaWidthDp, landscape)
    val cell = if (cardsPerRow > 0) {
        (content - spacing * columns) / (columns + NextCardPeekFraction)
    } else {
        (content - spacing * (columns - 1)) / columns
    }
    return (cell / scale).toInt().coerceAtLeast(MinPosterWidthDp)
}

/** How many cards automatic sizing fits across a page area [areaWidthDp] wide. */
internal fun autoPosterColumns(areaWidthDp: Float, landscape: Boolean = false): Int {
    val content = (areaWidthDp - posterAreaMarginDp(areaWidthDp) * 2).coerceAtLeast(MinPosterWidthDp.toFloat())
    val spacing = ShelfGridSpacing.value
    val scale = if (landscape) PosterLandscapeWidthScale else 1f
    val target = autoPosterWidthDp(areaWidthDp) * scale
    return ((content + spacing) / (target + spacing)).roundToInt().coerceAtLeast(1)
}

/** How many columns automatic sizing gives the current page area, for the card style in use. */
@Composable
internal fun rememberAutoPosterColumns(): Int {
    PosterCardStyleRepository.ensureLoaded()
    val uiState by PosterCardStyleRepository.uiState.collectAsState()
    val windowWidthDp = with(LocalDensity.current) {
        LocalWindowInfo.current.containerSize.width.toDp().value
    }
    val areaWidthDp = LocalPosterAreaWidth.current?.value?.takeIf { it.isFinite() && it > 0f } ?: windowWidthDp
    return autoPosterColumns(areaWidthDp, uiState.catalogLandscapeModeEnabled)
}

/** The side margin pages give their shelves at this width, matching the Home screen's. */
internal fun posterAreaMarginDp(areaWidthDp: Float): Float = when {
    areaWidthDp >= 1440f -> 32f
    areaWidthDp >= 1024f -> 28f
    areaWidthDp >= 768f -> 24f
    else -> 16f
}

/** The poster width automatic sizing aims for, interpolated between these points and held at the ends. */
private fun autoPosterWidthDp(areaWidthDp: Float): Float {
    val points = AutoPosterWidthPoints
    if (areaWidthDp <= points.first().first) return points.first().second
    if (areaWidthDp >= points.last().first) return points.last().second
    val upper = points.indexOfFirst { it.first >= areaWidthDp }
    val (fromArea, fromWidth) = points[upper - 1]
    val (toArea, toWidth) = points[upper]
    val fraction = (areaWidthDp - fromArea) / (toArea - fromArea)
    return fromWidth + (toWidth - fromWidth) * fraction
}

/** Page area width to poster width, both in dp: compact phone, phone, tablet, laptop, desktop, 4K/TV. */
private val AutoPosterWidthPoints = listOf(
    360f to 104f,
    412f to 118f,
    840f to 150f,
    1280f to 176f,
    1920f to 210f,
    2560f to 250f,
)

/** The narrowest a poster gets, however many columns are asked for. */
internal const val MinPosterWidthDp = 56

/** How much of the next card a horizontal shelf shows past a set number of columns. */
private const val NextCardPeekFraction = 0.15f

enum class PosterCardShape {
    Poster,
    Square,
    Landscape,
}

@Composable
fun PosterCard(
    title: String,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    shape: PosterCardShape = PosterCardShape.Poster,
    fallbackImageUrls: List<String?> = emptyList(),
    detailLine: String? = null,
    showTitleBelow: Boolean = true,
    bottomLeftLogoUrl: String? = null,
    bottomLeftText: String? = null,
    isWatched: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val posterCardStyle = rememberPosterCardStyleUiState()
    val cardWidth = shape.cardWidth(basePosterWidthDp = posterCardStyle.widthDp)
    val cardShape = RoundedCornerShape(posterCardStyle.cornerRadiusDp.dp)
    val catalogLogoOverlaySize = catalogLogoOverlaySize(
        basePosterWidthDp = posterCardStyle.widthDp,
        shape = shape,
    )
    val shouldShowTitleBelow = showTitleBelow && !posterCardStyle.hideLabelsEnabled

    Column(
        modifier = modifier.width(cardWidth),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(shape.aspectRatio)
                .clip(cardShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .posterCardClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                    zoomImageUrl = imageUrl ?: fallbackImageUrls.firstOrNull { !it.isNullOrBlank() },
                    zoomCornerRadius = posterCardStyle.cornerRadiusDp.dp,
                ),
            contentAlignment = Alignment.Center,
        ) {
            val titleFallback: @Composable () -> Unit = {
                Text(
                    text = title,
                    modifier = Modifier.padding(horizontal = 14.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            ShapedArtworkImage(
                candidates = listOf(imageUrl) + fallbackImageUrls,
                contentDescription = title,
                modifier = Modifier.matchParentSize(),
                letterboxColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                fallback = titleFallback,
            )

            if (!bottomLeftLogoUrl.isNullOrBlank() || !bottomLeftText.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                ) {
                    if (!bottomLeftLogoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = bottomLeftLogoUrl,
                            contentDescription = stringResource(Res.string.poster_logo_content_description, title),
                            modifier = Modifier
                                .width(catalogLogoOverlaySize.width)
                                .height(catalogLogoOverlaySize.height),
                            contentScale = ContentScale.Fit,
                        )
                    } else {
                        Text(
                            text = bottomLeftText.orEmpty(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = catalogLogoOverlaySize.textMaxWidth),
                        )
                    }
                }
            }

            PosterWatchedOverlay(isWatched = isWatched)
        }
        if (shouldShowTitleBelow) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!detailLine.isNullOrBlank()) {
                Text(
                    text = detailLine,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Box(modifier = Modifier.height(0.dp))
            }
        } else {
            Box(modifier = Modifier.height(0.dp))
        }
    }
}

private val PosterCardShape.aspectRatio: Float
    get() = when (this) {
        PosterCardShape.Poster -> PosterPortraitAspectRatio
        PosterCardShape.Square -> 1f
        PosterCardShape.Landscape -> PosterLandscapeAspectRatio
    }

private data class CatalogLogoOverlaySize(
    val width: Dp,
    val height: Dp,
    val textMaxWidth: Dp,
)

private fun catalogLogoOverlaySize(
    basePosterWidthDp: Int,
    shape: PosterCardShape,
): CatalogLogoOverlaySize =
    if (shape == PosterCardShape.Landscape) {
        when {
            basePosterWidthDp <= 108 -> CatalogLogoOverlaySize(width = 92.dp, height = 24.dp, textMaxWidth = 120.dp)
            basePosterWidthDp <= 120 -> CatalogLogoOverlaySize(width = 104.dp, height = 28.dp, textMaxWidth = 132.dp)
            basePosterWidthDp <= 132 -> CatalogLogoOverlaySize(width = 116.dp, height = 30.dp, textMaxWidth = 144.dp)
            else -> CatalogLogoOverlaySize(width = 128.dp, height = 34.dp, textMaxWidth = 156.dp)
        }
    } else {
        when {
            basePosterWidthDp <= 108 -> CatalogLogoOverlaySize(width = 72.dp, height = 18.dp, textMaxWidth = 92.dp)
            basePosterWidthDp <= 120 -> CatalogLogoOverlaySize(width = 80.dp, height = 20.dp, textMaxWidth = 104.dp)
            basePosterWidthDp <= 132 -> CatalogLogoOverlaySize(width = 88.dp, height = 22.dp, textMaxWidth = 112.dp)
            else -> CatalogLogoOverlaySize(width = 96.dp, height = 24.dp, textMaxWidth = 124.dp)
        }
    }

private fun PosterCardShape.cardWidth(basePosterWidthDp: Int): Dp =
    when (this) {
        PosterCardShape.Poster -> basePosterWidthDp.dp
        PosterCardShape.Square -> basePosterWidthDp.dp
        PosterCardShape.Landscape -> landscapePosterWidth(basePosterWidthDp)
    }
