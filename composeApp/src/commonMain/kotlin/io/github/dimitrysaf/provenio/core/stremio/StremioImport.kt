package io.github.dimitrysaf.provenio.core.stremio

import co.touchlab.kermit.Logger
import io.github.dimitrysaf.provenio.core.addons.AddonRepository
import io.github.dimitrysaf.provenio.core.home.PosterShape
import io.github.dimitrysaf.provenio.core.library.LibraryClock
import io.github.dimitrysaf.provenio.core.library.LibraryItem
import io.github.dimitrysaf.provenio.core.library.LibraryRepository
import io.github.dimitrysaf.provenio.core.metadata.MetaDetailsRepository
import io.github.dimitrysaf.provenio.core.metadata.MetaVideo
import io.github.dimitrysaf.provenio.core.time.parseZonedIsoDateTimeToEpochMs
import io.github.dimitrysaf.provenio.core.watch.progress.WatchProgressEntry
import io.github.dimitrysaf.provenio.core.watch.progress.WatchProgressRepository
import io.github.dimitrysaf.provenio.core.watch.watched.WatchedItem
import io.github.dimitrysaf.provenio.core.watch.watched.WatchedRepository
import io.github.dimitrysaf.provenio.core.watch.watched.watchedItemKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.io.encoding.Base64

/** A Stremio account found signed in to the Stremio app on this device. */
data class StremioAccount(val authKey: String, val email: String)

enum class StremioImportStep {
    SigningIn,
    Addons,
    Library,
    Watched,
}

data class StremioImportSummary(
    val addons: Int,
    val library: Int,
    val continueWatching: Int,
    val watched: Int,
)

sealed interface StremioImportState {
    data object Idle : StremioImportState
    data class Running(val step: StremioImportStep) : StremioImportState
    data class Done(val summary: StremioImportSummary) : StremioImportState

    /** [signIn] is set when the account could not be signed in to, rather than read. */
    data class Failed(val signIn: Boolean) : StremioImportState
}

/**
 * Brings a Stremio account into the active profile: its add-ons, library, what is part-watched and
 * what has been watched. Nothing is sent back to Stremio, and what the profile already has stays.
 */
object StremioImport {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val log = Logger.withTag("StremioImport")
    private const val MaxSavedKeysTried = 8
    private const val MaxParallelMetaRequests = 4
    private const val AddonsReadyTimeoutMs = 20_000L

    private val _state = MutableStateFlow<StremioImportState>(StremioImportState.Idle)
    val state: StateFlow<StremioImportState> = _state.asStateFlow()

    private val _detected = MutableStateFlow<StremioAccount?>(null)

    /** The account the Stremio app on this device is signed in to, once one is found. */
    val detected: StateFlow<StremioAccount?> = _detected.asStateFlow()

    private var detection: Job? = null

    /** Looks once for a Stremio app signed in on this device; only desktops can see one. */
    fun detect() {
        if (detection != null) return
        detection = scope.launch {
            val keys = runCatching { StremioImportPlatform.savedAuthKeys() }
                .onFailure { log.w(it) { "Reading the Stremio app's sign-in failed" } }
                .getOrDefault(emptyList())
                .take(MaxSavedKeysTried)
            // Old sign-ins linger in the app's storage, so the first one Stremio still accepts is used.
            for (key in keys) {
                val user = runCatching { StremioApi.user(key) }.getOrNull() ?: continue
                _detected.value = StremioAccount(authKey = key, email = user.email)
                break
            }
        }
    }

    fun importDetected() {
        val account = _detected.value ?: return
        start { account.authKey }
    }

    fun signIn(email: String, password: String) {
        start { StremioApi.login(email.trim(), password).authKey }
    }

    private fun start(authKey: suspend () -> String) {
        if (_state.value is StremioImportState.Running) return
        _state.value = StremioImportState.Running(StremioImportStep.SigningIn)
        scope.launch {
            val key = try {
                authKey()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                log.w(error) { "Signing in to Stremio failed" }
                _state.value = StremioImportState.Failed(signIn = true)
                return@launch
            }
            _state.value = try {
                StremioImportState.Done(importAll(key))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                log.w(error) { "Importing from Stremio failed" }
                StremioImportState.Failed(signIn = false)
            }
        }
    }

    private suspend fun importAll(authKey: String): StremioImportSummary {
        _state.value = StremioImportState.Running(StremioImportStep.Addons)
        val addonUrls = StremioApi.addons(authKey).map(StremioAddonDescriptor::transportUrl).filter(::isImportableAddon)
        val addons = AddonRepository.importAddons(addonUrls)

        _state.value = StremioImportState.Running(StremioImportStep.Library)
        val items = StremioApi.library(authKey).filter { it.type.isNotBlank() && it.type != "other" }
        val library = items.filter { !it.removed && !it.temp }.map(StremioLibraryItem::toLibraryItem)
        LibraryRepository.applySyncedChanges(library, emptyList())
        // Stremio keeps what was only played, never added, as a temporary item; it still resumes.
        val progress = items
            .filter { (!it.removed || it.temp) && it.state.timeOffset > 0 }
            .mapNotNull(StremioLibraryItem::toProgressEntry)
        WatchProgressRepository.applySyncedChanges(progress, emptyList())

        _state.value = StremioImportState.Running(StremioImportStep.Watched)
        val watched = watchedItems(items)
        WatchedRepository.applySyncedChanges(
            watched.associateBy { watchedItemKey(it.type, it.id, it.season, it.episode) },
            emptyList(),
        )
        return StremioImportSummary(
            addons = addons,
            library = library.size,
            continueWatching = progress.size,
            watched = watched.size,
        )
    }

    private suspend fun watchedItems(items: List<StremioLibraryItem>): List<WatchedItem> {
        val titles = items
            .filter { it.type != SeriesType && (it.state.timesWatched > 0 || it.state.flaggedWatched > 0) }
            .map { item -> item.toWatchedItem(season = null, episode = null, videoId = null) }
        val series = items.filter { it.type == SeriesType && !it.state.watched.isNullOrBlank() }
        if (series.isEmpty()) return titles

        // Stremio keeps a series' watched episodes by their place in its episode list, so reading
        // them needs that list, which the add-ons just brought over provide once they are loaded.
        withTimeoutOrNull(AddonsReadyTimeoutMs) { AddonRepository.awaitManifestsLoaded() }
        val requests = Semaphore(MaxParallelMetaRequests)
        val episodes = coroutineScope {
            series.map { item ->
                async {
                    requests.withPermit {
                        val videos = runCatching { MetaDetailsRepository.fetch(item.type, item.id, cacheResult = false) }
                            .getOrNull()
                            ?.videos
                            .orEmpty()
                        watchedVideos(item.state.watched.orEmpty(), videos).mapNotNull { video ->
                            val season = video.season ?: return@mapNotNull null
                            val episode = video.episode ?: return@mapNotNull null
                            item.toWatchedItem(season = season, episode = episode, videoId = video.id)
                        }
                    }
                }
            }.awaitAll().flatten()
        }
        return titles + episodes
    }

    private fun isImportableAddon(url: String): Boolean {
        val lower = url.trim().lowercase()
        // The add-on for files on the Stremio computer only answers on that computer.
        return (lower.startsWith("https://") || lower.startsWith("http://")) &&
            "://127.0.0.1" !in lower && "://localhost" !in lower
    }
}

private const val SeriesType = "series"

/**
 * The episodes a Stremio watched field marks: `<anchor video>:<list length>:<zlib bitfield>`, one
 * bit per episode in season, episode and release order, lowest bit first. The anchor is the last
 * episode watched when it was saved, so episodes added to the list since then shift into place.
 */
internal fun watchedVideos(watched: String, videos: List<MetaVideo>): List<MetaVideo> {
    val parts = watched.split(':')
    if (parts.size < 3 || videos.isEmpty()) return emptyList()
    val anchorVideo = parts.dropLast(2).joinToString(":")
    val anchorLength = parts[parts.size - 2].toIntOrNull() ?: return emptyList()
    val packed = runCatching { Base64.decode(parts.last()) }.getOrNull() ?: return emptyList()
    val bits = StremioImportPlatform.inflate(packed) ?: return emptyList()
    val ordered = videos.sortedWith(
        compareBy<MetaVideo>(
            { it.seriesOrder { season, _ -> season } },
            { it.seriesOrder { _, episode -> episode } },
            { it.released?.let(::parseZonedIsoDateTimeToEpochMs) ?: Long.MIN_VALUE },
        ),
    )
    val anchorIndex = ordered.indexOfFirst { it.id == anchorVideo }
    if (anchorIndex < 0) return emptyList()
    val offset = anchorLength - anchorIndex - 1
    return ordered.filterIndexed { index, _ ->
        val saved = index + offset
        saved >= 0 && saved / 8 < bits.size && ((bits[saved / 8].toInt() shr (saved % 8)) and 1) == 1
    }
}

// An episode without both a season and a number sorts before every numbered one, as in Stremio.
private inline fun MetaVideo.seriesOrder(pick: (Int, Int) -> Int): Long {
    val number = episode ?: return Long.MIN_VALUE
    return pick(season ?: return Long.MIN_VALUE, number).toLong()
}

private fun String?.toEpochMs(): Long? = this?.takeIf(String::isNotBlank)?.let(::parseZonedIsoDateTimeToEpochMs)

private fun StremioLibraryItem.lastActivityEpochMs(): Long =
    state.lastWatched.toEpochMs() ?: modifiedAt.toEpochMs() ?: LibraryClock.nowEpochMs()

private fun StremioLibraryItem.toLibraryItem(): LibraryItem =
    LibraryItem(
        id = id,
        type = type,
        name = name.ifBlank { id },
        poster = poster?.takeIf(String::isNotBlank),
        posterShape = when (posterShape?.lowercase()) {
            "landscape" -> PosterShape.Landscape
            "square" -> PosterShape.Square
            else -> PosterShape.Poster
        },
        imdbId = id.takeIf { it.startsWith("tt") },
        savedAtEpochMs = createdAt.toEpochMs() ?: modifiedAt.toEpochMs() ?: LibraryClock.nowEpochMs(),
    )

private fun StremioLibraryItem.toProgressEntry(): WatchProgressEntry? {
    val videoId = state.videoId?.takeIf(String::isNotBlank) ?: id
    // A series resumes an episode, whose number is at the end of its id: `<series>:<season>:<episode>`.
    val numbers = videoId.removePrefix(id).trimStart(':').split(':').mapNotNull(String::toIntOrNull)
    val season = numbers.takeIf { it.size == 2 }?.first()
    val episode = numbers.takeIf { it.size == 2 }?.last()
    if (type == SeriesType && (season == null || episode == null)) return null
    val position = state.timeOffset.toLong()
    val duration = state.duration.toLong()
    return WatchProgressEntry(
        contentType = type,
        parentMetaId = id,
        parentMetaType = type,
        videoId = videoId,
        title = name.ifBlank { id },
        poster = poster?.takeIf(String::isNotBlank),
        seasonNumber = season,
        episodeNumber = episode,
        lastPositionMs = position,
        durationMs = duration,
        lastUpdatedEpochMs = lastActivityEpochMs(),
        progressPercent = if (duration > 0) (position * 100f / duration).coerceIn(0f, 100f) else null,
    )
}

private fun StremioLibraryItem.toWatchedItem(season: Int?, episode: Int?, videoId: String?): WatchedItem =
    WatchedItem(
        id = id,
        type = type,
        name = name.ifBlank { id },
        poster = poster?.takeIf(String::isNotBlank),
        season = season,
        episode = episode,
        videoId = videoId,
        markedAtEpochMs = lastActivityEpochMs(),
    )
