package io.github.dimitrysaf.provenio.pages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Subject
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.dimitrysaf.provenio.db.SimklItem
import io.github.dimitrysaf.provenio.simkl.SimklSync
import io.github.dimitrysaf.provenio.simkl.SyncState
import io.github.dimitrysaf.provenio.simkl.toSimklEpisodeCode
import io.github.dimitrysaf.provenio.stremio.AddonRepository
import io.github.dimitrysaf.provenio.stremio.model.Meta
import io.github.dimitrysaf.provenio.stremio.model.Video
import io.github.dimitrysaf.provenio.ui.SourcesSheet
import io.github.dimitrysaf.provenio.ui.rememberUrlOpener
import io.github.dimitrysaf.provenio.watch.EpisodeWatchedRepository

/**
 * The full record for one title.
 *
 * Sections are always present, with an empty state when the addon or the app cannot fill
 * them, so the page has a stable shape regardless of how thin a particular addon's data
 * is. Individual facts inside a section are still dropped when null rather than printed
 * as blanks.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailPage(
    type: String,
    id: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onPlay: (String) -> Unit,
) {
    var meta by remember { mutableStateOf<Meta?>(null) }
    var loading by remember { mutableStateOf(true) }
    // Which title the sources sheet is open for, or null when it is closed.
    var sourcesFor by remember { mutableStateOf<String?>(null) }

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
            else -> MetaContent(current) { sourcesFor = it }
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
            id = openFor,
            title = meta?.name,
            onDismiss = { sourcesFor = null },
            onPlay = { source ->
                sourcesFor = null
                source.playableUrl?.let(onPlay)
            },
        )
    }
}

@Composable
private fun MetaContent(meta: Meta, onChooseSource: (String) -> Unit) {
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

    // Simkl never sends a per-episode watched list, only the aggregate count above and a
    // next-to-watch marker, so every regular episode before that marker is inferred
    // watched from it. Local overrides win over that inference in either direction — see
    // EpisodeWatchedRepository for why an override is not simply "watched" or absent.
    val overrides by EpisodeWatchedRepository.overrides.collectAsState()
    val watchedIds = remember(meta, simklItem, overrides) {
        val inferred = simklWatchedIds(meta, simklItem)
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
        item { Header(meta) }
        item { WatchAction(meta, simklItem, onChooseSource) }
        item { WatchProgress(meta, simklItem, watchedIds) }
        item { Ratings(meta) }
        item { Synopsis(meta) }

        seasonSection(seasons, expanded, watchedIds, onChooseSource, onToggleWatched)
        castAndCrew(meta)
        tagsAndThemes(meta)
        commentsSection()
        factsSection(meta)
        trailersSection(meta, openUrl)
        backdropsSection(meta)

        item { Spacer(Modifier.height(32.dp)) }
    }
}

/** Backdrop behind, poster and title in front. */
@Composable
private fun Header(meta: Meta) {
    Column {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
            if (meta.background != null) {
                AsyncImage(
                    model = meta.background,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                )
            }
            // Keeps the back button and the title legible over a bright still.
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.45f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.55f),
                        ),
                    ),
                ),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(110.dp)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                if (meta.poster != null) {
                    AsyncImage(
                        model = meta.poster,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = meta.name ?: "Unknown",
                    style = MaterialTheme.typography.headlineSmall,
                )
                val facts = listOfNotNull(
                    meta.releaseInfo,
                    meta.runtime,
                    meta.trackedEpisodeCount()
                        .takeIf { it > 0 }
                        ?.let { "$it eps" },
                )
                if (facts.isNotEmpty()) {
                    Text(
                        text = facts.joinToString("  ·  "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}

/**
 * The primary action. Resumes at whatever Simkl says is next, when it has an opinion;
 * otherwise opens the first episode of the first regular season, never a special.
 */
@Composable
private fun WatchAction(meta: Meta, simklItem: SimklItem?, onChooseSource: (String) -> Unit) {
    val next = simklNextEpisode(meta, simklItem) ?: meta.firstRegularEpisode()
    val label = when {
        next?.season != null && next.episode != null ->
            "Watch S${pad(next.season)}E${pad(next.episode)} now"
        meta.videos.isNotEmpty() -> "Watch first episode now"
        else -> "Watch now"
    }

    // A series plays its first episode; a film plays itself. Either way the id decides
    // which streams the addons are asked for.
    val playId = next?.id ?: meta.id

    Button(
        onClick = { onChooseSource(playId) },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
        Icon(Icons.Filled.PlayArrow, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

/**
 * Simkl's synced count is preferred when it has one, because it reflects everything the
 * user has ever marked watched, on this device or anywhere else Simkl is connected.
 * The local, per-episode tally ([watchedIds]) is only the fallback, for a title Simkl has
 * no record of yet or while signed out.
 */
@Composable
private fun WatchProgress(meta: Meta, simklItem: SimklItem?, watchedIds: Set<String>) {
    val localWatched = meta.videos.count {
        (it.season ?: SpecialsSeason) != SpecialsSeason && it.id in watchedIds
    }
    val fromSimkl = simklItem?.takeIf { it.totalEpisodes > 0 }
    val total = fromSimkl?.totalEpisodes?.toInt() ?: meta.trackedEpisodeCount()
    val watched = fromSimkl?.watchedEpisodes?.toInt() ?: localWatched

    SectionCard(title = "Watch progress", icon = Icons.Outlined.Visibility) {
        // A bar pinned at zero says nothing that the text below it does not.
        if (watched > 0 && total > 0) {
            LinearProgressIndicator(
                progress = { (watched.toFloat() / total).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
        }
        Text(
            text = when {
                total == 0 -> "Not tracked yet"
                watched == 0 -> "Not started, $total episodes"
                else -> "$watched of $total episodes watched"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Ratings(meta: Meta) {
    SectionCard(title = "Ratings", icon = Icons.Outlined.StarOutline) {
        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            RatingBlock("IMDb", meta.imdbRating)
            RatingBlock("Simkl", null)
        }
    }
}

@Composable
private fun RatingBlock(source: String, value: String?) {
    Column {
        Text(
            text = source,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value ?: "Not rated",
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
private fun Synopsis(meta: Meta) {
    SectionCard(title = "Plot", icon = Icons.AutoMirrored.Outlined.Subject) {
        Text(
            text = meta.description ?: "No description provided",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun LazyListScope.seasonSection(
    seasons: List<Pair<Int, List<Video>>>,
    expanded: MutableState<Int>,
    watchedIds: Set<String>,
    onChooseSource: (String) -> Unit,
    onToggleWatched: (Video) -> Unit,
) {
    item { SectionHeader("Episodes", Icons.Outlined.Tv) }

    if (seasons.isEmpty()) {
        item { EmptyNote("No episodes listed for this title.") }
        return
    }

    seasons.forEach { (season, episodes) ->
        item {
            SeasonHeader(
                season = season,
                episodeCount = episodes.size,
                watched = episodes.count { it.id in watchedIds },
                expanded = expanded.value == season,
                onToggle = {
                    expanded.value = if (expanded.value == season) -1 else season
                },
            )
        }
        item {
            AnimatedVisibility(visible = expanded.value == season) {
                Column {
                    episodes.forEach { video ->
                        EpisodeRow(
                            video = video,
                            watched = video.id in watchedIds,
                            onClick = { onChooseSource(video.id) },
                            onToggleWatched = { onToggleWatched(video) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SeasonHeader(
    season: Int,
    episodeCount: Int,
    watched: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (season == SpecialsSeason) "Specials" else "Season $season",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "$watched/$episodeCount",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = if (expanded) {
                    Icons.Filled.KeyboardArrowUp
                } else {
                    Icons.Filled.KeyboardArrowDown
                },
                contentDescription = null,
            )
        }
        if (watched > 0) {
            LinearProgressIndicator(
                progress = { watched.toFloat() / episodeCount },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
private fun EpisodeRow(
    video: Video,
    watched: Boolean,
    onClick: () -> Unit,
    onToggleWatched: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(110.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            if (video.thumbnail != null) {
                AsyncImage(
                    model = video.thumbnail,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            val season = video.season
            val episode = video.episode
            if (season != null && episode != null) {
                Text(
                    text = "S${pad(season)} | E${pad(episode)}",
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            Text(
                text = video.title ?: "Episode",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        // A dedicated touch target, nested inside the row's own click target. Compose
        // resolves nested clickables front-to-back, so pressing the tick box toggles
        // watched state instead of also opening the sources sheet underneath it.
        IconButton(onClick = onToggleWatched) {
            Icon(
                imageVector = if (watched) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                contentDescription = if (watched) "Watched" else "Not watched",
                tint = if (watched) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

private fun LazyListScope.castAndCrew(meta: Meta) {
    item { SectionHeader("Cast and crew", Icons.Outlined.Group) }

    val people = meta.cast.map { it to "Cast" } + meta.director.map { it to "Director" }
    if (people.isEmpty()) {
        item { EmptyNote("This add-on did not list any cast or crew.") }
        return
    }
    item {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            people.forEach { (name, role) -> PersonCard(name, role) }
        }
    }
}

@Composable
private fun PersonCard(name: String, role: String) {
    Column(
        modifier = Modifier.width(96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The protocol carries names only, so there is no portrait to show.
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.take(1).uppercase(),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            text = role,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun LazyListScope.tagsAndThemes(meta: Meta) {
    item { SectionHeader("Tags", Icons.Outlined.Sell) }
    if (meta.genres.isEmpty()) {
        item { EmptyNote("No tags provided.") }
    } else {
        item { ChipFlow(meta.genres) }
    }

    item { SectionHeader("Themes", Icons.Outlined.Palette) }
    // Themes are a Simkl concept. The addon protocol has no equivalent field.
    item { EmptyNote("Themes are not available from add-ons.") }
}

private fun LazyListScope.commentsSection() {
    item { SectionHeader("Comments", Icons.Outlined.ChatBubbleOutline) }
    item { EmptyNote("Comments arrive with account sign-in.") }
}

private fun LazyListScope.factsSection(meta: Meta) {
    item { SectionHeader("Facts", Icons.Outlined.Info) }

    val facts = listOfNotNull(
        meta.released?.let { "Air date" to it.take(10) },
        meta.country?.let { "Country" to it },
        meta.language?.let { "Language" to it },
        meta.runtime?.let { "Runtime" to it },
        meta.awards?.let { "Awards" to it },
        meta.website?.let { "Website" to it },
    )
    if (facts.isEmpty()) {
        item { EmptyNote("No facts provided.") }
        return
    }
    item {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            facts.forEach { (label, value) ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(110.dp),
                    )
                    Text(text = value, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

private fun LazyListScope.trailersSection(meta: Meta, openUrl: (String) -> Unit) {
    item { SectionHeader("Trailers", Icons.Outlined.Movie) }
    if (meta.trailers.isEmpty()) {
        item { EmptyNote("No trailers provided.") }
        return
    }
    items(meta.trailers, key = { it.source }) { trailer ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { openUrl("https://www.youtube.com/watch?v=${trailer.source}") }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.PlayCircle, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text(
                text = trailer.type ?: "Trailer",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

private fun LazyListScope.backdropsSection(meta: Meta) {
    item { SectionHeader("Backdrops", Icons.Outlined.Image) }
    val art = listOfNotNull(meta.background, meta.poster, meta.logo)
    if (art.isEmpty()) {
        item { EmptyNote("No artwork provided.") }
        return
    }
    item {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            art.forEach { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .height(110.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Column {
        HorizontalDivider(modifier = Modifier.padding(top = 20.dp))
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(12.dp))
            Text(text = title, style = MaterialTheme.typography.titleLarge)
        }
    }
}

/** Season 0 is the specials bucket by convention across every metadata source. */
private const val SpecialsSeason = 0

/**
 * Episodes that count towards progress.
 *
 * Specials are excluded. They are supplementary, they are not part of the run a viewer is
 * working through, and counting them makes a finished series read as incomplete.
 */
private fun Meta.trackedEpisodeCount(): Int =
    videos.count { (it.season ?: SpecialsSeason) != SpecialsSeason }

/**
 * The episode "Watch now" opens: the lowest numbered episode of the lowest numbered
 * regular season. Never a special, regardless of how the addon orders [videos] — Season 0
 * routinely sorts first in raw addon data, and starting there is never what "watch now" on
 * a series means. Falls back to the very first video for the rare title that lists nothing
 * but specials, since playing something beats the button doing nothing.
 */
private fun Meta.firstRegularEpisode(): Video? =
    videos
        .filter { (it.season ?: SpecialsSeason) != SpecialsSeason }
        .minWithOrNull(compareBy({ it.season }, { it.episode }))
        ?: videos.firstOrNull()

/** The video matching Simkl's next-to-watch marker, when it has one and [meta] lists it. */
private fun simklNextEpisode(meta: Meta, simklItem: SimklItem?): Video? {
    val (season, episode) = simklItem?.nextToWatch?.toSimklEpisodeCode() ?: return null
    return meta.videos.firstOrNull { it.season == season && it.episode == episode }
}

/**
 * Regular episode ids Simkl considers already watched, inferred rather than read back
 * directly: Simkl's synced library never sends a per-episode list, only the aggregate
 * count [WatchProgress] already uses and the next-to-watch marker [simklNextEpisode]
 * reads. Every regular episode strictly before that marker counts as watched; with no
 * marker at all, everything does once Simkl's own counts agree there is nothing left,
 * and nothing does otherwise (a title Simkl has not synced episode-level data for).
 */
private fun simklWatchedIds(meta: Meta, simklItem: SimklItem?): Set<String> {
    if (simklItem == null || simklItem.totalEpisodes <= 0) return emptySet()
    val regular = meta.videos.filter { (it.season ?: SpecialsSeason) != SpecialsSeason }
    val next = simklItem.nextToWatch?.toSimklEpisodeCode()
        ?: return if (simklItem.watchedEpisodes >= simklItem.totalEpisodes) {
            regular.map { it.id }.toSet()
        } else {
            emptySet()
        }
    val (nextSeason, nextEpisode) = next
    return regular
        .filter { video ->
            val season = video.season ?: return@filter false
            val episode = video.episode ?: return@filter false
            season < nextSeason || (season == nextSeason && episode < nextEpisode)
        }
        .map { it.id }
        .toSet()
}

/** Zero padded episode and season numbers. Common Kotlin has no String.format. */
private fun pad(value: Int): String = value.toString().padStart(2, '0')

@Composable
private fun SectionCard(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            modifier = Modifier.padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(12.dp))
            Text(text = title, style = MaterialTheme.typography.titleMedium)
        }
        content()
    }
}

@Composable
private fun EmptyNote(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

@Composable
private fun ChipFlow(values: List<String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        values.forEach { value -> AssistChip(onClick = {}, label = { Text(value) }) }
    }
}
