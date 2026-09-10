package io.github.dimitrysaf.provenio.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.core.platform.currentTimeMillis
import io.github.dimitrysaf.provenio.designsystem.components.AppTopBar
import io.github.dimitrysaf.provenio.designsystem.components.EmptyState
import io.github.dimitrysaf.provenio.feature.home.components.ContinueWatchingCarousel
import io.github.dimitrysaf.provenio.feature.home.components.HeroCarousel
import io.github.dimitrysaf.provenio.feature.home.components.LibraryEmptyState
import io.github.dimitrysaf.provenio.feature.home.components.Shelf
import io.github.dimitrysaf.provenio.feature.home.components.catalogShelves
import io.github.dimitrysaf.provenio.simkl.SimklAuthState
import io.github.dimitrysaf.provenio.simkl.SimklPlaybackRepository
import io.github.dimitrysaf.provenio.simkl.SimklPlaybackSession
import io.github.dimitrysaf.provenio.simkl.SimklRepository
import io.github.dimitrysaf.provenio.simkl.SimklSync
import io.github.dimitrysaf.provenio.simkl.SyncTrigger
import io.github.dimitrysaf.provenio.stremio.AddonRepository
import io.github.dimitrysaf.provenio.stremio.model.MetaPreview

/** Clears the floating brand mark, so the first shelf does not start underneath it. */
private val TopBarHeight = 64.dp

/** How many titles the hero cycles through before it repeats. */
private const val HeroCount = 10

/**
 * Continue watching, then the library, then everything the add-ons offer.
 *
 * The brand mark floats over the content rather than sitting in a bar above it — the same
 * arrangement as the details screen's back button — so the shelves run the full height of
 * the window and nothing reserves a strip at the top.
 *
 * Simkl rows draw only when they have something in them. An empty or signed-out library
 * used to replace this whole screen with a prompt, which also hid every add-on shelf; the
 * prompt now appears only when there is nothing else to show at all.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
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
    val shelves = collection.browsableCatalogs()

    // The spotlight comes off the first browsable catalog, which is the first catalog of
    // the highest priority addon — so reordering add-ons in settings changes what leads
    // the screen, the same control that orders the shelves below.
    val featured = shelves.firstOrNull()
    var heroItems by remember { mutableStateOf<List<MetaPreview>>(emptyList()) }
    LaunchedEffect(featured?.let { "${it.first.manifest.id}:${it.second.id}" }) {
        heroItems = featured
            ?.let { (addon, catalog) -> AddonRepository.catalogPage(addon, catalog, skip = 0) }
            ?.take(HeroCount)
            .orEmpty()
    }

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

    // The bar sits above the status bar inset, so clearing it means clearing both. The
    // hero is the exception: it is meant to run under the bar and the status bar both, so
    // when there is one the list starts flush against the top instead.
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topPadding = if (heroItems.isEmpty()) TopBarHeight + topInset else 0.dp

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = topPadding, bottom = 24.dp),
        ) {
            if (!hasMetadata) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.Extension,
                        title = "No metadata available",
                        description = "Add an add-on to get started.",
                        actionLabel = "Add an add-on",
                        onAction = onAddAddons,
                    )
                }
                return@LazyColumn
            }

            item { HeroCarousel(heroItems, onOpenDetail) }
            item { ContinueWatchingCarousel(watching, timeProgressByImdbId, onOpenDetail) }
            item { Shelf("Plan to watch", planToWatch, onOpenDetail) }

            // Only worth explaining the empty library when it is the only thing missing.
            // With shelves to browse, a sign-in prompt on top of them is just noise.
            if (libraryEmpty && shelves.isEmpty()) {
                item {
                    LibraryEmptyState(
                        signedIn = signedIn,
                        syncState = syncState,
                        onOpenSettings = onSettingsClick,
                        onRetry = { SimklSync.sync(SyncTrigger.Manual, currentTimeMillis()) },
                    )
                }
            }

            catalogShelves(shelves = shelves, onOpenDetail = onOpenDetail)

            item { Spacer(Modifier.height(8.dp)) }
        }

        AppTopBar(
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        )
    }
}
