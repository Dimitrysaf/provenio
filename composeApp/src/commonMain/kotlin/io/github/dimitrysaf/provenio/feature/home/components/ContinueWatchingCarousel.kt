package io.github.dimitrysaf.provenio.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.dimitrysaf.provenio.db.SimklItem
import io.github.dimitrysaf.provenio.simkl.SimklImages
import io.github.dimitrysaf.provenio.simkl.SimklPlaybackSession

// Sized to sit inside the page margin rather than bleed past it, and shorter than the
// shelf it leads so it reads as one row among several rather than a hero.
private val ContinueWatchingItemWidth = 220.dp
private val ContinueWatchingItemHeight = 300.dp

/**
 * The lead shelf: Simkl's "watching" list, so it is never empty just because nothing has a
 * live scrobble session yet. A card's progress bar is real time-into-this-episode from
 * [timeProgressByImdbId] when Simkl has that session, and simply absent otherwise — an
 * episode-count fraction is a different metric, not a rougher version of the same one, so
 * there is no fallback bar to draw without it.
 *
 * Uses M3's multi-browse carousel rather than a plain scrolling row: the next card sits at
 * reduced width and grows into place as it scrolls to the front, which is the carousel
 * spec's whole point — https://m3.material.io/components/carousel/overview — rather than a
 * card just appearing whole once it clears the edge.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContinueWatchingCarousel(
    items: List<SimklItem>,
    timeProgressByImdbId: Map<String, SimklPlaybackSession>,
    onOpenDetail: (type: String, id: String) -> Unit,
) {
    if (items.isEmpty()) return
    val carouselState = rememberCarouselState { items.size }

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp)) {
        Text(
            text = "Continue watching",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        HorizontalMultiBrowseCarousel(
            state = carouselState,
            preferredItemWidth = ContinueWatchingItemWidth,
            itemSpacing = 12.dp,
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.fillMaxWidth().height(ContinueWatchingItemHeight),
        ) { index ->
            val item = items[index]
            val session = item.imdbId?.let { timeProgressByImdbId[it] }
            ContinueWatchingCard(
                item = item,
                session = session,
                // Reads the carousel's own live mask math, so the card's corners and
                // edge clip exactly as it collapses — not a static rounded rect.
                modifier = Modifier.maskClip(RoundedCornerShape(20.dp)),
                onClick = { item.imdbId?.let { onOpenDetail(item.stremioType(), it) } },
            )
        }
    }
}

@Composable
private fun ContinueWatchingCard(
    item: SimklItem,
    session: SimklPlaybackSession?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val poster = SimklImages.poster(item.poster)
    // The watching list has no per-episode number of its own; a matched session's is the
    // one actually being resumed, so it is shown only when there is one to show.
    val episode = session?.episode

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        if (poster != null) {
            AsyncImage(
                model = poster,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        // Scrim only behind the label, the same reasoning as the details page hero.
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.55f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.78f),
                ),
            ),
        )
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(14.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (episode != null) {
                Text(
                    text = "S${pad(episode.season)} · E${pad(episode.number)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.75f),
                )
            }
            // The position itself, in time — not an episode count — and no label: the
            // bar sitting on the art is the whole point, the same way a video scrubber
            // never needs to spell out what it is. Nothing drawn at all when Simkl has
            // not reported one.
            if (session != null) {
                LinearProgressIndicator(
                    progress = { (session.progress / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(4.dp),
                    trackColor = Color.White.copy(alpha = 0.25f),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/** Zero padded episode and season numbers. Common Kotlin has no String.format. */
private fun pad(value: Int): String = value.toString().padStart(2, '0')
