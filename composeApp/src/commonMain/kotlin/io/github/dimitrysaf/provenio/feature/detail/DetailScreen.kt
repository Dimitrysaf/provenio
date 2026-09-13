package io.github.dimitrysaf.provenio.feature.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Tv
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.core.platform.rememberUrlOpener
import io.github.dimitrysaf.provenio.player.PlaybackPosition
import io.github.dimitrysaf.provenio.player.PlaybackPositionRepository
import io.github.dimitrysaf.provenio.db.SimklItem
import io.github.dimitrysaf.provenio.designsystem.components.BackdropWash
import io.github.dimitrysaf.provenio.designsystem.components.backdropHeightFor
import io.github.dimitrysaf.provenio.designsystem.layout.WindowSizeClass
import io.github.dimitrysaf.provenio.designsystem.layout.isPortraitPhone
import io.github.dimitrysaf.provenio.designsystem.layout.maxContentWidth
import io.github.dimitrysaf.provenio.designsystem.layout.windowSizeClassOf
import io.github.dimitrysaf.provenio.feature.detail.components.DetailHeader
import io.github.dimitrysaf.provenio.feature.detail.components.Ratings
import io.github.dimitrysaf.provenio.feature.detail.components.SectionHeader
import io.github.dimitrysaf.provenio.feature.detail.components.SourcesSheet
import io.github.dimitrysaf.provenio.feature.detail.components.Synopsis
import io.github.dimitrysaf.provenio.feature.detail.components.WatchAction
import io.github.dimitrysaf.provenio.feature.detail.components.WatchProgress
import io.github.dimitrysaf.provenio.feature.detail.components.WatchlistAction
import io.github.dimitrysaf.provenio.feature.detail.components.backdropsSection
import io.github.dimitrysaf.provenio.feature.detail.components.castAndCrew
import io.github.dimitrysaf.provenio.feature.detail.components.factsSection
import io.github.dimitrysaf.provenio.feature.detail.components.seasonSection
import io.github.dimitrysaf.provenio.feature.detail.components.tagsSection
import io.github.dimitrysaf.provenio.feature.detail.components.trailersSection
import io.github.dimitrysaf.provenio.core.platform.currentTimeMillis
import io.github.dimitrysaf.provenio.simkl.SimklAuthState
import io.github.dimitrysaf.provenio.simkl.SimklPlaybackRepository
import io.github.dimitrysaf.provenio.simkl.SimklPlaybackSession
import io.github.dimitrysaf.provenio.simkl.SimklRepository
import io.github.dimitrysaf.provenio.simkl.SimklSync
import io.github.dimitrysaf.provenio.simkl.SyncState
import io.github.dimitrysaf.provenio.stremio.AddonRepository
import io.github.dimitrysaf.provenio.stremio.streamId
import io.github.dimitrysaf.provenio.stremio.model.Meta
import io.github.dimitrysaf.provenio.stremio.model.Video
import io.github.dimitrysaf.provenio.watch.EpisodeWatchedRepository
import kotlinx.coroutines.launch
import io.github.dimitrysaf.provenio.resources.Res
import io.github.dimitrysaf.provenio.resources.back
import io.github.dimitrysaf.provenio.resources.detail_episodes
import io.github.dimitrysaf.provenio.resources.detail_no_addon
import org.jetbrains.compose.resources.stringResource

/**
 * How much of the backdrop has to scroll away before the bar is fully solid. A collapsing
 * bar that finishes early leaves the title sitting on artwork; one that finishes late
 * pops.
 */
private val TopBarHeight = 64.dp

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
    // How far into each episode or film this device got, and which source it used — so a
    // part-watched entry shows a bar and "resume" can try that exact source again.
    val positions by PlaybackPositionRepository.positions.collectAsState()
    val scope = rememberCoroutineScope()
    // The one video currently being resolved against its remembered source, so a second
    // tap while that is in flight is ignored rather than starting a race with it.
    var resolvingVideoId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(type, id) {
        loading = true
        meta = AddonRepository.meta(type, id)
        loading = false
    }

    // BoxWithConstraints rather than Box: the backdrop is sized against the window, and
    // inside the lazy list below the vertical space is unbounded, so this is the last
    // place that still knows how tall the window actually is.
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val backdropHeight = backdropHeightFor(width = maxWidth, viewportHeight = maxHeight)

        // The backdrop is a portrait-phone treatment. Anywhere else — a phone lying down
        // included — there is nothing to collapse: the bar is simply always there, and
        // the list starts below it rather than under it.
        val showBackdrop = isPortraitPhone(maxWidth, maxHeight)

        // Wide enough that a single column of actions above a wall of episodes stops
        // making sense — from here on the actions and the episode list get a rail of
        // their own beside the rest, the way a title page reads on a couch rather than a
        // phone in a hand.
        val isWide = windowSizeClassOf(maxWidth) !in setOf(WindowSizeClass.Compact, WindowSizeClass.Medium)

        // The bar is not a Material scroll behaviour: those collapse a headline the bar
        // owns, and what collapses here is the first item of the list. So the state is
        // read straight off the list instead, and drives the fade by hand.
        val listState = rememberLazyListState()
        val collapseDistance = with(LocalDensity.current) {
            (backdropHeight - TopBarHeight).coerceAtLeast(0.dp).toPx()
        }
        val collapse by remember(collapseDistance, showBackdrop) {
            derivedStateOf {
                when {
                    // Nothing to collapse: the bar starts solid and stays that way.
                    !showBackdrop -> 1f
                    // Past the backdrop entirely: nothing left of it to reveal.
                    listState.firstVisibleItemIndex > 0 -> 1f
                    collapseDistance <= 0f -> 1f
                    else -> (listState.firstVisibleItemScrollOffset / collapseDistance)
                        .coerceIn(0f, 1f)
                }
            }
        }

        // The bar sits above the status bar inset, so clearing it means clearing both.
        val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val topPadding = if (showBackdrop) 0.dp else TopBarHeight + topInset

        // Where the backdrop is not a band across the top, the artwork becomes the page's
        // own background instead of being dropped. Drawn first, so everything else sits
        // over it, and fixed rather than scrolling — a wash that scrolls away is just the
        // backdrop again, moved.
        if (!showBackdrop) {
            BackdropWash(meta?.background)
        }

        val current = meta
        when {
            loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            current == null -> Text(
                text = stringResource(Res.string.detail_no_addon),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
            )
            else -> MetaContent(
                meta = current,
                backdropHeight = backdropHeight,
                listState = listState,
                backdropAlpha = 1f - collapse,
                showBackdrop = showBackdrop,
                topPadding = topPadding,
                isWide = isWide,
                positions = positions,
                resolvingVideoId = resolvingVideoId,
                onChooseSource = { videoId, resumeProgress ->
                    // A resolve already in flight is left to finish rather than started
                    // twice, so a second tap on "Resume" while it is running does nothing.
                    val rememberedStreamId = positions[videoId]?.streamId
                    if (resolvingVideoId != null) {
                        // ignored: a resolve for another video is already running
                    } else if (rememberedStreamId == null) {
                        sourcesFor = SourcesTarget(videoId, resumeProgress)
                    } else {
                        resolvingVideoId = videoId
                        scope.launch {
                            val currentMeta = meta
                            val playType = currentMeta?.type ?: type
                            val resolved = resolveRememberedSource(playType, videoId, rememberedStreamId)
                            resolvingVideoId = null
                            if (resolved == null) {
                                // No addon lists this source any more, peer-to-peer has no
                                // consent, or the swarm could not be reached — any of which
                                // means falling back to letting the source be picked by hand.
                                sourcesFor = SourcesTarget(videoId, resumeProgress)
                            } else {
                                val video = currentMeta?.videos?.firstOrNull { it.id == videoId }
                                onPlay(
                                    PlaybackRequest(
                                        url = resolved.url,
                                        type = playType,
                                        imdbId = currentMeta?.id ?: id,
                                        title = currentMeta?.name,
                                        videoId = videoId,
                                        episodeTitle = video?.title,
                                        season = video?.season,
                                        episode = video?.episode,
                                        resumeProgressPercent = resumeProgress,
                                        streamId = resolved.streamId,
                                    ),
                                )
                            }
                        }
                    }
                },
            )
        }

        // Transparent over the artwork, solid once the backdrop has gone, and the title
        // arrives with it — the name is already spelled out below while the backdrop is
        // still on screen, so showing it twice at once would be the odd state.
        TopAppBar(
            title = {
                Text(
                    text = meta?.name.orEmpty(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.graphicsLayer { alpha = collapse },
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.back),
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
                    .copy(alpha = collapse),
            ),
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
                        title = currentMeta?.name,
                        videoId = openFor.videoId,
                        episodeTitle = video?.title,
                        season = video?.season,
                        episode = video?.episode,
                        resumeProgressPercent = openFor.resumeProgressPercent,
                        streamId = source.streamId,
                    ),
                )
            },
        )
    }
}

/** Which video the sources sheet is open for, and the resume point "Watch now" picked. */
private data class SourcesTarget(val videoId: String, val resumeProgressPercent: Float? = null)

@Composable
private fun MetaContent(
    meta: Meta,
    backdropHeight: Dp,
    listState: LazyListState,
    backdropAlpha: Float,
    showBackdrop: Boolean,
    topPadding: Dp,
    isWide: Boolean,
    positions: Map<String, PlaybackPosition>,
    /** The video, if any, currently being resolved against its remembered source. */
    resolvingVideoId: String?,
    onChooseSource: (String, Float?) -> Unit,
) {
    val openUrl = rememberUrlOpener()
    // Specials last. They routinely spoil the run they belong to, so leading with them is
    // the wrong default even though their season number sorts first.
    val seasons = meta.videos
        .groupBy { it.season ?: 0 }
        .toList()
        .sortedWith(compareBy({ (season, _) -> season == SpecialsSeason }, { it.first }))
    val expanded = rememberSaveable { mutableStateOf(seasons.firstOrNull()?.first ?: 1) }
    // Set once Simkl's watched list has landed, which it does after the page is already
    // drawn — and never again after the viewer has opened a season themselves, since at
    // that point they have said which one they want open.
    var seasonChosenByViewer by rememberSaveable { mutableStateOf(false) }

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

    // The one a viewer is part way through is the one worth opening; failing that the
    // first, which is what a series nobody has started should show.
    LaunchedEffect(seasons, watchedIds, seasonChosenByViewer) {
        if (seasonChosenByViewer) return@LaunchedEffect
        expanded.value = inProgressSeason(seasons, watchedIds)
            ?: seasons.firstOrNull()?.first
            ?: return@LaunchedEffect
    }

    val onToggleWatched: (Video) -> Unit = { video ->
        EpisodeWatchedRepository.setWatched(meta.id, video, video.id !in watchedIds)
    }

    // Moving a title between lists is a write plus the sync that reads the result back,
    // so the button reports itself busy until both have finished.
    val authState by SimklRepository.authState.collectAsState()
    val scope = rememberCoroutineScope()
    var movingList by remember(meta.id) { mutableStateOf(false) }

    val watchAction: @Composable () -> Unit = {
        WatchAction(
            meta = meta,
            simklWatchedEpisodes = simklWatchedEpisodes,
            resumeSession = resumeSession,
            positions = positions,
            resolvingVideoId = resolvingVideoId,
            onChooseSource = onChooseSource,
        )
    }
    val watchlistAction: (@Composable () -> Unit)? = if (authState is SimklAuthState.SignedIn) {
        {
            WatchlistAction(
                currentStatus = simklItem?.status,
                working = movingList,
                onSelect = { status ->
                    scope.launch {
                        movingList = true
                        SimklSync.setListStatus(
                            imdbId = meta.id,
                            isMovie = meta.type == "movie",
                            status = status,
                            nowMillis = currentTimeMillis(),
                        )
                        movingList = false
                    }
                },
            )
        }
    } else {
        null
    }
    val onChooseEpisode: (String) -> Unit = { videoId -> onChooseSource(videoId, null) }

    if (isWide) {
        // The actions and the episode list move into a rail of their own beside the rest
        // — a show gets its episodes there, a film just the actions, above nothing — so
        // neither stretches across a window wide enough to sit two of them side by side.
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = DetailWideMaxWidth)
                    .padding(top = topPadding),
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentPadding = PaddingValues(bottom = 24.dp, end = 24.dp),
                ) {
                    item { DetailHeader(meta, backdropHeight, backdropAlpha, showBackdrop) }
                    item { WatchProgress(meta, simklItem, watchedIds) }
                    item { Ratings(meta) }
                    item { Synopsis(meta) }
                    castAndCrew(meta)
                    tagsSection(meta)
                    factsSection(meta)
                    trailersSection(meta, openUrl)
                    backdropsSection(meta)
                    item { Spacer(Modifier.height(32.dp)) }
                }
                Column(modifier = Modifier.width(DetailSidebarWidth).fillMaxHeight()) {
                    watchAction()
                    watchlistAction?.invoke()
                    if (seasons.isNotEmpty()) {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            item {
                                SectionHeader(stringResource(Res.string.detail_episodes), Icons.Outlined.Tv)
                            }
                            seasonSection(
                                seasons = seasons,
                                expanded = expanded,
                                onSeasonToggled = { seasonChosenByViewer = true },
                                watchedIds = watchedIds,
                                positions = positions,
                                onChooseSource = onChooseEpisode,
                                onToggleWatched = onToggleWatched,
                            )
                            item { Spacer(Modifier.height(24.dp)) }
                        }
                    }
                }
            }
        }
    } else {
        // Capped and centered the same way the rest of the app's wide-window screens
        // are — a no-op on a phone, which is already narrower than the cap.
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().widthIn(max = maxContentWidth).fillMaxHeight(),
                contentPadding = PaddingValues(top = topPadding),
            ) {
                item { DetailHeader(meta, backdropHeight, backdropAlpha, showBackdrop) }
                item { watchAction() }
                watchlistAction?.let { action -> item { action() } }
                item { WatchProgress(meta, simklItem, watchedIds) }
                item { Ratings(meta) }
                item { Synopsis(meta) }

                // A movie has no seasons at all, so the header only appears for a title
                // that actually has episodes under it — otherwise it is a headline over
                // nothing, on every single movie.
                if (seasons.isNotEmpty()) {
                    item { SectionHeader(stringResource(Res.string.detail_episodes), Icons.Outlined.Tv) }
                }
                seasonSection(
                    seasons = seasons,
                    expanded = expanded,
                    onSeasonToggled = { seasonChosenByViewer = true },
                    watchedIds = watchedIds,
                    positions = positions,
                    onChooseSource = onChooseEpisode,
                    onToggleWatched = onToggleWatched,
                )
                castAndCrew(meta)
                tagsSection(meta)
                factsSection(meta)
                trailersSection(meta, openUrl)
                backdropsSection(meta)

                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

/** Total width the two-column layout is allowed to grow to, centered beyond it — a
 * window wide enough for a rail is not automatically wide enough that the rail should
 * grow with it too. */
private val DetailWideMaxWidth = 1400.dp

/** Fixed width for the actions-and-episodes rail beside the main column. */
private val DetailSidebarWidth = 360.dp
