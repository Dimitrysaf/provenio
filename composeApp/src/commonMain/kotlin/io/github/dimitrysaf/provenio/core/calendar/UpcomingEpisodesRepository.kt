package io.github.dimitrysaf.provenio.core.calendar

import co.touchlab.kermit.Logger
import io.github.dimitrysaf.provenio.core.addons.AddonRepository
import io.github.dimitrysaf.provenio.core.library.LibraryItem
import io.github.dimitrysaf.provenio.core.library.LibraryRepository
import io.github.dimitrysaf.provenio.core.metadata.MetaDetailsRepository
import io.github.dimitrysaf.provenio.core.notifications.isSeriesLibraryType
import io.github.dimitrysaf.provenio.core.time.daysUntilEpisodeRelease
import io.github.dimitrysaf.provenio.core.time.parseEpisodeReleaseLocalDate
import io.github.dimitrysaf.provenio.core.watch.progress.CurrentDateProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull

data class UpcomingEpisode(
    val showId: String,
    val showType: String,
    val showName: String,
    val poster: String?,
    val episodeId: String,
    val episodeTitle: String?,
    val seasonNumber: Int?,
    val episodeNumber: Int?,
    val releaseDateIso: String,
)

data class UpcomingEpisodesUiState(
    val isLoading: Boolean = false,
    val isLoaded: Boolean = false,
    val episodes: List<UpcomingEpisode> = emptyList(),
)

// Episodes airing soon for the shows in the Library, whichever source it currently shows (local, Trakt or Simkl).
object UpcomingEpisodesRepository {
    private const val MetadataFetchConcurrency = 4
    private const val WindowDays = 60

    private val log = Logger.withTag("UpcomingEpisodes")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _uiState = MutableStateFlow(UpcomingEpisodesUiState())
    val uiState: StateFlow<UpcomingEpisodesUiState> = _uiState.asStateFlow()
    private var loadedForShows: Set<String>? = null
    private var refreshJob: Job? = null

    // Refreshes only when the Library's shows changed since the last load, unless forced.
    fun refresh(force: Boolean = false) {
        LibraryRepository.ensureLoaded()
        val libraryState = LibraryRepository.uiState.value
        if (!libraryState.isLoaded) return
        val shows = libraryState.items.filter { isSeriesLibraryType(it.type) }.distinctBy { "${it.type}:${it.id}" }
        val showKeys = shows.map { "${it.type}:${it.id}" }.toSet()
        if (!force && showKeys == loadedForShows) return
        loadedForShows = showKeys
        refreshJob?.cancel()
        refreshJob = scope.launch {
            _uiState.update { it.copy(isLoading = true) }
            AddonRepository.initialize()
            withTimeoutOrNull(10_000L) { AddonRepository.awaitManifestsLoaded() }
            val today = CurrentDateProvider.todayIsoDate()
            val semaphore = Semaphore(MetadataFetchConcurrency)
            val episodes = shows.map { show ->
                async { semaphore.withPermit { upcomingFor(show, today) } }
            }.awaitAll().flatten().sortedWith(
                compareBy<UpcomingEpisode>({ it.releaseDateIso }, { it.showName }, { it.seasonNumber ?: 0 }, { it.episodeNumber ?: 0 }),
            )
            _uiState.value = UpcomingEpisodesUiState(isLoading = false, isLoaded = true, episodes = episodes)
        }
    }

    private suspend fun upcomingFor(show: LibraryItem, today: String): List<UpcomingEpisode> {
        val meta = runCatching { MetaDetailsRepository.fetch(type = show.type, id = show.id) }
            .onFailure { log.w { "Failed to load ${show.type}:${show.id}: ${it.message}" } }
            .getOrNull() ?: return emptyList()
        return meta.videos.mapNotNull { video ->
            if (video.season == null && video.episode == null) return@mapNotNull null
            val days = daysUntilEpisodeRelease(today, video.released) ?: return@mapNotNull null
            if (days !in 0..WindowDays) return@mapNotNull null
            UpcomingEpisode(
                showId = show.id,
                showType = show.type,
                showName = meta.name.ifBlank { show.name },
                poster = meta.poster ?: show.poster,
                episodeId = video.id,
                episodeTitle = video.title.takeIf { it.isNotBlank() },
                seasonNumber = video.season,
                episodeNumber = video.episode,
                releaseDateIso = parseEpisodeReleaseLocalDate(video.released) ?: return@mapNotNull null,
            )
        }
    }
}
