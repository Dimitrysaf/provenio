package io.github.dimitrysaf.provenio.shell.screens.details.components

import androidx.compose.ui.focus.onFocusChanged
import io.github.dimitrysaf.provenio.shell.components.ShapedArtworkImage
import io.github.dimitrysaf.provenio.shell.components.SelectableText
import io.github.dimitrysaf.provenio.shell.components.FindableText
import io.github.dimitrysaf.provenio.shell.components.horizontalWheelPaging
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.carousel.HorizontalCenteredHeroCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.dimitrysaf.provenio.shell.components.shapedClickable
import io.github.dimitrysaf.provenio.core.metadata.DetailHeroSlide
import io.github.dimitrysaf.provenio.core.metadata.MetaDetails
import io.github.dimitrysaf.provenio.shell.screens.home.components.HeroMinSmallItemWidth
import io.github.dimitrysaf.provenio.shell.screens.home.components.HeroOnArtworkColor
import io.github.dimitrysaf.provenio.shell.screens.home.components.HeroOnArtworkVariantColor
import io.github.dimitrysaf.provenio.shell.screens.home.components.HomeHeroLayout
import io.github.dimitrysaf.provenio.shell.screens.home.components.heroCarouselTopInset
import io.github.dimitrysaf.provenio.shell.screens.home.components.heroItemContentAlpha
import io.github.dimitrysaf.provenio.shell.screens.home.components.homeHeroLayout
import kotlinx.coroutines.launch
import provenio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

private const val TRAILER_ASPECT_RATIO = 16f / 9f


/** The home screen's hero carousel, over one title's own pictures: its main artwork, then more. */
@Composable
fun DetailHero(
    meta: MetaDetails,
    slides: List<DetailHeroSlide>,
    modifier: Modifier = Modifier,
    viewportHeight: Dp? = null,
    onHeightChanged: (Int) -> Unit = {},
    onOpenArtwork: ((String) -> Unit)? = null,
) {
    // A title with no artwork at all still gets its page, over the plain surface.
    val pages = slides.ifEmpty { listOf(DetailHeroSlide.Artwork("")) }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val layout = homeHeroLayout(
            maxWidthDp = maxWidth.value,
            viewportHeightDp = viewportHeight?.value,
        )
        val topInset = heroCarouselTopInset()
        val sectionHeight = topInset + layout.heroHeight + layout.contentVerticalPadding
        val sectionHeightPx = with(LocalDensity.current) { sectionHeight.roundToPx() }
        LaunchedEffect(sectionHeightPx) { onHeightChanged(sectionHeightPx) }

        DetailHeroPages(
            meta = meta,
            pages = pages,
            layout = layout,
            topInset = topInset,
            onOpenArtwork = onOpenArtwork,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailHeroPages(
    meta: MetaDetails,
    pages: List<DetailHeroSlide>,
    layout: HomeHeroLayout,
    topInset: Dp,
    onOpenArtwork: ((String) -> Unit)?,
) {
    val carouselState = rememberCarouselState(itemCount = { pages.size })
    val openableArtworkUrl: (Int) -> String? = { index ->
        (pages.getOrNull(index) as? DetailHeroSlide.Artwork)?.url?.takeIf { onOpenArtwork != null && it.isNotBlank() }
    }
    val coroutineScope = rememberCoroutineScope()
    val focalPage = carouselState.currentItem

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topInset),
    ) {
        if (pages.size == 1) {
            // Nothing to peek at, so the page takes the width between equal margins.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = layout.contentHorizontalPadding)
                    .height(layout.heroHeight)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .shapedClickable(MaterialTheme.shapes.extraLarge, enabled = openableArtworkUrl(0) != null) {
                        openableArtworkUrl(0)?.let { onOpenArtwork?.invoke(it) }
                    },
            ) {
                DetailHeroPage(
                    page = pages[0],
                    meta = meta,
                    layout = layout,
                    posterFallback = true,
                    contentAlpha = { 1f },
                )
            }
        } else {
            HorizontalCenteredHeroCarousel(
                state = carouselState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(layout.heroHeight)
                    .horizontalWheelPaging(
                        key = carouselState,
                        canStep = { forward -> if (forward) carouselState.canScrollForward else carouselState.canScrollBackward },
                        step = { forward -> carouselState.animateScrollToItem(carouselState.currentItem + if (forward) 1 else -1) },
                    ),
                itemSpacing = layout.itemSpacing,
                minSmallItemWidth = minOf(HeroMinSmallItemWidth, layout.smallItemWidth),
                maxSmallItemWidth = layout.smallItemWidth,
                contentPadding = PaddingValues(horizontal = layout.contentHorizontalPadding),
            ) { index ->
                val drawInfo = carouselItemDrawInfo
                val isFocal = index == focalPage
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .maskClip(MaterialTheme.shapes.extraLarge)
                        // Moving onto a sliver with the arrow keys brings it to the centre.
                        .onFocusChanged { state ->
                            if (state.isFocused && !isFocal) {
                                coroutineScope.launch { carouselState.animateScrollToItem(index) }
                            }
                        }
                        .shapedClickable(
                            MaterialTheme.shapes.extraLarge,
                            enabled = !isFocal || openableArtworkUrl(index) != null,
                        ) {
                            if (isFocal) {
                                openableArtworkUrl(index)?.let { onOpenArtwork?.invoke(it) }
                            } else {
                                coroutineScope.launch { carouselState.animateScrollToItem(index) }
                            }
                        },
                ) {
                    DetailHeroPage(
                        page = pages[index],
                        meta = meta,
                        layout = layout,
                        posterFallback = index == 0,
                        contentAlpha = { heroItemContentAlpha(drawInfo) },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(layout.contentVerticalPadding))
    }
}

@Composable
private fun DetailHeroPage(
    page: DetailHeroSlide,
    meta: MetaDetails,
    layout: HomeHeroLayout,
    posterFallback: Boolean,
    contentAlpha: () -> Float,
) {
    when (page) {
        is DetailHeroSlide.Artwork -> HeroArtworkPage(
            url = page.url,
        meta = meta,
            layout = layout,
            posterFallback = posterFallback,
            // The title, its logo and genres belong to the first page only; the others are its pictures.
            showTitle = posterFallback,
            contentAlpha = contentAlpha,
        )
    }
}

@Composable
private fun HeroArtworkPage(
    url: String,
    meta: MetaDetails,
    layout: HomeHeroLayout,
    posterFallback: Boolean,
    showTitle: Boolean,
    contentAlpha: () -> Float,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (url.isBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
            )
        } else {
            ShapedArtworkImage(
                candidates = if (posterFallback) listOf(url) + meta.landscapeArtworkCandidates() else listOf(url),
                contentDescription = meta.name,
                showSkeleton = true,
            )
        }

        if (!showTitle) return@Box
        HeroPageScrim()

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(layout.contentWidthFraction)
                .widthIn(max = layout.contentMaxWidth)
                .padding(
                    horizontal = layout.contentHorizontalPadding,
                    vertical = layout.contentVerticalPadding,
                )
                .graphicsLayer { alpha = contentAlpha() },
        ) {
            HeroTitleBlock(meta = meta, layout = layout)
        }
    }
}

@Composable
private fun HeroTitleBlock(
    meta: MetaDetails,
    layout: HomeHeroLayout,
) {
    var logoLoadError by remember(meta.id, meta.logo) { mutableStateOf(false) }
    val logoUrl = meta.logo?.takeIf { it.isNotBlank() }
    val horizontalAlignment = if (layout.centerTitle) Alignment.CenterHorizontally else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = horizontalAlignment,
    ) {
        if (logoUrl != null && !logoLoadError) {
            AsyncImage(
                model = logoUrl,
                contentDescription = stringResource(Res.string.detail_logo_content_description, meta.name),
                modifier = Modifier
                    .fillMaxWidth(layout.logoWidthFraction)
                    .aspectRatio(2.6f),
                alignment = if (layout.centerTitle) Alignment.Center else Alignment.CenterStart,
                contentScale = ContentScale.Fit,
                onError = { logoLoadError = true },
            )
        } else {
            SelectableText(copyText = meta.name) {
                FindableText(
                    text = meta.name,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.displaySmallEmphasized,
                    color = HeroOnArtworkColor,
                    textAlign = if (layout.centerTitle) TextAlign.Center else TextAlign.Start,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (meta.genres.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            FindableText(
                text = meta.genres.take(3).joinToString(" • "),
                style = MaterialTheme.typography.labelLarge,
                color = HeroOnArtworkVariantColor,
                textAlign = if (layout.centerTitle) TextAlign.Center else TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HeroPageScrim() {
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
}

/** The title's wide pictures best first, with the poster last for a title that has none. */
private fun MetaDetails.landscapeArtworkCandidates(): List<String?> =
    listOf(background) + extraArtwork + listOf(poster)
