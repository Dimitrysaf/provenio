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
import io.github.dimitrysaf.provenio.player.PlaybackPosition
import io.github.dimitrysaf.provenio.simkl.SimklImages
import io.github.dimitrysaf.provenio.simkl.SimklPlaybackSession
import io.github.dimitrysaf.provenio.stremio.AddonRepository
import io.github.dimitrysaf.provenio.resources.Res
import io.github.dimitrysaf.provenio.resources.home_continue_watching
import org.jetbrains.compose.resources.stringResource

// Sized to sit inside the page margin rather than bleed past it, and shorter than the
// shelf it leads so it reads as one row among several rather than a hero.
private val ContinueWatchingItemWidth = 220.dp
private val ContinueWatchingItemHeight = 300.dp

/** One card of the shelf: enough to draw the poster, title, episode line and time bar. */
data class ContinueWatchingEntry(
    val id: String,
    val type: String,
    val title: String,
    val poster: String?,
    val season: Int? = null,
    val episode: Int? = null,
    /** 0..1 real time-into-this-title, or null when there is nothing to draw a bar for. */
    val progress: Float? = null,
)

/**
 * The lead shelf. Built primarily from this device's own local resume points — see
 * [buildContinueWatchingEntries] — so it is never empty just because nobody is signed into
 * Simkl, and its bar is always real time-into-this-episode rather than a percentage that
 * only updates when a scrobble happens to reach Simkl.
 *
 * Uses M3's multi-browse carousel rather than a plain scrolling row: the next card sits at
 * reduced width and grows into place as it scrolls to the front, which is the carousel
 * spec's whole point — https://m3.material.io/components/carousel/overview — rather than a
 * card just appearing whole once it clears the edge.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContinueWatchingCarousel(
    items: List<ContinueWatchingEntry>,
    onOpenDetail: (type: String, id: String) -> Unit,
) {
    if (items.isEmpty()) return
    val carouselState = rememberCarouselState { items.size }

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp)) {
        Text(
            text = stringResource(Res.string.home_continue_watching),
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
            ContinueWatchingCard(
                item = item,
                // Reads the carousel's own live mask math, so the card's corners and
                // edge clip exactly as it collapses — not a static rounded rect.
                modifier = Modifier.maskClip(RoundedCornerShape(20.dp)),
                onClick = { onOpenDetail(item.type, item.id) },
            )
        }
    }
}

@Composable
private fun ContinueWatchingCard(
    item: ContinueWatchingEntry,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        if (item.poster != null) {
            AsyncImage(
                model = item.poster,
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
            if (item.season != null && item.episode != null) {
                Text(
                    text = "S${pad(item.season)} · E${pad(item.episode)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.75f),
                )
            }
            // The position itself, in time — not an episode count — and no label: the
            // bar sitting on the art is the whole point, the same way a video scrubber
            // never needs to spell out what it is. Nothing drawn at all when there is no
            // real progress to show.
            if (item.progress != null) {
                LinearProgressIndicator(
                    progress = { item.progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(4.dp),
                    trackColor = Color.White.copy(alpha = 0.25f),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/**
 * Merges this device's own resume points with Simkl's "watching" list into one shelf.
 *
 * Local positions come first and win on a shared title: they are exact, written by the
 * player itself every few seconds, and need nobody signed in. Simkl only fills in a title
 * with no local record at all — watched from another device, say — using whatever bar it
 * can offer, which is only ever a paused session's own percentage.
 *
 * One card per title: within the local positions, only the most recently touched episode
 * of a show is kept, the same rule [io.github.dimitrysaf.provenio.feature.detail.components.WatchAction]
 * already uses for "resume where you left off".
 */
suspend fun buildContinueWatchingEntries(
    positions: Map<String, PlaybackPosition>,
    simklWatching: List<SimklItem>,
    simklSessionsByImdbId: Map<String, SimklPlaybackSession>,
): List<ContinueWatchingEntry> {
    val seenIds = mutableSetOf<String>()

    val local = positions.entries
        .sortedByDescending { it.value.updatedAtMillis }
        .mapNotNull { (videoId, position) ->
            val fraction = position.fraction ?: return@mapNotNull null
            val (imdbId, season, episode) = parseVideoId(videoId)
            if (!seenIds.add(imdbId)) return@mapNotNull null
            val type = if (season != null) "series" else "movie"
            val meta = AddonRepository.meta(type, imdbId) ?: return@mapNotNull null
            ContinueWatchingEntry(
                id = imdbId,
                type = type,
                title = meta.name ?: imdbId,
                poster = meta.poster,
                season = season,
                episode = episode,
                progress = fraction,
            )
        }

    val fromSimkl = simklWatching.mapNotNull { item ->
        val imdbId = item.imdbId ?: return@mapNotNull null
        if (!seenIds.add(imdbId)) return@mapNotNull null
        val session = simklSessionsByImdbId[imdbId]
        ContinueWatchingEntry(
            id = imdbId,
            type = item.stremioType(),
            title = item.title,
            poster = SimklImages.poster(item.poster),
            season = session?.episode?.season,
            episode = session?.episode?.number,
            progress = session?.progress?.let { (it / 100f).coerceIn(0f, 1f) },
        )
    }

    return local + fromSimkl
}

/** An addon video id such as `tt0108778` (film) or `tt0108778:1:1` (episode). */
private fun parseVideoId(videoId: String): Triple<String, Int?, Int?> {
    val parts = videoId.split(":")
    return if (parts.size >= 3) {
        Triple(parts[0], parts[1].toIntOrNull(), parts[2].toIntOrNull())
    } else {
        Triple(videoId, null, null)
    }
}

/** Zero padded episode and season numbers. Common Kotlin has no String.format. */
private fun pad(value: Int): String = value.toString().padStart(2, '0')
