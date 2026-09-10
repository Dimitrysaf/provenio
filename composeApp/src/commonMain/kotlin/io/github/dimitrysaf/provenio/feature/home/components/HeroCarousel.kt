package io.github.dimitrysaf.provenio.feature.home.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.dimitrysaf.provenio.designsystem.components.Backdrop
import io.github.dimitrysaf.provenio.designsystem.theme.MotionTokens
import io.github.dimitrysaf.provenio.stremio.AddonRepository
import io.github.dimitrysaf.provenio.stremio.model.Meta
import io.github.dimitrysaf.provenio.stremio.model.MetaPreview

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
    height: Dp,
    onOpenDetail: (type: String, id: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { items.size })
    val full = remember { mutableStateMapOf<String, Meta>() }

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            // An explicit height, not an aspect ratio with a cap after it: a cap placed
            // inside aspectRatio constrains the child but cannot shrink what aspectRatio
            // reports, so the hero grew past the window however low the maximum was set.
            modifier = Modifier.fillMaxWidth().height(height),
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
                height = height,
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
    height: Dp,
    onClick: () -> Unit,
) {
    // Tied to the hero rather than fixed: a cinematic hero on a tablet is far shorter
    // than a portrait one, and a 110dp logo inside it would crowd out everything else.
    val logoHeight = minOf(height * 0.22f, 110.dp)

    // The catalog preview carries a banner but never a backdrop, so the wide art is only
    // right once the full meta lands; the poster keeps something on screen until then.
    val backdrop = meta?.background ?: item.banner ?: item.poster
    val logo = meta?.logo
    val facts = listOfNotNull(
        heroTypeLabel(item.type),
        (meta?.genres ?: item.genres).firstOrNull(),
        item.releaseInfo?.take(4)?.takeIf { it.length == 4 },
    )

    Backdrop(
        url = backdrop,
        height = height,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
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
                        modifier = Modifier.fillMaxWidth(0.75f).heightIn(max = logoHeight),
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
