package com.nuvio.app.features.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.CarouselItemDrawInfo
import androidx.compose.material3.carousel.HorizontalCenteredHeroCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nuvio.app.core.format.formatReleaseDateForDisplay
import com.nuvio.app.core.ui.ScreenActivityEffect
import com.nuvio.app.features.home.MetaPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val HERO_AUTO_SCROLL_INTERVAL_MS = 8_000L
private const val MOBILE_HERO_VIEWPORT_RATIO = 0.82f
private const val MOBILE_HERO_MIN_HEIGHT_DP = 360f
private const val MOBILE_HERO_MAX_HEIGHT_DP = 760f
private const val MOBILE_HERO_SMALL_ITEM_WIDTH_RATIO = 0.07f
private const val MOBILE_HERO_SMALL_ITEM_MIN_DP = 24f
private const val MOBILE_HERO_SMALL_ITEM_MAX_DP = 36f

/** Where a carousel item's overlay text has finished fading in, as a fraction of its unmasking. */
private const val HERO_ITEM_CONTENT_FADE_START = 0.62f

private val HeroIndicatorHeight = 8.dp
private val HeroIndicatorActiveWidth = 32.dp
internal val HeroIndicatorRowHeight = 24.dp

/** Text drawn over artwork, which is dark by the scrim rather than by the colour scheme. */
internal val HeroOnArtworkColor = Color.White
internal val HeroOnArtworkVariantColor = Color.White.copy(alpha = 0.76f)

/**
 * How the hero is sized and aligned at this width.
 *
 * Every width gets the same component, Material's own hero carousel, where the next titles sit
 * at the edges as masked slivers instead of the width being spent on empty artwork. Only the
 * sizing changes: a phone gets slimmer slivers and less breathing room than a tablet or a desktop
 * window does, and keeps its title centred the way a full-bleed phone hero always has, rather
 * than pinned to the carousel's leading edge the way a wider window's does.
 */
internal data class HomeHeroLayout(
    val heroHeight: Dp,
    val contentMaxWidth: Dp,
    val contentWidthFraction: Float,
    val contentHorizontalPadding: Dp,
    val contentVerticalPadding: Dp,
    val logoWidthFraction: Float,
    /** The gap between the focal item and the slivers beside it. */
    val itemSpacing: Dp,
    /** How wide those slivers are allowed to get. */
    val smallItemWidth: Dp,
    /** Phones keep their title centred; wider windows pin it to the carousel's leading edge. */
    val centerTitle: Boolean,
) {
    /** What the section occupies in the list, indicators included. */
    val totalHeight: Dp
        get() = heroHeight + contentVerticalPadding + HeroIndicatorRowHeight
}

@Composable
fun HomeHeroSection(
    items: List<MetaPreview>,
    modifier: Modifier = Modifier,
    viewportHeight: Dp? = null,
    mobileBelowSectionHeightHint: Dp? = null,
    onItemClick: ((MetaPreview) -> Unit)? = null,
) {
    if (items.isEmpty()) return

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val layout = homeHeroLayout(
            maxWidthDp = maxWidth.value,
            viewportHeightDp = viewportHeight?.value,
            mobileBelowSectionHeightHintDp = mobileBelowSectionHeightHint?.value,
        )

        HomeHeroCarousel(
            items = items,
            layout = layout,
            onItemClick = onItemClick,
        )
    }
}

/**
 * The home hero, at every width.
 *
 * [HorizontalCenteredHeroCarousel] is the Material 3 answer to a browsing hero: one large item in
 * the middle, the titles either side of it masked down to slivers that unmask as they reach the
 * centre. The artwork keeps its shape instead of being cropped to a letterbox, and the width that
 * a static hero would waste shows what is coming next - on a phone as much as on a tablet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeHeroCarousel(
    items: List<MetaPreview>,
    layout: HomeHeroLayout,
    onItemClick: ((MetaPreview) -> Unit)?,
) {
    val carouselState = rememberCarouselState(itemCount = { items.size })
    val coroutineScope = rememberCoroutineScope()

    HeroAutoAdvance(
        itemCount = items.size,
        currentItem = carouselState.currentItem,
        isScrollInProgress = { carouselState.isScrollInProgress },
        onAdvance = { page -> carouselState.animateScrollToItem(page) },
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = heroCarouselTopInset()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HorizontalCenteredHeroCarousel(
            state = carouselState,
            modifier = Modifier
                .fillMaxWidth()
                .height(layout.heroHeight),
            itemSpacing = layout.itemSpacing,
            minSmallItemWidth = 24.dp,
            maxSmallItemWidth = layout.smallItemWidth,
            contentPadding = PaddingValues(horizontal = layout.contentHorizontalPadding),
        ) { index ->
            val item = items[index]
            val drawInfo = carouselItemDrawInfo
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .maskClip(MaterialTheme.shapes.extraLarge)
                    .clickable {
                        // A tap on a sliver asks for that title, not for its details: bring it to
                        // the centre first, and open only what is already there.
                        if (heroItemIsFocal(drawInfo)) {
                            onItemClick?.invoke(item)
                        } else {
                            coroutineScope.launch { carouselState.animateScrollToItem(index) }
                        }
                    },
            ) {
                AsyncImage(
                    model = item.banner ?: item.poster,
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f),
                                    MaterialTheme.colorScheme.scrim.copy(alpha = 0.86f),
                                ),
                            ),
                        ),
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(layout.contentWidthFraction)
                        .widthIn(max = layout.contentMaxWidth)
                        .padding(
                            horizontal = layout.contentHorizontalPadding,
                            vertical = layout.contentVerticalPadding,
                        )
                        .graphicsLayer { alpha = heroItemContentAlpha(drawInfo) },
                ) {
                    HeroContentBlock(item = item, layout = layout)
                }
            }
        }

        Spacer(modifier = Modifier.height(layout.contentVerticalPadding))

        HeroIndicatorRow(
            itemCount = items.size,
            activeFraction = { index ->
                animateFloatAsState(
                    targetValue = if (index == carouselState.currentItem) 1f else 0f,
                    label = "HeroIndicator",
                ).value
            },
            onSelect = { index ->
                coroutineScope.launch { carouselState.animateScrollToItem(index) }
            },
            modifier = Modifier.height(HeroIndicatorRowHeight),
        )
    }
}

/**
 * The carousel is a card rather than a backdrop, so it starts below the status bar instead of
 * running under it the way a full-bleed hero would.
 */
@Composable
internal fun heroCarouselTopInset(): Dp =
    WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp

/** Moves to the next title on its own, and only while this screen is the one being looked at. */
@Composable
internal fun HeroAutoAdvance(
    itemCount: Int,
    currentItem: Int,
    isScrollInProgress: () -> Boolean,
    onAdvance: suspend (Int) -> Unit,
    enabled: Boolean = true,
) {
    val latestItem = rememberUpdatedState(currentItem)
    val latestIsScrollInProgress = rememberUpdatedState(isScrollInProgress)

    // Keyed only on the item count (and the screen's active state) - never on the current item.
    // The carousel's "current item" ticks continuously while it scrolls, including while this
    // effect's own `onAdvance` is animating it there; keying on that value would tear this effect
    // down every one of those ticks, cancelling the animation it just started and leaving the
    // carousel visibly stalled partway to the next title.
    ScreenActivityEffect(itemCount, enabled) { active ->
        if (!active || !enabled || itemCount <= 1) return@ScreenActivityEffect
        while (true) {
            delay(HERO_AUTO_SCROLL_INTERVAL_MS)
            while (latestIsScrollInProgress.value()) {
                delay(100L)
            }
            onAdvance((latestItem.value + 1) % itemCount)
        }
    }
}

/**
 * Which title is showing, and a way to jump to another.
 *
 * The active one widens into a pill and takes the accent colour, the rest stay dots: the state is
 * carried by shape and colour together rather than by opacity alone.
 */
@Composable
internal fun HeroIndicatorRow(
    itemCount: Int,
    activeFraction: @Composable (Int) -> Float,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (itemCount <= 1) return

    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(itemCount) { index ->
            key(index) {
                val fraction = activeFraction(index)
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(lerp(inactiveColor, activeColor, fraction))
                        .width(HeroIndicatorHeight + ((HeroIndicatorActiveWidth - HeroIndicatorHeight) * fraction))
                        .height(HeroIndicatorHeight)
                        .clickable { onSelect(index) },
                )
            }
        }
    }
}

/** Whether this item is the one at the centre, rather than a sliver on the way there. */
private fun heroItemIsFocal(drawInfo: CarouselItemDrawInfo): Boolean =
    drawInfo.size >= drawInfo.maxSize * 0.9f

/** A carousel item's text belongs to the item at the centre, so it fades with the mask. */
internal fun heroItemContentAlpha(drawInfo: CarouselItemDrawInfo): Float {
    val range = drawInfo.maxSize - drawInfo.minSize
    val unmasked = if (range <= 0f) 1f else ((drawInfo.size - drawInfo.minSize) / range)
    return ((unmasked - HERO_ITEM_CONTENT_FADE_START) / (1f - HERO_ITEM_CONTENT_FADE_START))
        .coerceIn(0f, 1f)
}

@Composable
fun HomeHeroReservedSpace(
    modifier: Modifier = Modifier,
    viewportHeight: Dp? = null,
    mobileBelowSectionHeightHint: Dp? = null,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val layout = homeHeroLayout(
            maxWidthDp = maxWidth.value,
            viewportHeightDp = viewportHeight?.value,
            mobileBelowSectionHeightHintDp = mobileBelowSectionHeightHint?.value,
        )

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(layout.totalHeight + heroCarouselTopInset()),
        )
    }
}

@Composable
private fun HeroContentBlock(
    item: MetaPreview,
    layout: HomeHeroLayout,
) {
    var logoLoadError by remember(item.type, item.id, item.logo) {
        mutableStateOf(false)
    }
    val logoUrl = item.logo?.takeIf { it.isNotBlank() }
    val horizontalAlignment = if (layout.centerTitle) Alignment.CenterHorizontally else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = horizontalAlignment,
    ) {
        if (logoUrl != null && !logoLoadError) {
            AsyncImage(
                model = logoUrl,
                contentDescription = item.name,
                modifier = Modifier
                    .fillMaxWidth(layout.logoWidthFraction)
                    .aspectRatio(2.6f),
                alignment = if (layout.centerTitle) Alignment.Center else Alignment.CenterStart,
                contentScale = ContentScale.Fit,
                onError = { logoLoadError = true },
            )
        } else {
            Text(
                text = item.name,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.displaySmallEmphasized,
                color = HeroOnArtworkColor,
                textAlign = if (layout.centerTitle) TextAlign.Center else TextAlign.Start,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (layout.centerTitle) {
                Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            } else {
                Arrangement.spacedBy(8.dp, Alignment.Start)
            },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeroMetaText(text = item.type.replaceFirstChar(Char::uppercase), color = HeroOnArtworkVariantColor)
            item.genres.firstOrNull()?.let { genre ->
                HeroMetaDot(color = HeroOnArtworkVariantColor)
                HeroMetaText(text = genre, color = HeroOnArtworkVariantColor)
            }
            item.releaseInfo?.takeIf { it.isNotBlank() }?.let { info ->
                HeroMetaDot(color = HeroOnArtworkVariantColor)
                HeroMetaText(text = formatReleaseDateForDisplay(info), color = HeroOnArtworkVariantColor)
            }
        }
    }
}

@Composable
private fun HeroMetaText(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun HeroMetaDot(color: Color) {
    Box(
        modifier = Modifier
            .size(4.dp)
            .clip(CircleShape)
            .background(color),
    )
}

internal fun homeHeroLayout(
    maxWidthDp: Float,
    viewportHeightDp: Float? = null,
    mobileBelowSectionHeightHintDp: Float? = null,
): HomeHeroLayout =
    when {
        maxWidthDp >= 1200f -> HomeHeroLayout(
            heroHeight = (maxWidthDp * 0.32f).dp.coerceIn(360.dp, 460.dp),
            contentMaxWidth = 620.dp,
            contentWidthFraction = 0.58f,
            contentHorizontalPadding = 32.dp,
            contentVerticalPadding = 28.dp,
            logoWidthFraction = 0.74f,
            itemSpacing = 12.dp,
            smallItemWidth = 96.dp,
            centerTitle = false,
        )
        maxWidthDp >= 840f -> HomeHeroLayout(
            heroHeight = (maxWidthDp * 0.40f).dp.coerceIn(320.dp, 420.dp),
            contentMaxWidth = 560.dp,
            contentWidthFraction = 0.66f,
            contentHorizontalPadding = 28.dp,
            contentVerticalPadding = 24.dp,
            logoWidthFraction = 0.78f,
            itemSpacing = 12.dp,
            smallItemWidth = 80.dp,
            centerTitle = false,
        )
        maxWidthDp >= 600f -> HomeHeroLayout(
            heroHeight = (maxWidthDp * 0.50f).dp.coerceIn(280.dp, 360.dp),
            contentMaxWidth = 480.dp,
            contentWidthFraction = 0.74f,
            contentHorizontalPadding = 24.dp,
            contentVerticalPadding = 20.dp,
            logoWidthFraction = 0.82f,
            itemSpacing = 8.dp,
            smallItemWidth = 64.dp,
            centerTitle = false,
        )
        else -> HomeHeroLayout(
            heroHeight = mobileHeroHeight(
                maxWidthDp = maxWidthDp,
                viewportHeightDp = viewportHeightDp,
                mobileBelowSectionHeightHintDp = mobileBelowSectionHeightHintDp,
            ),
            contentMaxWidth = 480.dp,
            contentWidthFraction = 1f,
            contentHorizontalPadding = 12.dp,
            contentVerticalPadding = 16.dp,
            logoWidthFraction = 0.62f,
            itemSpacing = 4.dp,
            smallItemWidth = (maxWidthDp * MOBILE_HERO_SMALL_ITEM_WIDTH_RATIO).dp
                .coerceIn(MOBILE_HERO_SMALL_ITEM_MIN_DP.dp, MOBILE_HERO_SMALL_ITEM_MAX_DP.dp),
            centerTitle = true,
        )
    }

private fun mobileHeroHeight(
    maxWidthDp: Float,
    viewportHeightDp: Float?,
    mobileBelowSectionHeightHintDp: Float?,
): Dp {
    val viewportDrivenHeight = viewportHeightDp?.let { (it * MOBILE_HERO_VIEWPORT_RATIO).dp }
    val widthFallbackHeight = (maxWidthDp * 1.16f).dp
    val baseHeight = if (mobileBelowSectionHeightHintDp == null) {
        viewportDrivenHeight?.coerceAtMost(widthFallbackHeight) ?: widthFallbackHeight
    } else {
        viewportDrivenHeight ?: widthFallbackHeight
    }

    val maxAllowedFromViewportDp = if (viewportHeightDp != null && mobileBelowSectionHeightHintDp != null) {
        viewportHeightDp - mobileBelowSectionHeightHintDp
    } else {
        null
    }
    val cappedHeight = if (maxAllowedFromViewportDp != null) {
        val maxAllowedFromViewport = maxAllowedFromViewportDp.dp
        baseHeight.coerceAtMost(maxAllowedFromViewport)
    } else {
        baseHeight
    }
    val minHeight = if (maxAllowedFromViewportDp != null) {
        minOf(MOBILE_HERO_MIN_HEIGHT_DP, maxAllowedFromViewportDp.coerceAtLeast(0f)).dp
    } else {
        MOBILE_HERO_MIN_HEIGHT_DP.dp
    }

    return cappedHeight.coerceIn(minHeight, MOBILE_HERO_MAX_HEIGHT_DP.dp)
}