package io.github.dimitrysaf.provenio.shell.screens.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        BoxWithConstraints(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            val columns = if (maxWidth >= DetailInfoWideMinWidth) 3 else 2
            val lines = remember(rows, columns) { detailInfoLines(rows, columns) }
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                lines.forEach { line ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        line.forEach { (label, value) ->
                            DetailInfoCell(
                                label = label,
                                value = value,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(if (line.size == 1 && line.first().second.isLongInfoValue()) 0 else columns - line.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

private fun detailInfoLines(
    rows: List<Pair<String, String>>,
    columns: Int,
): List<List<Pair<String, String>>> {
    val lines = mutableListOf<List<Pair<String, String>>>()
    var current = mutableListOf<Pair<String, String>>()
    rows.forEach { row ->
        if (row.second.isLongInfoValue()) {
            if (current.isNotEmpty()) lines += current
            lines += listOf(row)
            current = mutableListOf()
        } else {
            current += row
            if (current.size == columns) {
                lines += current
                current = mutableListOf()
            }
        }
    }
    if (current.isNotEmpty()) lines += current
    return lines
}

private fun String.isLongInfoValue(): Boolean = length > LongInfoValueLength

@Composable
private fun DetailInfoCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private val DetailInfoWideMinWidth = 520.dp
private const val LongInfoValueLength = 22
