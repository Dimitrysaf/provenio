package io.github.dimitrysaf.provenio.shell.screens.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.core.format.formatReleaseDateForDisplay
import io.github.dimitrysaf.provenio.core.metadata.MetaDetails
import io.github.dimitrysaf.provenio.core.metadata.SPECIALS_SEASON_NUMBER
import io.github.dimitrysaf.provenio.core.metadata.formatRuntimeForDisplay
import io.github.dimitrysaf.provenio.core.metadata.groupedEpisodesForDisplay
import provenio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun DetailAdditionalInfoSection(
    meta: MetaDetails,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
) {
    val rows = detailInfoRows(meta)
    if (rows.isEmpty()) return

    DetailSection(
        title = detailInfoTitle(meta),
        modifier = modifier,
        showHeader = showHeader,
    ) {
        DetailInfoRows(rows = rows)
    }
}

/** What this title's own details table holds, shared by the section and the overview's expander. */
@Composable
internal fun detailInfoRows(meta: MetaDetails): List<Pair<String, String>> {
    val seasons = remember(meta) { meta.groupedEpisodesForDisplay() }
    val seasonCount = seasons.keys.count { it != SPECIALS_SEASON_NUMBER }
    val episodeCount = seasons.filterKeys { it != SPECIALS_SEASON_NUMBER }.values.sumOf { it.size }

    // Every label resolves on every pass, so the composition keeps one shape; missing values drop after.
    val candidates = listOf(
        stringResource(Res.string.details_status) to meta.status,
        stringResource(Res.string.details_release_info) to meta.releaseInfo?.let(::formatReleaseDateForDisplay),
        stringResource(Res.string.details_last_aired) to meta.lastAirDate?.let(::formatReleaseDateForDisplay),
        stringResource(Res.string.details_seasons) to seasonCount.takeIf { it > 0 }?.toString(),
        stringResource(Res.string.details_episodes) to episodeCount.takeIf { it > 0 }?.toString(),
        stringResource(Res.string.details_runtime) to formatRuntimeForDisplay(meta.runtime),
        stringResource(Res.string.details_certification) to meta.ageRating,
        stringResource(Res.string.details_genres) to meta.genres.joinToString(", "),
        stringResource(Res.string.details_origin_country) to meta.country,
        stringResource(Res.string.details_original_language) to meta.language?.uppercase(),
        stringResource(Res.string.details_awards) to meta.awards,
        stringResource(Res.string.details_website) to meta.website?.let(::formatWebsiteForDisplay),
    )
    return candidates.mapNotNull { (label, value) ->
        value?.trim()?.takeIf(String::isNotBlank)?.let { label to it }
    }
}

private fun formatWebsiteForDisplay(url: String): String =
    url.trim()
        .removePrefix("https://")
        .removePrefix("http://")
        .removePrefix("www.")
        .trimEnd('/')

@Composable
internal fun detailInfoTitle(meta: MetaDetails): String {
    val isSeriesLike = meta.type == "series" || meta.videos.any { it.season != null || it.episode != null }
    return if (isSeriesLike) {
        stringResource(Res.string.details_show_details)
    } else {
        stringResource(Res.string.details_movie_details)
    }
}

@Composable
internal fun DetailInfoRows(
    rows: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        rows.forEachIndexed { index, (label, value) ->
            DetailInfoRow(
                label = label,
                value = value,
                showDivider = index < rows.lastIndex,
            )
        }
    }
}

@Composable
private fun DetailInfoRow(
    label: String,
    value: String,
    showDivider: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (showDivider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}
