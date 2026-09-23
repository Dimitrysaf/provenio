package com.nuvio.app.features.details.components

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
import com.nuvio.app.core.format.formatReleaseDateForDisplay
import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.details.SPECIALS_SEASON_NUMBER
import com.nuvio.app.features.details.formatRuntimeForDisplay
import com.nuvio.app.features.details.groupedEpisodesForDisplay
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.StringResource
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

    return buildList {
        addIfPresent(Res.string.details_status, meta.status)
        addIfPresent(Res.string.details_release_info, meta.releaseInfo?.let(::formatReleaseDateForDisplay))
        addIfPresent(Res.string.details_last_aired, meta.lastAirDate?.let(::formatReleaseDateForDisplay))
        if (seasonCount > 0) add(stringResource(Res.string.details_seasons) to seasonCount.toString())
        if (episodeCount > 0) add(stringResource(Res.string.details_episodes) to episodeCount.toString())
        addIfPresent(Res.string.details_runtime, formatRuntimeForDisplay(meta.runtime))
        addIfPresent(Res.string.details_certification, meta.ageRating)
        addIfPresent(Res.string.details_genres, meta.genres.joinToString(", "))
        addIfPresent(Res.string.details_origin_country, meta.country)
        addIfPresent(Res.string.details_original_language, meta.language?.uppercase())
        addIfPresent(Res.string.details_awards, meta.awards)
        addIfPresent(Res.string.details_website, meta.website?.let(::formatWebsiteForDisplay))
    }
}

/** Adds a row only when the addon actually gave us the value. */
@Composable
private fun MutableList<Pair<String, String>>.addIfPresent(label: StringResource, value: String?) {
    val text = value?.trim()?.takeIf { it.isNotBlank() } ?: return
    add(stringResource(label) to text)
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
