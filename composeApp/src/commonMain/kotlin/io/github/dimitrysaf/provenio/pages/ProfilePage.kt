package io.github.dimitrysaf.provenio.pages

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Person
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
    val profile by SimklRepository.profile.collectAsState()
    val watching by SimklSync.watching.collectAsState()
    val planToWatch by SimklSync.planToWatch.collectAsState()
    val syncState by SimklSync.state.collectAsState()
    val signedIn = authState is SimklAuthState.SignedIn
    val libraryEmpty = watching.isEmpty() && planToWatch.isEmpty()

    val hasMetadata = collection.active.any { addon ->
        addon.manifest.resources.any { it.name == "meta" }
    }

    // The backdrop is the thing the user is furthest into. Simkl does not return fanart in
    // the library payload, so it comes from the metadata addon that already knows the
    // title, which costs nothing extra on the Simkl quota.
    val featured = watching.firstOrNull()
    var backdrop by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(featured?.simklId, hasMetadata) {
        backdrop = null
        val item = featured ?: return@LaunchedEffect
        val imdb = item.imdbId ?: return@LaunchedEffect
        if (!hasMetadata) return@LaunchedEffect
        backdrop = AddonRepository.meta(item.stremioType(), imdb)?.background
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
        AccountHeader(
            name = profile?.name,
            avatarUrl = profile?.avatarUrl,
            backdropUrl = backdrop,
            signedIn = signedIn,
        )
        Spacer(Modifier.height(16.dp))

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

        Shelf("Continue watching", watching, onOpenDetail)
        Shelf("Plan to watch", planToWatch, onOpenDetail)
    }
}

/** Simkl types its libraries as shows, movies and anime. The addon protocol does not. */
private fun SimklItem.stremioType(): String = if (mediaType == "movies") "movie" else "series"

/**
 * Backdrop, avatar and name.
 *
 * With artwork the identity sits over it. Without, the header collapses to a compact row
 * rather than reserving a 16:9 block for a picture that is not coming. Holding the height
 * keeps the layout from shifting, but the cost is a large empty rectangle, and an empty
 * rectangle is worse than a smaller header.
 */
@Composable
private fun AccountHeader(
    name: String?,
    avatarUrl: String?,
    backdropUrl: String?,
    signedIn: Boolean,
) {
    if (backdropUrl == null) {
        CompactHeader(name = name, avatarUrl = avatarUrl, signedIn = signedIn)
        return
    }

    Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
        AsyncImage(
            model = backdropUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        // Scrim only where the text sits, so the artwork stays visible above it.
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.55f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.75f),
                ),
            ),
        )
        Identity(
            name = name,
            avatarUrl = avatarUrl,
            signedIn = signedIn,
            onArtwork = true,
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
        )
    }
}

@Composable
private fun CompactHeader(name: String?, avatarUrl: String?, signedIn: Boolean) {
    Identity(
        name = name,
        avatarUrl = avatarUrl,
        signedIn = signedIn,
        onArtwork = false,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun Identity(
    name: String?,
    avatarUrl: String?,
    signedIn: Boolean,
    onArtwork: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            if (avatarUrl != null) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = name ?: if (signedIn) "Simkl" else "Not signed in",
                style = MaterialTheme.typography.titleLarge,
                color = if (onArtwork) Color.White else Color.Unspecified,
            )
            if (!signedIn) {
                Text(
                    text = "Connect Simkl to sync your library",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (onArtwork) {
                        Color.White.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

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
        // Shown only when there is progress, matching the detail page.
        if (item.totalEpisodes > 0 && item.watchedEpisodes > 0) {
            LinearProgressIndicator(
                progress = { item.watchedEpisodes.toFloat() / item.totalEpisodes },
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            )
            Text(
                text = "${item.watchedEpisodes}/${item.totalEpisodes}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
