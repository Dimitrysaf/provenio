package io.github.dimitrysaf.provenio.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.AsyncImage
import io.github.dimitrysaf.provenio.db.SimklItem
import io.github.dimitrysaf.provenio.simkl.SimklAuthState
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
) {
    val collection by AddonRepository.collection.collectAsState()
    val simklAuth by SimklRepository.authState.collectAsState()
    val watching by SimklSync.watching.collectAsState()
    val planToWatch by SimklSync.planToWatch.collectAsState()
    val signedIn = simklAuth is SimklAuthState.SignedIn
    // Every section on this page is metadata. Without an addon that serves it there is
    // nothing to show and nothing that could arrive later.
    val hasMetadata = collection.active.any { addon ->
        addon.manifest.resources.any { it.name == "meta" }
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
        AccountHeader(signedIn = signedIn)
        Spacer(Modifier.height(24.dp))

        if (!hasMetadata) {
            NoMetadataState(onAddAddons = onAddAddons)
        } else {
            LibrarySection(
                title = "Continue watching",
                items = watching,
                emptyText = if (signedIn) {
                    "Nothing in progress."
                } else {
                    "Sign in to Simkl to see what you are watching."
                },
            )
            LibrarySection(
                title = "Plan to watch",
                items = planToWatch,
                emptyText = if (signedIn) {
                    "Nothing planned."
                } else {
                    "Sign in to Simkl to see your watchlist."
                },
            )
            LibrarySection(
                title = "Airing next",
                items = emptyList(),
                emptyText = "Not available yet.",
            )
        }
    }
}

/**
 * Stands in for the signed-in account.
 *
 * Simkl is what will fill this once it exists. Until then the placeholder says so rather
 * than showing a fake name.
 */
@Composable
private fun AccountHeader(signedIn: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                text = if (signedIn) "Simkl" else "Not signed in",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = if (signedIn) {
                    "Library synced"
                } else {
                    "Connect Simkl to sync your library"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A library shelf, filled from the synced Simkl library. */
@Composable
private fun LibrarySection(title: String, items: List<SimklItem>, emptyText: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
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
            items.forEach { item -> LibraryCard(item) }
        }
    }
}

@Composable
private fun LibraryCard(item: SimklItem) {
    Column(modifier = Modifier.width(120.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            // Simkl returns a poster path rather than a URL, so it needs their CDN prefix.
            if (!item.poster.isNullOrBlank()) {
                AsyncImage(
                    model = "https://wsrv.nl/?url=simkl.in/posters/${item.poster}_m.jpg",
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
        // Only shown once there is progress to show, matching the detail page.
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
