package com.nuvio.app.features.details.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalCenteredHeroCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nuvio.app.core.ui.heroStretchHeight
import com.nuvio.app.features.details.DetailHeroSlide
import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.details.MetaTrailer
import com.nuvio.app.features.details.youtubePlaybackUrl
import com.nuvio.app.features.details.youtubeThumbnailUrl
import com.nuvio.app.features.home.components.HeroAutoAdvance
import com.nuvio.app.features.home.components.HeroIndicatorRow
import com.nuvio.app.features.home.components.HeroIndicatorRowHeight
import com.nuvio.app.features.home.components.HeroOnArtworkColor
import com.nuvio.app.features.home.components.HeroOnArtworkVariantColor
import com.nuvio.app.features.home.components.HomeHeroLayout
import com.nuvio.app.features.home.components.heroCarouselTopInset
import com.nuvio.app.features.home.components.heroItemContentAlpha
import com.nuvio.app.features.home.components.homeHeroLayout
import com.nuvio.app.features.trailer.TrailerPlaybackResolver
import com.nuvio.app.features.trailer.TrailerPlaybackSource
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

private const val TRAILER_ASPECT_RATIO = 16f / 9f

/** Room kept under the trailer band for its audio button. */
private val TrailerBandBottomClearance = 52.dp

/** The home screen's hero carousel, over one title's own pages: artwork, trailers, more artwork. */
@Composable
fun DetailHero(
    meta: MetaDetails,
    slides: List<DetailHeroSlide>,
    modifier: Modifier = Modifier,
    stretchPx: () -> Float = { 0f },
    onHeightChanged: (Int) -> Unit = {},
    trailerResolutionEnabled: Boolean = false,
    trailerPlayWhenReady: () -> Boolean = { false },
    trailerMuted: Boolean = true,
    onTrailerMuteToggle: () -> Unit = {},
    onBackdropLoaded: (Painter, ImageBitmap?) -> Unit = { _, _ -> },
) {
    // A title with no artwork at all still gets its page, over the plain surface.
    val pages = slides.ifEmpty { listOf(DetailHeroSlide.Artwork("")) }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val layout = homeHeroLayout(maxWidthDp = maxWidth.value)
        val topInset = heroCarouselTopInset()
        val sectionHeightPx = with(LocalDensity.current) {
            (topInset + layout.totalHeight).roundToPx()
        }
        LaunchedEffect(sectionHeightPx) { onHeightChanged(sectionHeightPx) }

        DetailHeroCarousel(
            meta = meta,
            pages = pages,
            layout = layout,
            topInset = topInset,
            stretchPx = stretchPx,
            trailerResolutionEnabled = trailerResolutionEnabled,
            trailerPlayWhenReady = trailerPlayWhenReady,
            trailerMuted = trailerMuted,
            onTrailerMuteToggle = onTrailerMuteToggle,
            onBackdropLoaded = onBackdropLoaded,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailHeroCarousel(
    meta: MetaDetails,
    pages: List<DetailHeroSlide>,
    layout: HomeHeroLayout,
    topInset: Dp,
    stretchPx: () -> Float,
    trailerResolutionEnabled: Boolean,
    trailerPlayWhenReady: () -> Boolean,
    trailerMuted: Boolean,
    onTrailerMuteToggle: () -> Unit,
    onBackdropLoaded: (Painter, ImageBitmap?) -> Unit,
) {
    val carouselState = rememberCarouselState(itemCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()
    val focalPage = carouselState.currentItem
    val focalTrailer = (pages.getOrNull(focalPage) as? DetailHeroSlide.Trailer)?.trailer
    val trailerSources = remember(meta.id) { mutableStateMapOf<String, TrailerPlaybackSource>() }
    // Trailers that have played out, errored, or have no playable stream: the page keeps its still.
    val spentTrailerIds = remember(meta.id) { mutableStateListOf<String>() }

    LaunchedEffect(focalTrailer?.id, trailerResolutionEnabled) {
        val trailer = focalTrailer ?: return@LaunchedEffect
        if (!trailerResolutionEnabled) return@LaunchedEffect
        if (trailerSources.containsKey(trailer.id) || trailer.id in spentTrailerIds) {
            return@LaunchedEffect
        }
        val source = runCatching {
            TrailerPlaybackResolver.resolveFromYouTubeUrl(trailer.youtubePlaybackUrl())
        }.getOrNull()
        if (source == null) {
            spentTrailerIds.add(trailer.id)
        } else {
            trailerSources[trailer.id] = source
        }
    }

    HeroAutoAdvance(
        itemCount = pages.size,
        currentItem = focalPage,
        isScrollInProgress = { carouselState.isScrollInProgress },
        onAdvance = { page -> carouselState.animateScrollToItem(page) },
        // A trailer page holds its place until the trailer is done with it.
        enabled = focalTrailer == null || focalTrailer.id in spentTrailerIds,
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topInset),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HorizontalCenteredHeroCarousel(
            state = carouselState,
            modifier = Modifier
                .fillMaxWidth()
                .heroStretchHeight(layout.heroHeight, stretchPx),
            itemSpacing = layout.itemSpacing,
            maxSmallItemWidth = layout.smallItemWidth,
            contentPadding = PaddingValues(horizontal = layout.contentHorizontalPadding),
        ) { index ->
            val page = pages[index]
            val drawInfo = carouselItemDrawInfo
            val contentAlpha = { heroItemContentAlpha(drawInfo) }
            val isFocal = index == focalPage
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .maskClip(MaterialTheme.shapes.extraLarge)
                    .clickable(enabled = !isFocal) {
                        coroutineScope.launch { carouselState.animateScrollToItem(index) }
                    },
            ) {
                when (page) {
                    is DetailHeroSlide.Artwork -> HeroArtworkPage(
                        url = page.url,
                        meta = meta,
                        layout = layout,
                        reportLoaded = index == 0,
                        contentAlpha = contentAlpha,
                        onBackdropLoaded = onBackdropLoaded,
                    )

                    is DetailHeroSlide.Trailer -> HeroTrailerPage(
                        trailer = page.trailer,
                        artworkUrl = meta.background ?: meta.poster,
                        source = trailerSources[page.trailer.id],
                        isFocal = isFocal,
                        spent = page.trailer.id in spentTrailerIds,
                        playWhenReady = {
                            trailerPlayWhenReady() && !carouselState.isScrollInProgress
                        },
                        muted = trailerMuted,
                        layout = layout,
                        contentAlpha = contentAlpha,
                        onMuteToggle = onTrailerMuteToggle,
                        onSpent = { spentTrailerIds.add(page.trailer.id) },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(layout.contentVerticalPadding))

        HeroIndicatorRow(
            itemCount = pages.size,
            activeFraction = { index ->
                animateFloatAsState(
                    targetValue = if (index == focalPage) 1f else 0f,
                    label = "DetailHeroIndicator",
                ).value
            },
            onSelect = { index ->
                coroutineScope.launch { carouselState.animateScrollToItem(index) }
            },
            modifier = Modifier.height(HeroIndicatorRowHeight),
        )
    }
}

@Composable
private fun HeroArtworkPage(
    url: String,
    meta: MetaDetails,
    layout: HomeHeroLayout,
    reportLoaded: Boolean,
    contentAlpha: () -> Float,
    onBackdropLoaded: (Painter, ImageBitmap?) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (url.isBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
            )
        } else {
            AsyncImage(
                model = url,
                contentDescription = meta.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onSuccess = { state ->
                    if (reportLoaded) {
                        onBackdropLoaded(state.painter, loadedBackdropImageBitmap(state.result))
                    }
                },
            )
        }

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

/** A trailer over the dimmed artwork, in a band of its own shape so its frame is never cropped. */
@Composable
private fun HeroTrailerPage(
    trailer: MetaTrailer,
    artworkUrl: String?,
    source: TrailerPlaybackSource?,
    isFocal: Boolean,
    spent: Boolean,
    playWhenReady: () -> Boolean,
    muted: Boolean,
    layout: HomeHeroLayout,
    contentAlpha: () -> Float,
    onMuteToggle: () -> Unit,
    onSpent: () -> Unit,
) {
    var videoReady by remember(trailer.id, isFocal) { mutableStateOf(false) }
    val videoAlpha by animateFloatAsState(
        targetValue = if (videoReady) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "detail_hero_trailer_alpha",
    )

    Box(modifier = Modifier.fillMaxSize()) {
        if (artworkUrl != null) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f)),
        )

        HeroPageScrim()

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = layout.contentHorizontalPadding,
                    vertical = layout.contentVerticalPadding,
                )
                .padding(bottom = TrailerBandBottomClearance),
            contentAlignment = Alignment.Center,
        ) {
            val bandFill = if (maxWidth / maxHeight < TRAILER_ASPECT_RATIO) {
                Modifier.fillMaxWidth()
            } else {
                Modifier.fillMaxHeight()
            }
            Box(
                modifier = bandFill
                    .aspectRatio(TRAILER_ASPECT_RATIO)
                    .clip(MaterialTheme.shapes.large)
                    .background(Color.Black),
            ) {
                AsyncImage(
                    model = trailer.youtubeThumbnailUrl(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                if (isFocal && source != null && !spent) {
                    HeroTrailerPlayerSurface(
                        sourceUrl = source.videoUrl,
                        sourceAudioUrl = source.audioUrl?.takeIf { it.isNotBlank() },
                        playWhenReady = playWhenReady(),
                        muted = muted,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = videoAlpha },
                        onReady = { videoReady = true },
                        onEnded = {
                            videoReady = false
                            onSpent()
                        },
                        onError = {
                            videoReady = false
                            onSpent()
                        },
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(layout.contentWidthFraction)
                .widthIn(max = layout.contentMaxWidth)
                .padding(
                    horizontal = layout.contentHorizontalPadding,
                    vertical = layout.contentVerticalPadding,
                )
                .graphicsLayer { alpha = contentAlpha() },
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalIconToggleButton(
                checked = !muted,
                onCheckedChange = { onMuteToggle() },
            ) {
                Icon(
                    imageVector = if (muted) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                    contentDescription = stringResource(
                        if (muted) {
                            Res.string.detail_hero_trailer_audio_unmute
                        } else {
                            Res.string.detail_hero_trailer_audio_mute
                        },
                    ),
                )
            }
            Text(
                text = trailer.displayName?.takeIf { it.isNotBlank() } ?: trailer.name,
                style = MaterialTheme.typography.labelLarge,
                color = HeroOnArtworkColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
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
            Text(
                text = meta.name,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.displaySmallEmphasized,
                color = HeroOnArtworkColor,
                textAlign = if (layout.centerTitle) TextAlign.Center else TextAlign.Start,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (meta.genres.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
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
