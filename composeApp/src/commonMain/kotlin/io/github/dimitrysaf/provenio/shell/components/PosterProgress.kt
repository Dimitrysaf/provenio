package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.provenio.core.home.MetaPreview
import io.github.dimitrysaf.provenio.core.watch.progress.WatchProgressRepository
import io.github.dimitrysaf.provenio.core.watch.watched.WatchedRepository
import io.github.dimitrysaf.provenio.core.watch.watching.application.WatchingState

/**
 * How far through a poster's title is, from 0 to 1, or null for none; see [WatchingState.posterProgress].
 * Continue Watching shows the episode or movie playing instead, so its cards do not ask this.
 */
val LocalPosterProgress = staticCompositionLocalOf<(MetaPreview) -> Float?> { { null } }

/** How far through each poster's title is, worked out once for every poster, for [LocalPosterProgress]. */
@Composable
fun rememberPosterProgressLookup(): (MetaPreview) -> Float? {
    val progressState by remember {
        WatchProgressRepository.ensureLoaded()
        WatchProgressRepository.uiState
    }.collectAsStateWithLifecycle()
    val seriesProgress by WatchedRepository.seriesProgress.collectAsStateWithLifecycle()
    val movieProgress = remember(progressState.entries) { WatchingState.movieProgressByKey(progressState.entries) }
    return remember(movieProgress, seriesProgress) {
        { item: MetaPreview -> WatchingState.posterProgress(item, movieProgress, seriesProgress) }
    }
}
