package io.github.dimitrysaf.provenio.shell.screens.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.core.format.formatReleaseDateLong
import io.github.dimitrysaf.provenio.core.i18n.localizedSeasonEpisodeCode
import io.github.dimitrysaf.provenio.core.metadata.MetaDetails
import io.github.dimitrysaf.provenio.core.metadata.MetaVideo
import io.github.dimitrysaf.provenio.shell.components.BottomSheetBodyMargin
import io.github.dimitrysaf.provenio.shell.components.ModalSheet
import io.github.dimitrysaf.provenio.shell.components.ShapedArtworkImage
import io.github.dimitrysaf.provenio.shell.components.SheetHeader
import io.github.dimitrysaf.provenio.shell.components.SheetNavigation
import io.github.dimitrysaf.provenio.shell.components.dismissBottomSheet
import io.github.dimitrysaf.provenio.shell.components.safeBottomPadding
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.*

/**
 * What is playing, kept short for the player: one picture, the title, a line of facts and the
 * description. Shows the episode when an episode is playing, otherwise the movie or show.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlayerInfoSheet(
    title: String,
    meta: MetaDetails?,
    episode: MetaVideo?,
    poster: String?,
    background: String?,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close: () -> Unit = { scope.launch { dismissBottomSheet(sheetState, onDismiss) } }
    val showName = meta?.name?.takeIf(String::isNotBlank) ?: title
    val heading = episode?.title?.takeIf(String::isNotBlank) ?: showName
    val subtitle = episode?.let {
        listOfNotNull(
            showName,
            localizedSeasonEpisodeCode(seasonNumber = it.season, episodeNumber = it.episode),
        ).joinToString(" · ")
    }
    val facts = if (episode != null) episodeFacts(episode) else metaFacts(meta)
    val description = (episode?.overview ?: meta?.description)?.trim()?.takeIf(String::isNotBlank)
    val images = listOf(episode?.thumbnail, background, meta?.background) +
        meta?.extraArtwork.orEmpty() + listOf(poster, meta?.poster)

    ModalSheet(
        onDismissRequest = close,
        sheetState = sheetState,
    ) {
        SheetHeader(
            title = stringResource(Res.string.player_info_title),
            navigation = SheetNavigation.Close,
            onNavigate = close,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = BottomSheetBodyMargin)
                .padding(bottom = safeBottomPadding(24.dp)),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(MaterialTheme.shapes.large),
            ) {
                ShapedArtworkImage(
                    candidates = images,
                    contentDescription = heading,
                    letterboxColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    showSkeleton = true,
                )
            }
            Text(
                text = heading,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (facts.isNotEmpty()) {
                Text(
                    text = facts.joinToString(" · "),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (description != null) {
                Text(
                    text = description,
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun episodeFacts(episode: MetaVideo): List<String> = listOfNotNull(
    episode.released?.takeIf(String::isNotBlank)?.let(::formatReleaseDateLong),
    episode.runtime?.takeIf { it > 0 }?.let { stringResource(Res.string.episode_details_runtime_value, it) },
    episode.rating?.takeIf { it > 0.0 }?.let { "★ ${(it * 10).roundToInt() / 10.0}" },
)

private fun metaFacts(meta: MetaDetails?): List<String> {
    meta ?: return emptyList()
    return listOfNotNull(
        meta.releaseInfo?.takeIf(String::isNotBlank),
        meta.runtime?.takeIf(String::isNotBlank),
        meta.ageRating?.takeIf(String::isNotBlank),
        meta.imdbRating?.takeIf(String::isNotBlank)?.let { "★ $it" },
        meta.genres.take(3).joinToString(", ").takeIf(String::isNotBlank),
    )
}
