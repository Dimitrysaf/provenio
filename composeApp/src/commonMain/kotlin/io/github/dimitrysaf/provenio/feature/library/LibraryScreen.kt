package io.github.dimitrysaf.provenio.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.db.SimklItem
import io.github.dimitrysaf.provenio.designsystem.components.EmptyState
import io.github.dimitrysaf.provenio.designsystem.components.PosterCard
import io.github.dimitrysaf.provenio.feature.library.components.LibraryControls
import io.github.dimitrysaf.provenio.feature.library.components.LibrarySort
import io.github.dimitrysaf.provenio.feature.library.components.listLabel
import io.github.dimitrysaf.provenio.feature.library.components.mediaTypeLabel
import io.github.dimitrysaf.provenio.simkl.SimklAuthState
import io.github.dimitrysaf.provenio.simkl.SimklImages
import io.github.dimitrysaf.provenio.simkl.SimklRepository
import io.github.dimitrysaf.provenio.simkl.SimklStatus
import io.github.dimitrysaf.provenio.simkl.SimklSync

/** The page margin everything on this screen lines up against. */
private val PageMargin = 16.dp

/**
 * The synced Simkl library, filtered three ways and sorted.
 *
 * Everything here is the local copy the sync already keeps up to date — nothing on this
 * screen calls Simkl. That is deliberate: [SimklSync] is built around Simkl's rate rules,
 * and a screen that fetched on open would be the polling those rules forbid.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    modifier: Modifier = Modifier,
    onOpenDetail: (type: String, id: String) -> Unit,
    onSettingsClick: () -> Unit,
) {
    val authState by SimklRepository.authState.collectAsState()
    val signedIn = authState is SimklAuthState.SignedIn
    val watching by SimklSync.watching.collectAsState()
    val planToWatch by SimklSync.planToWatch.collectAsState()
    val completed by SimklSync.completed.collectAsState()

    var list by rememberSaveable { mutableStateOf(SimklStatus.Watching) }
    var mediaType by rememberSaveable { mutableStateOf<String?>(null) }
    var sortName by rememberSaveable { mutableStateOf(LibrarySort.RecentlyWatched.name) }
    val sort = LibrarySort.entries.firstOrNull { it.name == sortName } ?: LibrarySort.RecentlyWatched

    val source = when (list) {
        SimklStatus.PlanToWatch -> planToWatch
        SimklStatus.Completed -> completed
        else -> watching
    }
    // Only the types actually present, so the picker never offers an empty result.
    val types = remember(source) { source.map { it.mediaType }.distinct().sorted() }
    val shown = remember(source, mediaType, sort) {
        source
            .filter { mediaType == null || it.mediaType == mediaType }
            .sortedWith(sort.comparator)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Text(
            text = "Simkl Watchlist",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(
                start = PageMargin,
                end = PageMargin,
                top = 16.dp,
                bottom = 12.dp,
            ),
        )

        if (!signedIn) {
            EmptyState(
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Outlined.CloudSync,
                title = "Your library lives on Simkl",
                description = "Sign in to bring what you are watching and planning to " +
                    "watch into Provenio.",
                actionLabel = "Sign in to Simkl",
                onAction = onSettingsClick,
            )
            return@Column
        }

        LibraryControls(
            list = list,
            onSelectList = { list = it },
            types = types,
            selectedType = mediaType,
            onSelectType = { mediaType = it },
            sort = sort,
            onSelectSort = { sortName = it.name },
            modifier = Modifier.padding(horizontal = PageMargin),
        )

        if (shown.isEmpty()) {
            EmptyState(
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Outlined.Bookmarks,
                title = "Nothing in ${listLabel(list)}",
                description = if (mediaType == null) {
                    "Anything you add to this list on Simkl shows up here."
                } else {
                    "No ${mediaTypeLabel(mediaType).lowercase()} in this list."
                },
            )
            return@Column
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 120.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = PageMargin,
                end = PageMargin,
                top = 12.dp,
                bottom = 24.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(shown, key = { it.simklId }) { item ->
                PosterCard(
                    title = item.title,
                    posterUrl = SimklImages.poster(item.poster),
                    onClick = {
                        item.imdbId?.let { onOpenDetail(item.stremioType(), it) }
                    },
                    subtitle = item.year?.toString(),
                    overlay = { if (item.isFinished()) WatchedBadge() },
                )
            }
        }
    }
}

/** Sits on the poster's own corner, so the tick reads against the art rather than beside it. */
@Composable
private fun BoxScope.WatchedBadge() {
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(6.dp)
            .size(24.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = "Watched",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(16.dp),
        )
    }
}

/** Every episode Simkl knows about has been watched. A film counts once it is completed. */
private fun SimklItem.isFinished(): Boolean =
    totalEpisodes > 0 && watchedEpisodes >= totalEpisodes

/** Simkl types its libraries as shows, movies and anime. The addon protocol does not. */
private fun SimklItem.stremioType(): String = if (mediaType == "movies") "movie" else "series"
