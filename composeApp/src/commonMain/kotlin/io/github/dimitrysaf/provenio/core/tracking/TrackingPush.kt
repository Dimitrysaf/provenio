package io.github.dimitrysaf.provenio.core.tracking

import co.touchlab.kermit.Logger
import io.github.dimitrysaf.provenio.core.library.LibraryItem
import io.github.dimitrysaf.provenio.core.library.LibraryRepository
import io.github.dimitrysaf.provenio.core.watch.watched.WatchedItem
import io.github.dimitrysaf.provenio.core.watch.watched.WatchedRepository
import io.github.dimitrysaf.provenio.core.watch.watched.watchedItemKeys
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

/** What [TrackingPush.push] sent, and how much of it could not be. */
data class TrackingPushResult(
    val watchedSent: Int = 0,
    val savedSent: Int = 0,
    val failed: Int = 0,
) {
    val sentNothing: Boolean
        get() = watchedSent == 0 && savedSent == 0
}

/**
 * Sends what is kept on this device to a tracking service: the watched history and the library.
 * Only what the service does not have yet is sent, as a service counts every watch sent to it as
 * one more play. Nothing on the service is changed or removed.
 */
object TrackingPush {
    private val log = Logger.withTag("TrackingPush")

    private const val HistoryBatchSize = 100

    /** A service takes about one change a second; a library is sent one title at a time. */
    private val LibraryWriteGap = 1.seconds

    // A push goes on when the page it was started from is left.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _pushing = MutableStateFlow<Set<TrackingProviderId>>(emptySet())

    /** The services being pushed to now. */
    val pushing: StateFlow<Set<TrackingProviderId>> = _pushing.asStateFlow()

    /** Pushes to [providerId], unless that is under way already, and says how it went to [onFinished]. */
    fun start(
        profileId: Int,
        providerId: TrackingProviderId,
        onFinished: suspend (TrackingPushResult) -> Unit,
    ) {
        if (providerId in _pushing.value) return
        _pushing.update { it + providerId }
        scope.launch {
            val result = try {
                push(profileId, providerId)
            } finally {
                _pushing.update { it - providerId }
            }
            onFinished(result)
        }
    }

    suspend fun push(profileId: Int, providerId: TrackingProviderId): TrackingPushResult {
        val history = pushHistory(profileId, providerId)
        val library = pushLibrary(profileId, providerId)
        if (history.watchedSent > 0 || library.savedSent > 0) refreshAfterPush(profileId, providerId)
        return TrackingPushResult(
            watchedSent = history.watchedSent,
            savedSent = library.savedSent,
            failed = history.failed + library.failed,
        )
    }

    private suspend fun pushHistory(profileId: Int, providerId: TrackingProviderId): TrackingPushResult {
        val provider = TrackingProviderRegistry.connectedWatchedProviders()
            .firstOrNull { it.providerId == providerId }
            ?: return TrackingPushResult()
        val local = WatchedRepository.localItemsForSync().values
        if (local.isEmpty()) return TrackingPushResult()

        // What the service has is read first, so a watch is not sent to it twice.
        val remote = try {
            provider.pull(profileId = profileId, pageSize = HistoryBatchSize)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            log.e(error) { "Failed to read ${providerId.storageId} history before pushing" }
            return TrackingPushResult(failed = local.size)
        }
        val remoteKeys = remote.flatMapTo(hashSetOf()) { it.keys() } +
            runCatching { provider.pullExtraWatchedKeys(profileId) }.getOrDefault(emptySet())
        val remoteTitles = remote.mapNotNullTo(hashSetOf()) { it.titleKey() }
        val missing = local.filter { item ->
            item.keys().none(remoteKeys::contains) && item.titleKey()?.let(remoteTitles::contains) != true
        }

        var sent = 0
        var failed = 0
        missing.chunked(HistoryBatchSize).forEach { batch ->
            try {
                provider.push(profileId = profileId, items = batch)
                sent += batch.size
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                log.e(error) { "Failed to push watched history to ${providerId.storageId}" }
                failed += batch.size
            }
        }
        return TrackingPushResult(watchedSent = sent, failed = failed)
    }

    private suspend fun pushLibrary(profileId: Int, providerId: TrackingProviderId): TrackingPushResult {
        val provider = TrackingProviderRegistry.connectedLibraryProviders()
            .firstOrNull { it.providerId == providerId }
            ?: return TrackingPushResult()
        val local = LibraryRepository.localItemsForSync()
        if (local.isEmpty()) return TrackingPushResult()

        try {
            provider.ensureLoaded()
            provider.refresh(TrackingRefreshIntent.USER_INITIATED)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            log.e(error) { "Failed to read ${providerId.storageId} library before pushing" }
            return TrackingPushResult(failed = local.size)
        }

        var sent = 0
        var failed = 0
        local.forEach { item ->
            try {
                if (!sendSaved(profileId, provider, item)) return@forEach
                sent++
                delay(LibraryWriteGap)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                log.e(error) { "Failed to push ${item.id} to the ${providerId.storageId} library" }
                failed++
            }
        }
        return TrackingPushResult(savedSent = sent, failed = failed)
    }

    // What was sent is read back, so the app shows it as the service now has it.
    private suspend fun refreshAfterPush(profileId: Int, providerId: TrackingProviderId) {
        try {
            TrackingProviderRegistry.connectedLibraryProviders()
                .firstOrNull { it.providerId == providerId }
                ?.refresh(TrackingRefreshIntent.USER_INITIATED)
            WatchedRepository.pullFromServer(profileId)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            log.w(error) { "Failed to read ${providerId.storageId} back after pushing" }
        }
    }

    /** Adds [item] to the service's default list, unless it is on one of its lists already. */
    private suspend fun sendSaved(
        profileId: Int,
        provider: TrackingLibraryProvider,
        item: LibraryItem,
    ): Boolean {
        if (provider.contains(item.id, item.type)) return false
        val membership = provider.membership(item)
        if (membership.values.any { it }) return false
        val desired = provider.toggledDefaultMembership(membership)
        if (desired.values.none { it }) return false
        provider.applyMembership(profileId = profileId, item = item, desiredMembership = desired)
        return true
    }

    private fun WatchedItem.keys(): Set<String> = watchedItemKeys(type, id, season, episode)

    // A service can know a title by another of its ids, so a watch is also matched by its name.
    private fun WatchedItem.titleKey(): String? =
        listOf(
            name.trim().lowercase().takeIf(String::isNotEmpty) ?: return null,
            if (type.trim().lowercase() in MovieTypes) "movie" else "series",
            season?.toString().orEmpty(),
            episode?.toString().orEmpty(),
        ).joinToString("|")

    private val MovieTypes = setOf("movie", "film")
}
