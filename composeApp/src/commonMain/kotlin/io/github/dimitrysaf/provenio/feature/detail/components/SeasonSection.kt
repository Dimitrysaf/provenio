package io.github.dimitrysaf.provenio.feature.detail.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.dimitrysaf.provenio.feature.detail.SpecialsSeason
import io.github.dimitrysaf.provenio.stremio.model.Video
import io.github.dimitrysaf.provenio.resources.Res
import io.github.dimitrysaf.provenio.resources.detail_episode
import io.github.dimitrysaf.provenio.resources.detail_episodes
import io.github.dimitrysaf.provenio.resources.detail_no_episodes
import io.github.dimitrysaf.provenio.resources.detail_season
import io.github.dimitrysaf.provenio.resources.detail_specials
import io.github.dimitrysaf.provenio.resources.not_watched
import io.github.dimitrysaf.provenio.resources.watched
import org.jetbrains.compose.resources.stringResource

fun LazyListScope.seasonSection(
    seasons: List<Pair<Int, List<Video>>>,
    expanded: MutableState<Int>,
    watchedIds: Set<String>,
    onChooseSource: (String) -> Unit,
    onToggleWatched: (Video) -> Unit,
) {
    item { SectionHeader(stringResource(Res.string.detail_episodes), Icons.Outlined.Tv) }

    if (seasons.isEmpty()) {
        item { EmptyNote(stringResource(Res.string.detail_no_episodes)) }
        return
    }

    seasons.forEach { (season, episodes) ->
        item {
            SeasonHeader(
                season = season,
                episodeCount = episodes.size,
                watched = episodes.count { it.id in watchedIds },
                expanded = expanded.value == season,
                onToggle = {
                    expanded.value = if (expanded.value == season) -1 else season
                },
            )
        }
        item {
            AnimatedVisibility(visible = expanded.value == season) {
                Column {
                    episodes.forEach { video ->
                        EpisodeRow(
                            video = video,
                            watched = video.id in watchedIds,
                            onClick = { onChooseSource(video.id) },
                            onToggleWatched = { onToggleWatched(video) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SeasonHeader(
    season: Int,
    episodeCount: Int,
    watched: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (season == SpecialsSeason) {
                    stringResource(Res.string.detail_specials)
                } else {
                    stringResource(Res.string.detail_season, season)
                },
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "$watched/$episodeCount",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = if (expanded) {
                    Icons.Filled.KeyboardArrowUp
                } else {
                    Icons.Filled.KeyboardArrowDown
                },
                contentDescription = null,
            )
        }
        if (watched > 0) {
            LinearProgressIndicator(
                progress = { watched.toFloat() / episodeCount },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
private fun EpisodeRow(
    video: Video,
    watched: Boolean,
    onClick: () -> Unit,
    onToggleWatched: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(110.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            if (video.thumbnail != null) {
                AsyncImage(
                    model = video.thumbnail,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            val season = video.season
            val episode = video.episode
            if (season != null && episode != null) {
                Text(
                    text = "S${pad(season)} | E${pad(episode)}",
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            Text(
                text = video.title ?: stringResource(Res.string.detail_episode),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        // A dedicated touch target, nested inside the row's own click target. Compose
        // resolves nested clickables front-to-back, so pressing the tick box toggles
        // watched state instead of also opening the sources sheet underneath it.
        IconButton(onClick = onToggleWatched) {
            Icon(
                imageVector = if (watched) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                contentDescription = stringResource(
                    if (watched) Res.string.watched else Res.string.not_watched,
                ),
                tint = if (watched) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}
