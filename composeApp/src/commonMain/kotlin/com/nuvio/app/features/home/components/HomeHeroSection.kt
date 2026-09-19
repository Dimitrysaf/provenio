package com.nuvio.app.features.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.stopScroll
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.CarouselItemDrawInfo
import androidx.compose.material3.carousel.HorizontalCenteredHeroCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nuvio.app.core.format.formatReleaseDateForDisplay
import com.nuvio.app.core.ui.ScreenActivityEffect
import com.nuvio.app.core.ui.heroStretchHeight
import com.nuvio.app.core.ui.heroStretchZoom
import com.nuvio.app.features.home.MetaPreview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val HERO_BACKGROUND_PARALLAX = 0.055f
private const val HERO_BACKGROUND_SCALE = 1.14f
private const val HERO_CONTENT_PARALLAX = 0.18f
private const val HERO_SCROLL_PARALLAX = 0.3f
private const val HERO_SCROLL_DOWN_SCALE_MULTIPLIER = 0.0001f
private const val HERO_SCROLL_UP_SCALE_MULTIPLIER = 0.002f
private const val HERO_SCROLL_MAX_SCALE = 1.3f
private const val HERO_SWIPE_THRESHOLD_FRACTION = 0.16f
private const val HERO_SWIPE_VELOCITY_THRESHOLD = 300f
private const val HERO_AUTO_SCROLL_INTERVAL_MS = 8_000L
private const val MOBILE_HERO_VIEWPORT_RATIO = 0.82f
private const val MOBILE_HERO_MIN_HEIGHT_DP = 360f
private const val MOBILE_HERO_MAX_HEIGHT_DP = 760f

/** Where a carousel item's overlay text has finished fading in, as a fraction of its unmasking. */
private const val HERO_ITEM_CONTENT_FADE_START = 0.62f

/** The phone hero meets the list with a soft edge, not a hard one. */
private val HeroBottomCornerRadius = 16.dp

private val HeroIndicatorHeight = 8.dp
private val HeroIndicatorActiveWidth = 32.dp
private val HeroIndicatorRowHeight = 24.dp

/** Text drawn over artwork, which is dark by the scrim rather than by the colour scheme. */
private val OnArtworkColor = Color.White
private val OnArtworkVariantColor = Color.White.copy(alpha = 0.76f)

/**
 * How the hero is presented at this width.
 *
 * Two shapes, not one stretched: a phone gets the full-bleed backdrop it has room to be
 * cinematic with, and anything wider gets Material's own hero carousel, where the next titles
 * sit at the edges as masked slivers instead of the width being spent on empty artwork.
 */
internal enum class HomeHeroStyle {
    Immersive,
    Carousel,
}

internal data class HomeHeroLayout(
    val style: HomeHeroStyle,
    val heroHeight: Dp,
    val contentMaxWidth: Dp,
    val contentWidthFraction: Float,
    val contentHorizontalPadding: Dp,
    val contentVerticalPadding: Dp,
    val bottomFadeHeight: Dp,
    val logoWidthFraction: Float,
    /** Carousel only: the gap between the focal item and the slivers beside it. */
    val itemSpacing: Dp = 0.dp,
    /** Carousel only: how wide those slivers are allowed to get. */
    val smallItemWidth: Dp = 0.dp,
) {
    val isCarousel: Boolean get() = style == HomeHeroStyle.Carousel

    /** What the section occupies in the list, indicators included. */
    val totalHeight: Dp
        get() = if (isCarousel) {
            heroHeight + contentVerticalPadding + HeroIndicatorRowHeight
        } else {
            heroHeight
        }
}

@Composable
fun HomeHeroSection(
    items: List<MetaPreview>,
    modifier: Modifier = Modifier,
    viewportHeight: Dp? = null,
    mobileBelowSectionHeightHint: Dp? = null,
    listState: LazyListState? = null,
    stretchPx: () -> Float = { 0f },
    onItemClick: ((MetaPreview) -> Unit)? = null,
) {
    if (items.isEmpty()) return

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val layout = homeHeroLayout(
            maxWidthDp = maxWidth.value,
            viewportHeightDp = viewportHeight?.value,
            mobileBelowSectionHeightHintDp = mobileBelowSectionHeightHint?.value,
        )

        when (layout.style) {
            HomeHeroStyle.Carousel -> HomeHeroCarousel(
                items = items,
                layout = layout,
                onItemClick = onItemClick,
            )

            HomeHeroStyle.Immersive -> HomeHeroImmersive(
                items = items,
                layout = layout,
                heroWidth = maxWidth,
                listState = listState,
                stretchPx = stretchPx,
                onItemClick = onItemClick,
            )
        }
    }
}

/**
 * The hero on a tablet, a foldable or a desktop window.
 *
 * [HorizontalCenteredHeroCarousel] is the Material 3 answer to a wide window: one large item in
 * the middle, the titles either side of it masked down to slivers that unmask as they reach the
 * centre. The artwork keeps its shape instead of being cropped to a letterbox, and the width that
 * a phone hero would waste shows what is coming next.
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
 * The hero on a phone: artwork edge to edge, the title over it, the list scrolling out from under.
 */
@Composable
private fun HomeHeroImmersive(
    items: List<MetaPreview>,
    layout: HomeHeroLayout,
    heroWidth: Dp,
    listState: LazyListState?,
    stretchPx: () -> Float,
    onItemClick: ((MetaPreview) -> Unit)?,
) {
    val pagerState = rememberPagerState(pageCount = { items.size })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pagerState) {
        pagerState.scrollToPage(pagerState.currentPage)
    }

    ScreenActivityEffect(pagerState) { active ->
        if (!active) {
            pagerState.stopScroll(MutatePriority.PreventUserInput)
            pagerState.scrollToPage(pagerState.currentPage)
        }
    }

    HeroAutoAdvance(
        itemCount = items.size,
        currentItem = pagerState.settledPage,
        isScrollInProgress = { pagerState.isScrollInProgress },
        onAdvance = { page -> pagerState.animateScrollToPage(page) },
    )

    val heroWidthPx = with(LocalDensity.current) { heroWidth.toPx() }
    val heroHeightPx = with(LocalDensity.current) { layout.heroHeight.toPx() }
    val scrollOffsetPx by remember(listState, heroHeightPx) {
        derivedStateOf {
            when {
                listState == null -> 0f
                listState.firstVisibleItemIndex > 0 -> heroHeightPx
                else -> listState.firstVisibleItemScrollOffset.toFloat()
            }
        }
    }
    val currentPage = pagerState.currentPage.coerceIn(items.indices)
    val visiblePages = listOf(
        currentPage,
        (currentPage - 1).coerceIn(items.indices),
        (currentPage + 1).coerceIn(items.indices),
    ).distinct()
        .mapNotNull { index ->
            val pageOffset = heroPageOffset(pagerState, index)
            val visibility = (1f - abs(pageOffset)).coerceIn(0f, 1f)
            if (visibility <= 0f) {
                null
            } else {
                HeroPageLayer(
                    page = index,
                    visibility = visibility,
                    offset = pageOffset,
                )
            }
        }
        .sortedBy(HeroPageLayer::visibility)
    val currentItem = items[currentPage]

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .homeHeroPagerGesture(
                pagerState = pagerState,
                itemCount = items.size,
                coroutineScope = coroutineScope,
            )
            .clip(RoundedCornerShape(bottomStart = HeroBottomCornerRadius, bottomEnd = HeroBottomCornerRadius))
            .heroStretchHeight(layout.heroHeight, stretchPx)
            .clickable(enabled = onItemClick != null) { onItemClick?.invoke(currentItem) },
    ) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.01f },
        ) {
            Box(modifier = Modifier.fillMaxSize())
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(layout.heroHeight)
                .heroStretchZoom(stretchPx),
        ) {
            visiblePages.forEach { layer ->
                AsyncImage(
                    model = items[layer.page].banner ?: items[layer.page].poster,
                    contentDescription = items[layer.page].name,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val offset = scrollOffsetPx
                            val scrollScale = heroBackgroundScrollScale(offset)
                            alpha = layer.visibility
                            translationX = -layer.offset * heroWidthPx * HERO_BACKGROUND_PARALLAX
                            translationY = heroBackgroundScrollTranslationY(offset)
                            scaleX = HERO_BACKGROUND_SCALE * scrollScale
                            scaleY = HERO_BACKGROUND_SCALE * scrollScale
                        },
                    alignment = Alignment.Center,
                    contentScale = ContentScale.Crop,
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background.copy(alpha = 0.02f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.34f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.78f),
                        ),
                    ),
                ),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(layout.bottomFadeHeight)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background.copy(alpha = 0f),
                            MaterialTheme.colorScheme.background,
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(
                    horizontal = layout.contentHorizontalPadding,
                    vertical = layout.contentVerticalPadding,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(layout.contentWidthFraction)
                    .widthIn(max = layout.contentMaxWidth),
                contentAlignment = Alignment.Center,
            ) {
                visiblePages.forEach { layer ->
                    Box(
                        modifier = Modifier.graphicsLayer {
                            alpha = layer.visibility
                            translationX = -layer.offset * heroWidthPx * HERO_CONTENT_PARALLAX
                        },
                    ) {
                        HeroContentBlock(item = items[layer.page], layout = layout)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            HeroIndicatorRow(
                itemCount = items.size,
                activeFraction = { index -> heroPageVisibility(pagerState, index) },
                onSelect = { index ->
                    coroutineScope.launch { pagerState.animateScrollToPage(index) }
                },
                modifier = Modifier.height(HeroIndicatorRowHeight),
            )
        }
    }
}

/**
 * The carousel is a card rather than a backdrop, so it starts below the status bar instead of
 * running under it the way the phone hero does.
 */
@Composable
internal fun heroCarouselTopInset(): Dp =
    WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp

/** Moves to the next title on its own, and only while this screen is the one being looked at. */
@Composable
private fun HeroAutoAdvance(
    itemCount: Int,
    currentItem: Int,
    isScrollInProgress: () -> Boolean,
    onAdvance: suspend (Int) -> Unit,
) {
    ScreenActivityEffect(currentItem, itemCount) { active ->
        if (!active || itemCount <= 1) return@ScreenActivityEffect
        delay(HERO_AUTO_SCROLL_INTERVAL_MS)
        while (isScrollInProgress()) {
            delay(100L)
        }
        onAdvance((currentItem + 1) % itemCount)
    }
}

/**
 * Which title is showing, and a way to jump to another.
 *
 * The active one widens into a pill and takes the accent colour, the rest stay dots: the state is
 * carried by shape and colour together rather than by opacity alone.
 */
@Composable
private fun HeroIndicatorRow(
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

private data class HeroPageLayer(
    val page: Int,
    val visibility: Float,
    val offset: Float,
)

private fun heroPageOffset(
    pagerState: PagerState,
    page: Int,
): Float = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction

private fun heroPageVisibility(
    pagerState: PagerState,
    page: Int,
): Float {
    return (1f - abs(heroPageOffset(pagerState, page))).coerceIn(0f, 1f)
}

/** Whether this item is the one at the centre, rather than a sliver on the way there. */
private fun heroItemIsFocal(drawInfo: CarouselItemDrawInfo): Boolean =
    drawInfo.size >= drawInfo.maxSize * 0.9f

/** A carousel item's text belongs to the item at the centre, so it fades with the mask. */
private fun heroItemContentAlpha(drawInfo: CarouselItemDrawInfo): Float {
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

        val topInset = if (layout.isCarousel) heroCarouselTopInset() else 0.dp

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(layout.totalHeight + topInset),
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
    val onArtwork = layout.isCarousel
    val titleColor = if (onArtwork) OnArtworkColor else MaterialTheme.colorScheme.onBackground
    val metaColor = if (onArtwork) OnArtworkVariantColor else MaterialTheme.colorScheme.onSurfaceVariant
    val horizontalAlignment = if (layout.isCarousel) Alignment.Start else Alignment.CenterHorizontally

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
                alignment = if (layout.isCarousel) Alignment.CenterStart else Alignment.Center,
                contentScale = ContentScale.Fit,
                onError = { logoLoadError = true },
            )
        } else {
            Text(
                text = item.name,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.displaySmallEmphasized,
                color = titleColor,
                textAlign = if (layout.isCarousel) TextAlign.Start else TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (layout.isCarousel) {
                Arrangement.spacedBy(8.dp, Alignment.Start)
            } else {
                Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeroMetaText(text = item.type.replaceFirstChar(Char::uppercase), color = metaColor)
            item.genres.firstOrNull()?.let { genre ->
                HeroMetaDot(color = metaColor)
                HeroMetaText(text = genre, color = metaColor)
            }
            item.releaseInfo?.takeIf { it.isNotBlank() }?.let { info ->
                HeroMetaDot(color = metaColor)
                HeroMetaText(text = formatReleaseDateForDisplay(info), color = metaColor)
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

/** Whether this width gets the carousel rather than the full-bleed phone hero. */
internal fun homeHeroUsesCarousel(maxWidthDp: Float): Boolean =
    homeHeroLayout(maxWidthDp).isCarousel

internal fun homeHeroLayout(
    maxWidthDp: Float,
    viewportHeightDp: Float? = null,
    mobileBelowSectionHeightHintDp: Float? = null,
): HomeHeroLayout =
    when {
        maxWidthDp >= 1200f -> HomeHeroLayout(
            style = HomeHeroStyle.Carousel,
            heroHeight = (maxWidthDp * 0.32f).dp.coerceIn(360.dp, 460.dp),
            contentMaxWidth = 620.dp,
            contentWidthFraction = 0.58f,
            contentHorizontalPadding = 32.dp,
            contentVerticalPadding = 28.dp,
            bottomFadeHeight = 0.dp,
            logoWidthFraction = 0.74f,
            itemSpacing = 12.dp,
            smallItemWidth = 96.dp,
        )
        maxWidthDp >= 840f -> HomeHeroLayout(
            style = HomeHeroStyle.Carousel,
            heroHeight = (maxWidthDp * 0.40f).dp.coerceIn(320.dp, 420.dp),
            contentMaxWidth = 560.dp,
            contentWidthFraction = 0.66f,
            contentHorizontalPadding = 28.dp,
            contentVerticalPadding = 24.dp,
            bottomFadeHeight = 0.dp,
            logoWidthFraction = 0.78f,
            itemSpacing = 12.dp,
            smallItemWidth = 80.dp,
        )
        maxWidthDp >= 600f -> HomeHeroLayout(
            style = HomeHeroStyle.Carousel,
            heroHeight = (maxWidthDp * 0.50f).dp.coerceIn(280.dp, 360.dp),
            contentMaxWidth = 480.dp,
            contentWidthFraction = 0.74f,
            contentHorizontalPadding = 24.dp,
            contentVerticalPadding = 20.dp,
            bottomFadeHeight = 0.dp,
            logoWidthFraction = 0.82f,
            itemSpacing = 8.dp,
            smallItemWidth = 64.dp,
        )
        else -> HomeHeroLayout(
            style = HomeHeroStyle.Immersive,
            heroHeight = mobileHeroHeight(
                maxWidthDp = maxWidthDp,
                viewportHeightDp = viewportHeightDp,
                mobileBelowSectionHeightHintDp = mobileBelowSectionHeightHintDp,
            ),
            contentMaxWidth = 480.dp,
            contentWidthFraction = 1f,
            contentHorizontalPadding = 24.dp,
            contentVerticalPadding = 16.dp,
            bottomFadeHeight = 220.dp,
            logoWidthFraction = 0.62f,
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

private fun heroBackgroundScrollScale(scrollOffsetPx: Float): Float {
    val scaleIncrease = if (scrollOffsetPx < 0f) {
        abs(scrollOffsetPx) * HERO_SCROLL_UP_SCALE_MULTIPLIER
    } else {
        scrollOffsetPx * HERO_SCROLL_DOWN_SCALE_MULTIPLIER
    }
    return (1f + scaleIncrease).coerceAtMost(HERO_SCROLL_MAX_SCALE)
}

private fun heroBackgroundScrollTranslationY(scrollOffsetPx: Float): Float {
    return scrollOffsetPx * HERO_SCROLL_PARALLAX
}

private fun Modifier.homeHeroPagerGesture(
    pagerState: PagerState,
    itemCount: Int,
    coroutineScope: CoroutineScope,
): Modifier {
    if (itemCount <= 1) return this

    return pointerInput(pagerState, itemCount) {
        awaitEachGesture {
            val down = awaitFirstDown(pass = PointerEventPass.Initial)
            val widthPx = size.width.toFloat().takeIf { it > 0f } ?: return@awaitEachGesture
            val velocityTracker = VelocityTracker().apply {
                addPosition(down.uptimeMillis, down.position)
            }
            val startPage = pagerState.currentPage
            var totalDx = 0f
            var totalDy = 0f
            var dragging = false

            while (true) {
                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                velocityTracker.addPosition(change.uptimeMillis, change.position)

                if (!change.pressed) {
                    if (dragging) {
                        val targetPage = resolveHeroTargetPage(
                            startPage = startPage,
                            itemCount = itemCount,
                            totalDx = totalDx,
                            velocityX = velocityTracker.calculateVelocity().x,
                            widthPx = widthPx,
                        )
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(targetPage)
                        }
                    }
                    break
                }

                val delta = change.position - change.previousPosition
                totalDx += delta.x
                totalDy += delta.y

                if (!dragging) {
                    val horizontalDrag =
                        abs(totalDx) > viewConfiguration.touchSlop && abs(totalDx) > abs(totalDy)
                    val verticalDrag =
                        abs(totalDy) > viewConfiguration.touchSlop && abs(totalDy) > abs(totalDx)

                    when {
                        verticalDrag -> break
                        horizontalDrag -> dragging = true
                        else -> continue
                    }
                }

                pagerState.dispatchRawDelta(-delta.x)
                change.consume()
            }
        }
    }
}

private fun resolveHeroTargetPage(
    startPage: Int,
    itemCount: Int,
    totalDx: Float,
    velocityX: Float,
    widthPx: Float,
): Int {
    val thresholdPassed = abs(totalDx) > widthPx * HERO_SWIPE_THRESHOLD_FRACTION ||
        abs(velocityX) > HERO_SWIPE_VELOCITY_THRESHOLD
    if (!thresholdPassed) return startPage

    val currentPage = startPage.coerceIn(0, itemCount - 1)
    return when {
        totalDx > 0f -> if (currentPage == 0) itemCount - 1 else currentPage - 1
        totalDx < 0f -> if (currentPage == itemCount - 1) 0 else currentPage + 1
        else -> currentPage
    }
}
