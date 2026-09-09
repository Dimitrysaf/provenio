package io.github.dimitrysaf.provenio.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.dimitrysaf.provenio.db.SimklItem
import io.github.dimitrysaf.provenio.simkl.SimklAuthState
import io.github.dimitrysaf.provenio.simkl.SimklImages
import io.github.dimitrysaf.provenio.simkl.SimklPlaybackRepository
import io.github.dimitrysaf.provenio.simkl.SimklPlaybackSession
import io.github.dimitrysaf.provenio.simkl.SimklRepository
import io.github.dimitrysaf.provenio.simkl.SimklSync
import io.github.dimitrysaf.provenio.simkl.SyncState
import io.github.dimitrysaf.provenio.simkl.SyncTrigger
import io.github.dimitrysaf.provenio.util.currentTimeMillis
import io.github.dimitrysaf.provenio.stremio.AddonRepository
import io.github.dimitrysaf.provenio.ui.components.AppTopBar
import io.github.dimitrysaf.provenio.ui.components.PageScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilePage(
    modifier: Modifier = Modifier,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAddAddons: () -> Unit,
    onOpenDetail: (type: String, id: String) -> Unit,
) {
    val collection by AddonRepository.collection.collectAsState()
    val authState by SimklRepository.authState.collectAsState()
    val watching by SimklSync.watching.collectAsState()
    val planToWatch by SimklSync.planToWatch.collectAsState()
    val syncState by SimklSync.state.collectAsState()
    val signedIn = authState is SimklAuthState.SignedIn
    val libraryEmpty = watching.isEmpty() && planToWatch.isEmpty()

    val hasMetadata = collection.active.any { addon ->
        addon.manifest.resources.any { it.name == "meta" }
    }

    // The shelf itself is Simkl's "watching" list — the same one this page has always
    // shown — so it is never empty just because nothing has a live scrobble session yet.
    // Per card, a matching playback session (when Simkl has one) supplies the one thing
    // the watching list itself cannot: real time-into-this-episode progress, keyed by
    // imdb id so each card can look up its own without a second round trip.
    var timeProgressByImdbId by remember { mutableStateOf<Map<String, SimklPlaybackSession>>(emptyMap()) }
    LaunchedEffect(signedIn) {
        timeProgressByImdbId = if (signedIn) {
            SimklPlaybackRepository.continueWatching()
                .mapNotNull { session -> session.media?.ids?.imdb?.let { it to session } }
                .toMap()
        } else {
            emptyMap()
        }
    }

    PageScaffold(
        modifier = modifier,
        topBar = { scrollBehavior ->
            AppTopBar(
                title = "Profile",
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) {
        if (!hasMetadata) {
            NoMetadataState(onAddAddons = onAddAddons)
            return@PageScaffold
        }

        // Two shelves both saying "nothing" is a worse answer than one that explains why.
        if (libraryEmpty) {
            LibraryEmptyState(
                signedIn = signedIn,
                syncState = syncState,
                onOpenSettings = onSettingsClick,
                onRetry = { SimklSync.sync(SyncTrigger.Manual, currentTimeMillis()) },
            )
            return@PageScaffold
        }

        ContinueWatchingCarousel(watching, timeProgressByImdbId, onOpenDetail)
        Shelf("Plan to watch", planToWatch, onOpenDetail)
    }
}

/** Simkl types its libraries as shows, movies and anime. The addon protocol does not. */
private fun SimklItem.stremioType(): String = if (mediaType == "movies") "movie" else "series"

/**
 * The lead shelf: Simkl's "watching" list, same as this page has always shown, so it is
 * never empty just because nothing has a live scrobble session yet. A card's progress bar
 * is real time-into-this-episode from [timeProgressByImdbId] when Simkl has that session,
 * and simply absent otherwise — an episode-count fraction is a different metric, not a
 * rougher version of the same one, so there is no fallback bar to draw without it.
 */
@Composable
private fun ContinueWatchingCarousel(
    items: List<SimklItem>,
    timeProgressByImdbId: Map<String, SimklPlaybackSession>,
    onOpenDetail: (type: String, id: String) -> Unit,
) {
    if (items.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        Text(
            text = "Continue watching",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items.forEach { item ->
                val session = item.imdbId?.let { timeProgressByImdbId[it] }
                ContinueWatchingCard(item, session) {
                    item.imdbId?.let { onOpenDetail(item.stremioType(), it) }
                }
            }
        }
    }
}

@Composable
private fun ContinueWatchingCard(
    item: SimklItem,
    session: SimklPlaybackSession?,
    onClick: () -> Unit,
) {
    val poster = SimklImages.poster(item.poster)
    // The watching list has no per-episode number of its own; a matched session's is the
    // one actually being resumed, so it is shown only when there is one to show.
    val episode = session?.episode

    Column(modifier = Modifier.width(150.dp).clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            if (poster != null) {
                AsyncImage(
                    model = poster,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            // Scrim only behind the label, the same reasoning as the details page hero.
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.55f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.78f),
                    ),
                ),
            )
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (episode != null) {
                    Text(
                        text = "S${pad(episode.season)} · E${pad(episode.number)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.75f),
                    )
                }
                // The position itself, in time — not an episode count — and no label:
                // the bar sitting on the art is the whole point, the same way a video
                // scrubber never needs to spell out what it is. Nothing drawn at all
                // when Simkl has not reported one.
                if (session != null) {
                    LinearProgressIndicator(
                        progress = { (session.progress / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(3.dp),
                        trackColor = Color.White.copy(alpha = 0.25f),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

/** Zero padded episode and season numbers. Common Kotlin has no String.format. */
private fun pad(value: Int): String = value.toString().padStart(2, '0')

/**
 * One shelf. Never rendered empty: a heading over nothing is noise, so the page shows a
 * single explanation instead when the whole library is empty.
 */
@Composable
private fun Shelf(
    title: String,
    items: List<SimklItem>,
    onOpenDetail: (type: String, id: String) -> Unit,
) {
    if (items.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items.forEach { item ->
                LibraryCard(item) {
                    item.imdbId?.let { onOpenDetail(item.stremioType(), it) }
                }
            }
        }
    }
}

@Composable
private fun LibraryCard(item: SimklItem, onClick: () -> Unit) {
    Column(modifier = Modifier.width(120.dp).clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            val poster = SimklImages.poster(item.poster)
            if (poster != null) {
                AsyncImage(
                    model = poster,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/**
 * Says why the shelves are empty.
 *
 * A failed sync and a genuinely empty library produced the same blank screen before this,
 * which made a broken sync indistinguishable from having watched nothing.
 */
@Composable
private fun LibraryEmptyState(
    signedIn: Boolean,
    syncState: SyncState,
    onOpenSettings: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when {
            !signedIn -> {
                Icon(
                    imageVector = Icons.Outlined.CloudSync,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("Your library lives on Simkl", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Sign in to bring what you are watching and planning to watch " +
                        "into Provenio.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Button(onClick = onOpenSettings) { Text("Sign in to Simkl") }
            }
            syncState is SyncState.Running -> {
                CircularProgressIndicator()
                Text(
                    text = "Syncing your library",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            syncState is SyncState.Failed -> {
                Icon(
                    imageVector = Icons.Outlined.CloudOff,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
                Text("Sync did not finish", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = syncState.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Button(onClick = onRetry) { Text("Try again") }
            }
            else -> {
                Icon(
                    imageVector = Icons.Outlined.Bookmarks,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("Nothing tracked yet", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Anything you mark as watching or plan to watch on Simkl shows " +
                        "up here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Button(onClick = onRetry) { Text("Refresh") }
            }
        }
    }
}

@Composable
private fun NoMetadataState(onAddAddons: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Extension,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = "No metadata available", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Add an add-on to get started.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onAddAddons) { Text("Add an add-on") }
    }
}
