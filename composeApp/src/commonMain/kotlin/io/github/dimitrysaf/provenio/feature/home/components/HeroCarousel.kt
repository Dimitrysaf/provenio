package io.github.dimitrysaf.provenio.feature.home.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import io.github.dimitrysaf.provenio.designsystem.theme.MotionTokens
import io.github.dimitrysaf.provenio.stremio.AddonRepository
import io.github.dimitrysaf.provenio.stremio.model.Meta
import io.github.dimitrysaf.provenio.stremio.model.MetaPreview

/** Taller than it is wide, the way a phone hero fills the top of a screen. */
private const val HeroAspectRatio = 0.9f

/** Past this the hero stops growing, so a tablet does not get a full page of one title. */
private val HeroMaxHeight = 560.dp

private val DotSize = 8.dp
private val ActiveDotWidth = 24.dp

/**
 * The spotlight at the top of Home: one title at a time, swiped through.
 *
 * Backdrop and logo artwork live on the full [Meta], not on the catalog preview, so each
 * page fetches its own the first time it is shown and keeps it for the session. The pager
 * only composes the page either side of the current one, so this is two or three requests
 * rather than one per title, and swiping back never refetches.
 *
 * Everything the preview already knows — name, genre, year — draws immediately, and the
 * artwork fades in over a placeholder when it arrives. Nothing waits on the network to
 * appear, and nothing pops.
 *
 * The whole page is the target: a hero that fills half the screen does not need a button
 * to say it can be tapped.
 */
@Composable
fun HeroCarousel(
    items: List<MetaPreview>,
    onOpenDetail: (type: String, id: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { items.size })
    val full = remember { mutableStateMapOf<String, Meta>() }

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(HeroAspectRatio)
                .heightIn(max = HeroMaxHeight),
        ) { page ->
            val item = items[page]

            LaunchedEffect(item.id) {
                if (item.id !in full) {
                    AddonRepository.meta(item.type, item.id)?.let { full[item.id] = it }
                }
            }

            HeroPage(
                item = item,
                meta = full[item.id],
                onClick = { onOpenDetail(item.type, item.id) },
            )
        }

        Spacer(Modifier.height(16.dp))
        PageDots(count = items.size, current = pagerState.currentPage)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun HeroPage(
    item: MetaPreview,
    meta: Meta?,
    onClick: () -> Unit,
) {
    // The catalog preview carries a banner but never a backdrop, so the wide art is only
    // right once the full meta lands; the poster keeps something on screen until then.
    val backdrop = meta?.background ?: item.banner ?: item.poster
    val logo = meta?.logo
    val facts = listOfNotNull(
        heroTypeLabel(item.type),
        (meta?.genres ?: item.genres).firstOrNull(),
        item.releaseInfo?.take(4)?.takeIf { it.length == 4 },
    )

    Box(modifier = Modifier.fillMaxSize().clickable(onClick = onClick)) {
        Artwork(url = backdrop)

        // Resolves to the page's own background before the text starts, so everything
        // below sits on a real surface and can use scheme colours rather than a fixed
        // white that would ignore the wallpaper palette. `scrim` is the role for
        // darkening media, so the fade honours a scheme override too.
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.30f to MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f),
                    0.52f to MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
                    0.62f to MaterialTheme.colorScheme.background,
                    1f to MaterialTheme.colorScheme.background,
                ),
            ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // The logo only exists once the full meta lands, so the name holds the space
            // until then and the two crossfade rather than one replacing the other.
            AnimatedContent(
                targetState = logo,
                transitionSpec = {
                    val spec = tween<Float>(
                        durationMillis = MotionTokens.DurationMedium2,
                        easing = MotionTokens.Standard,
                    )
                    fadeIn(spec) togetherWith fadeOut(spec)
                },
                label = "heroTitle",
            ) { current ->
                if (current != null) {
                    AsyncImage(
                        model = current,
                        contentDescription = item.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth(0.75f).heightIn(max = 110.dp),
                    )
                } else {
                    Text(
                        text = item.name ?: item.id,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (facts.isNotEmpty()) {
                Text(
                    text = facts.joinToString("  •  "),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

/**
 * A filled container that breathes while the image is on its way, then hands over to the
 * artwork on a fade. A spinner over half a screen of colour reads as an error; a container
 * that is visibly waiting does not.
 */
@Composable
private fun Artwork(url: String?) {
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

/**
 * The current page reads as a bar, the rest as dots.
 *
 * Width and colour are animated rather than swapped, so the bar grows into place as the
 * page settles instead of jumping between two states.
 */
@Composable
private fun PageDots(count: Int, current: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            val active = index == current

            val width by animateDpAsState(
                targetValue = if (active) ActiveDotWidth else DotSize,
                animationSpec = tween(
                    durationMillis = MotionTokens.DurationMedium1,
                    easing = MotionTokens.Emphasized,
                ),
                label = "dotWidth",
            )
            val color by animateColorAsState(
                targetValue = if (active) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                animationSpec = tween(
                    durationMillis = MotionTokens.DurationMedium1,
                    easing = MotionTokens.Standard,
                ),
                label = "dotColor",
            )

            Box(
                modifier = Modifier
                    .height(DotSize)
                    .width(width)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

/** Singular here: the hero names one title, not a category of them. */
private fun heroTypeLabel(type: String): String = when (type) {
    "movie" -> "Movie"
    "series" -> "Series"
    "channel" -> "Channel"
    "tv" -> "TV"
    else -> type.replaceFirstChar { it.uppercase() }
}
