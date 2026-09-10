package io.github.dimitrysaf.provenio.feature.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.dimitrysaf.provenio.core.platform.currentTimeMillis
import io.github.dimitrysaf.provenio.designsystem.components.AppTopBar
import io.github.dimitrysaf.provenio.designsystem.components.EmptyState
import io.github.dimitrysaf.provenio.designsystem.components.ScreenScaffold
import io.github.dimitrysaf.provenio.feature.home.components.ContinueWatchingCarousel
import io.github.dimitrysaf.provenio.feature.home.components.LibraryEmptyState
import io.github.dimitrysaf.provenio.feature.home.components.Shelf
import io.github.dimitrysaf.provenio.simkl.SimklAuthState
import io.github.dimitrysaf.provenio.simkl.SimklPlaybackRepository
import io.github.dimitrysaf.provenio.simkl.SimklPlaybackSession
import io.github.dimitrysaf.provenio.simkl.SimklRepository
import io.github.dimitrysaf.provenio.simkl.SimklSync
import io.github.dimitrysaf.provenio.simkl.SyncTrigger
import io.github.dimitrysaf.provenio.stremio.AddonRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
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

    // The shelf itself is Simkl's "watching" list — the same one this screen has always
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

    ScreenScaffold(
        modifier = modifier,
        topBar = { scrollBehavior ->
            AppTopBar(
                title = "Home",
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                },
            )
        },
    ) {
        if (!hasMetadata) {
            EmptyState(
                icon = Icons.Outlined.Extension,
                title = "No metadata available",
                description = "Add an add-on to get started.",
                actionLabel = "Add an add-on",
                onAction = onAddAddons,
            )
            return@ScreenScaffold
        }

        // Two shelves both saying "nothing" is a worse answer than one that explains why.
        if (libraryEmpty) {
            LibraryEmptyState(
                signedIn = signedIn,
                syncState = syncState,
                onOpenSettings = onSettingsClick,
                onRetry = { SimklSync.sync(SyncTrigger.Manual, currentTimeMillis()) },
            )
            return@ScreenScaffold
        }

        ContinueWatchingCarousel(watching, timeProgressByImdbId, onOpenDetail)
        Shelf("Plan to watch", planToWatch, onOpenDetail)
    }
}
