package io.github.dimitrysaf.provenio.shell.screens.details

import io.github.dimitrysaf.provenio.shell.screens.details.components.DetailListSkeleton
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import io.github.dimitrysaf.provenio.core.metadata.MetaDetails
import io.github.dimitrysaf.provenio.core.metadata.MetaDetailsRepository
import io.github.dimitrysaf.provenio.core.metadata.MetaVideo
import io.github.dimitrysaf.provenio.core.metadata.normalizeSeasonNumber
import io.github.dimitrysaf.provenio.shell.components.FindableText
import io.github.dimitrysaf.provenio.shell.screens.details.components.DetailInfoRows
import io.github.dimitrysaf.provenio.shell.screens.details.components.DetailSection
import io.github.dimitrysaf.provenio.shell.screens.details.components.ExpandableDescription
import io.github.dimitrysaf.provenio.shell.screens.details.components.isGenericSeasonName
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.*

/** One season's own page: its poster, name, overview and dates, laid out like Episode info. */
@Composable
fun SeasonDetailScreen(
    type: String,
    metaId: String,
    season: Int,
    seasonTitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var meta by remember(type, metaId) { mutableStateOf(MetaDetailsRepository.peek(type, metaId)) }
    var loading by remember(type, metaId) { mutableStateOf(meta == null) }
    LaunchedEffect(type, metaId) {
        if (meta != null) return@LaunchedEffect
        MetaDetailsRepository.fetch(type, metaId)?.let { meta = it }
        loading = false
    }
    val show = meta
    val episodes = show?.videos?.filter { normalizeSeasonNumber(it.season) == season }.orEmpty()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (show == null || episodes.isEmpty()) {
            SeasonDetailPlaceholder(season = season, title = seasonTitle, loading = loading, onBack = onBack)
        } else {
            SeasonDetailContent(show = show, season = season, seasonTitle = seasonTitle, episodes = episodes, onBack = onBack)
        }
    }
}

@Composable
private fun SeasonDetailContent(
    show: MetaDetails,
    season: Int,
    seasonTitle: String,
    episodes: List<MetaVideo>,
    onBack: () -> Unit,
) {
    val poster = episodes.firstNotNullOfOrNull { it.seasonPoster?.takeIf(String::isNotBlank) }
        ?: show.seasonPosters[season]
        ?: show.poster
    val ownName = episodes.firstNotNullOfOrNull { it.seasonName?.takeIf(String::isNotBlank) }
        ?.takeIf { season > 0 && !isGenericSeasonName(it, season) }
    val overview = episodes.firstNotNullOfOrNull { it.seasonOverview?.trim()?.takeIf(String::isNotBlank) }
    val airDates = episodes.mapNotNull { it.released?.takeIf(String::isNotBlank) }.sorted()
    val rows = listOf(
        stringResource(Res.string.episode_details_show) to show.name,
        stringResource(Res.string.episode_details_season) to
            if (season <= 0) stringResource(Res.string.episodes_specials) else season.toString(),
        stringResource(Res.string.details_episodes) to episodes.size.toString(),
        stringResource(Res.string.season_details_first_aired) to airDates.firstOrNull()?.let(::formatReleaseDateLong),
        stringResource(Res.string.season_details_last_aired) to
            airDates.lastOrNull()?.takeIf { airDates.size > 1 }?.let(::formatReleaseDateLong),
    ).mapNotNull { (label, value) -> value?.trim()?.takeIf(String::isNotBlank)?.let { label to it } }
    val detailsTitle = stringResource(Res.string.season_details_section)
    val overviewTitle = stringResource(Res.string.episode_details_overview)
    val subtitle = listOfNotNull(show.name.takeIf(String::isNotBlank), seasonTitle.takeIf { ownName != null })
        .joinToString(" · ")

    DetailPage(
        pageKey = "season-${show.id}-$season",
        name = ownName ?: seasonTitle,
        subtitle = subtitle.takeIf(String::isNotBlank),
        images = listOfNotNull(poster),
        deceased = false,
        onBack = onBack,
        infoItems = { metrics ->
            if (overview != null) {
                item(key = "season-overview") {
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
            item(key = "season-info") {
                DetailSectionContainer(
                    horizontalPadding = metrics.horizontalPadding,
                    contentMaxWidth = metrics.contentMaxWidth,
                ) {
                    DetailSection(title = detailsTitle) {
                        DetailInfoRows(rows = rows)
                    }
                }
            }
        },
        railItems = {},
        hasRails = false,
    )
}

@Composable
private fun SeasonDetailPlaceholder(
    season: Int,
    title: String,
    loading: Boolean,
    onBack: () -> Unit,
) {
    DetailPage(
        pageKey = "season-placeholder-$season",
        name = title,
        subtitle = null,
        images = emptyList(),
        deceased = false,
        onBack = onBack,
        infoItems = {
            item(key = "season-placeholder") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(if (loading) 16.dp else 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (loading) {
                        DetailListSkeleton()
                    } else {
                        FindableText(
                            text = stringResource(Res.string.season_details_not_found),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        railItems = {},
        hasRails = false,
    )
}
