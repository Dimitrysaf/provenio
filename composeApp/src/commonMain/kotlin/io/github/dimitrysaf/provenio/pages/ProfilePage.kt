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
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.dimitrysaf.provenio.db.SimklItem
import io.github.dimitrysaf.provenio.simkl.SimklAuthState
import io.github.dimitrysaf.provenio.simkl.SimklImages
import io.github.dimitrysaf.provenio.simkl.SimklRepository
import io.github.dimitrysaf.provenio.simkl.SimklSync
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
    val signedIn = authState is SimklAuthState.SignedIn

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

        Shelf(
            title = "Continue watching",
            items = watching,
            emptyText = if (signedIn) {
                "Nothing in progress."
            } else {
                "Sign in to Simkl to see what you are watching."
            },
            onOpenDetail = onOpenDetail,
        )
        Shelf(
            title = "Plan to watch",
            items = planToWatch,
            emptyText = if (signedIn) {
                "Nothing planned."
            } else {
                "Sign in to Simkl to see your watchlist."
            },
            onOpenDetail = onOpenDetail,
        )
    }
}

/** Simkl types its libraries as shows, movies and anime. The addon protocol does not. */
private fun SimklItem.stremioType(): String = if (mediaType == "movies") "movie" else "series"

/**
 * Backdrop, avatar and name.
 *
 * The backdrop sits behind the identity rather than beside it, so the page opens on the
 * thing the user is actually watching. Without one it falls back to a tonal surface rather
 * than collapsing, which keeps the header the same height either way.
 */
@Composable
private fun AccountHeader(
    name: String?,
    avatarUrl: String?,
    backdropUrl: String?,
    signedIn: Boolean,
) {
    Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
        if (backdropUrl != null) {
            AsyncImage(
                model = backdropUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            )
        }

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

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
                    color = if (backdropUrl != null) Color.White else Color.Unspecified,
                )
                if (!signedIn) {
                    Text(
                        text = "Connect Simkl to sync your library",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (backdropUrl != null) {
                            Color.White.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun Shelf(
    title: String,
    items: List<SimklItem>,
    emptyText: String,
    onOpenDetail: (type: String, id: String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        if (items.isEmpty()) {
            Text(
                text = emptyText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            return
        }
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
