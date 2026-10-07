package io.github.dimitrysaf.provenio.shell.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.core.format.formatReleaseDateLong
import io.github.dimitrysaf.provenio.core.i18n.localizedSeasonEpisodeCode
import io.github.dimitrysaf.provenio.core.metadata.MetaDetails
import io.github.dimitrysaf.provenio.core.metadata.MetaDetailsRepository
import io.github.dimitrysaf.provenio.core.metadata.MetaVideo
import io.github.dimitrysaf.provenio.shell.components.LoadingSpinner
import io.github.dimitrysaf.provenio.shell.screens.details.components.DetailInfoRows
import io.github.dimitrysaf.provenio.shell.screens.details.components.DetailSection
import io.github.dimitrysaf.provenio.shell.screens.details.components.ExpandableDescription
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.*

/** One episode's own page: its thumbnail, title, details and overview, laid out like the Details page. */
@Composable
fun EpisodeDetailScreen(
    type: String,
    metaId: String,
    videoId: String,
    episodeTitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var meta by remember(type, metaId) { mutableStateOf(MetaDetailsRepository.peek(type, metaId)) }
    var loading by remember(type, metaId) { mutableStateOf(meta?.videos?.none { it.id == videoId } ?: true) }
    LaunchedEffect(type, metaId, videoId) {
        if (meta?.videos?.any { it.id == videoId } == true) return@LaunchedEffect
        MetaDetailsRepository.fetch(type, metaId)?.let { meta = it }
        loading = false
    }
    val show = meta
    val episode = show?.videos?.firstOrNull { it.id == videoId }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (show == null || episode == null) {
            EpisodeDetailPlaceholder(
                videoId = videoId,
                title = episodeTitle,
                loading = loading,
                onBack = onBack,
            )
        } else {
            EpisodeDetailContent(show = show, episode = episode, onBack = onBack)
        }
    }
}

@Composable
private fun EpisodeDetailContent(
    show: MetaDetails,
    episode: MetaVideo,
    onBack: () -> Unit,
) {
    val images = remember(show, episode) {
        listOfNotNull(episode.thumbnail?.takeIf(String::isNotBlank)).ifEmpty {
            (listOf(show.background) + show.extraArtwork)
                .mapNotNull { it?.takeIf(String::isNotBlank) }
                .take(1)
        }
    }
    val code = localizedSeasonEpisodeCode(seasonNumber = episode.season, episodeNumber = episode.episode)
    val subtitle = listOfNotNull(show.name.takeIf(String::isNotBlank), code).joinToString(" · ")
    val rows = episodeInfoRows(show = show, episode = episode)
    val detailsTitle = stringResource(Res.string.episode_details_section)
    val overviewTitle = stringResource(Res.string.episode_details_overview)
    val overview = episode.overview?.trim()?.takeIf(String::isNotBlank)

    DetailPage(
        pageKey = "episode-${show.id}-${episode.id}",
        name = episode.title,
        subtitle = subtitle.takeIf(String::isNotBlank),
        images = images,
        deceased = false,
        onBack = onBack,
        infoItems = { metrics ->
            if (overview != null) {
                item(key = "episode-overview") {
                    DetailSectionContainer(
                        horizontalPadding = metrics.horizontalPadding,
                        contentMaxWidth = metrics.contentMaxWidth,
                    ) {
                        DetailSection(title = overviewTitle) {
                            ExpandableDescription(
                                text = overview,
                                collapsedMaxLines = 6,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            if (rows.isNotEmpty()) {
                item(key = "episode-info") {
                    DetailSectionContainer(
                        horizontalPadding = metrics.horizontalPadding,
                        contentMaxWidth = metrics.contentMaxWidth,
                    ) {
                        DetailSection(title = detailsTitle) {
                            DetailInfoRows(rows = rows)
                        }
                    }
                }
            }
        },
        railItems = {},
    )
}

@Composable
private fun episodeInfoRows(show: MetaDetails, episode: MetaVideo): List<Pair<String, String>> {
    val season = episode.season?.let {
        if (it <= 0) stringResource(Res.string.episodes_specials) else it.toString()
    }
    val candidates = listOf(
        stringResource(Res.string.episode_details_show) to show.name,
        stringResource(Res.string.episode_details_season) to season,
        stringResource(Res.string.episode_details_episode) to episode.episode?.toString(),
        stringResource(Res.string.episode_details_air_date) to episode.released?.let(::formatReleaseDateLong),
        stringResource(Res.string.details_runtime) to episode.runtime?.takeIf { it > 0 }?.let {
            stringResource(Res.string.episode_details_runtime_value, it)
        },
        stringResource(Res.string.episode_details_rating) to episode.rating?.takeIf { it > 0.0 }?.let {
            ((it * 10).roundToInt() / 10.0).toString()
        },
    )
    return candidates.mapNotNull { (label, value) ->
        value?.trim()?.takeIf(String::isNotBlank)?.let { label to it }
    }
}

@Composable
private fun EpisodeDetailPlaceholder(
    videoId: String,
    title: String,
    loading: Boolean,
    onBack: () -> Unit,
) {
    DetailPage(
        pageKey = "episode-placeholder-$videoId",
        name = title,
        subtitle = null,
        images = emptyList(),
        deceased = false,
        onBack = onBack,
        infoItems = {
            item(key = "episode-placeholder") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (loading) {
                        LoadingSpinner()
                    } else {
                        Text(
                            text = stringResource(Res.string.episode_details_not_found),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        railItems = {},
    )
}
