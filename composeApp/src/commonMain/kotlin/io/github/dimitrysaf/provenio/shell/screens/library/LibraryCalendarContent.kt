package io.github.dimitrysaf.provenio.shell.screens.library

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.dimitrysaf.provenio.core.calendar.UpcomingEpisode
import io.github.dimitrysaf.provenio.core.calendar.UpcomingEpisodesUiState
import io.github.dimitrysaf.provenio.core.format.formatReleaseDateForDisplay
import io.github.dimitrysaf.provenio.core.time.daysUntilEpisodeRelease
import io.github.dimitrysaf.provenio.shell.components.rememberPosterCardStyleUiState
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.compose_player_episode_code_full
import provenio.composeapp.generated.resources.cw_airs_today_short
import provenio.composeapp.generated.resources.cw_airs_tomorrow_short
import provenio.composeapp.generated.resources.library_calendar_empty

private val CalendarGridSpacing = 12.dp

// Upcoming episodes of the Library's shows, as a poster grid under a header for each air date.
internal fun LazyListScope.libraryCalendarContent(
    state: UpcomingEpisodesUiState,
    columns: Int,
    todayIsoDate: String,
    onEpisodeClick: (UpcomingEpisode) -> Unit,
) {
    if (!state.isLoaded) {
        libraryVerticalSkeletonItems(columns)
        return
    }
    if (state.episodes.isEmpty()) {
        item(key = "library-calendar-empty") {
            Text(
                text = stringResource(Res.string.library_calendar_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 32.dp),
            )
        }
        return
    }
    state.episodes.groupBy { it.releaseDateIso }.forEach { (dateIso, episodes) ->
        item(key = "library-calendar-date:$dateIso") {
            Text(
                text = calendarDateLabel(todayIsoDate, dateIso),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
            )
        }
        items(
            items = episodes.chunked(columns),
            key = { row -> "library-calendar-row:$dateIso:${row.first().showId}:${row.first().episodeId}" },
        ) { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(CalendarGridSpacing),
            ) {
                row.forEach { episode ->
                    CalendarEpisodeTile(
                        episode = episode,
                        onClick = { onEpisodeClick(episode) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(columns - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun calendarDateLabel(todayIsoDate: String, dateIso: String): String =
    when (daysUntilEpisodeRelease(todayIsoDate, dateIso)) {
        0 -> stringResource(Res.string.cw_airs_today_short)
        1 -> stringResource(Res.string.cw_airs_tomorrow_short)
        else -> formatReleaseDateForDisplay(dateIso)
    }

@Composable
private fun CalendarEpisodeTile(
    episode: UpcomingEpisode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val posterCardStyle = rememberPosterCardStyleUiState()
    val code = if (episode.seasonNumber != null && episode.episodeNumber != null) {
        stringResource(Res.string.compose_player_episode_code_full, episode.seasonNumber, episode.episodeNumber)
    } else {
        null
    }
    Column(
        modifier = modifier.clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(posterCardStyle.cornerRadiusDp.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            if (episode.poster != null) {
                AsyncImage(
                    model = episode.poster,
                    contentDescription = episode.showName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Text(
            text = episode.showName,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = listOfNotNull(code, episode.episodeTitle).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
