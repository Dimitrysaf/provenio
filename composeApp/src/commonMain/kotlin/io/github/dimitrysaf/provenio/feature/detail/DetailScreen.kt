package io.github.dimitrysaf.provenio.feature.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.core.platform.rememberUrlOpener
import io.github.dimitrysaf.provenio.db.SimklItem
import io.github.dimitrysaf.provenio.feature.detail.components.DetailHeader
import io.github.dimitrysaf.provenio.feature.detail.components.Ratings
import io.github.dimitrysaf.provenio.feature.detail.components.SourcesSheet
import io.github.dimitrysaf.provenio.feature.detail.components.Synopsis
import io.github.dimitrysaf.provenio.feature.detail.components.WatchAction
import io.github.dimitrysaf.provenio.feature.detail.components.WatchProgress
import io.github.dimitrysaf.provenio.feature.detail.components.backdropsSection
import io.github.dimitrysaf.provenio.feature.detail.components.castAndCrew
import io.github.dimitrysaf.provenio.feature.detail.components.commentsSection
import io.github.dimitrysaf.provenio.feature.detail.components.factsSection
import io.github.dimitrysaf.provenio.feature.detail.components.seasonSection
import io.github.dimitrysaf.provenio.feature.detail.components.tagsAndThemes
import io.github.dimitrysaf.provenio.feature.detail.components.trailersSection
import io.github.dimitrysaf.provenio.simkl.SimklPlaybackRepository
import io.github.dimitrysaf.provenio.simkl.SimklPlaybackSession
import io.github.dimitrysaf.provenio.simkl.SimklSync
import io.github.dimitrysaf.provenio.simkl.SyncState
import io.github.dimitrysaf.provenio.stremio.AddonRepository
import io.github.dimitrysaf.provenio.stremio.model.Meta
import io.github.dimitrysaf.provenio.stremio.model.Video
import io.github.dimitrysaf.provenio.watch.EpisodeWatchedRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    type: String,
    id: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onPlay: (PlaybackRequest) -> Unit,
) {
    var meta by remember { mutableStateOf<Meta?>(null) }
    var loading by remember { mutableStateOf(true) }
    // Which title the sources sheet is open for, or null when it is closed.
    var sourcesFor by remember { mutableStateOf<SourcesTarget?>(null) }

    LaunchedEffect(type, id) {
        loading = true
        meta = AddonRepository.meta(type, id)
        loading = false
    }

    Box(modifier = modifier.fillMaxSize()) {
        val current = meta
        when {
            loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            current == null -> Text(
                text = "No add-on could describe this title.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
            )
            else -> MetaContent(current) { videoId, resumeProgress ->
                sourcesFor = SourcesTarget(videoId, resumeProgress)
            }
        }

        TopAppBar(
            title = {},
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        )
    }

    val openFor = sourcesFor
    if (openFor != null) {
        SourcesSheet(
            type = type,
            id = openFor.videoId,
            title = meta?.name,
            onDismiss = { sourcesFor = null },
            onPlay = { source ->
                sourcesFor = null
                val url = source.playableUrl ?: return@SourcesSheet
                val currentMeta = meta
                val video = currentMeta?.videos?.firstOrNull { it.id == openFor.videoId }
                onPlay(
                    PlaybackRequest(
                        url = url,
                        type = currentMeta?.type ?: type,
                        imdbId = currentMeta?.id ?: id,
                        season = video?.season,
                        episode = video?.episode,
                        resumeProgressPercent = openFor.resumeProgressPercent,
                    ),
                )
            },
        )
    }
}

/** Which video the sources sheet is open for, and the resume point "Watch now" picked. */
private data class SourcesTarget(val videoId: String, val resumeProgressPercent: Float? = null)

@Composable
private fun MetaContent(meta: Meta, onChooseSource: (String, Float?) -> Unit) {
    val openUrl = rememberUrlOpener()
    // Specials last. They routinely spoil the run they belong to, so leading with them is
    // the wrong default even though their season number sorts first.
    val seasons = meta.videos
        .groupBy { it.season ?: 0 }
        .toList()
        .sortedWith(compareBy({ (season, _) -> season == SpecialsSeason }, { it.first }))
    val expanded = rememberSaveable { mutableStateOf(seasons.firstOrNull()?.first ?: 1) }

    // Simkl only reports a watched/total count per show, refreshed whenever a sync lands,
    // so this re-checks it once on open and again whenever a sync finishes.
    var simklItem by remember(meta.id) { mutableStateOf<SimklItem?>(null) }
    val syncState by SimklSync.state.collectAsState()
    LaunchedEffect(meta.id, syncState) {
        if (syncState == SyncState.Idle) {
            simklItem = SimklSync.progressFor(meta.id)
        }
    }

    // A live look-up rather than anything synced: Simkl's paused-playback sessions are
    // meant to be read fresh, not cached, since they change the moment the user resumes
    // or finishes watching from any of Simkl's other connected apps.
    var resumeSession by remember(meta.id) { mutableStateOf<SimklPlaybackSession?>(null) }
    LaunchedEffect(meta.id) {
        resumeSession = SimklPlaybackRepository.sessionFor(meta.id)
    }

    // Simkl's own per-episode watched list for this show, synced via extended=full (see
    // SimklClient.library/changesSince) rather than guessed from the aggregate count.
    // Local overrides win over it in either direction — see EpisodeWatchedRepository for
    // why an override is not simply "watched" or absent.
    val simklWatchedEpisodes = remember(simklItem?.simklId) {
        simklItem?.let { SimklSync.watchedEpisodesFor(it.simklId) }.orEmpty()
    }
    val overrides by EpisodeWatchedRepository.overrides.collectAsState()
    val watchedIds = remember(meta, simklWatchedEpisodes, overrides) {
        val inferred = simklWatchedIds(meta, simklWatchedEpisodes)
        val watched = inferred.toMutableSet()
        overrides.forEach { (videoId, isWatched) ->
            if (isWatched) watched.add(videoId) else watched.remove(videoId)
        }
        watched
    }
    val onToggleWatched: (Video) -> Unit = { video ->
        EpisodeWatchedRepository.setWatched(meta.id, video, video.id !in watchedIds)
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { DetailHeader(meta) }
        item { WatchAction(meta, simklWatchedEpisodes, resumeSession, onChooseSource) }
        item { WatchProgress(meta, simklItem, watchedIds) }
        item { Ratings(meta) }
        item { Synopsis(meta) }

        seasonSection(
            seasons = seasons,
            expanded = expanded,
            watchedIds = watchedIds,
            onChooseSource = { videoId -> onChooseSource(videoId, null) },
            onToggleWatched = onToggleWatched,
        )
        castAndCrew(meta)
        tagsAndThemes(meta)
        commentsSection()
        factsSection(meta)
        trailersSection(meta, openUrl)
        backdropsSection(meta)

        item { Spacer(Modifier.height(32.dp)) }
    }
}
