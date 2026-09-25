package io.github.dimitrysaf.provenio.core.watch.watched

import co.touchlab.kermit.Logger
import io.github.dimitrysaf.provenio.core.tracking.ensureTrackingProvidersRegistered
import io.github.dimitrysaf.provenio.core.metadata.MetaDetails
import io.github.dimitrysaf.provenio.core.metadata.MetaVideo
import io.github.dimitrysaf.provenio.core.profiles.ProfileRepository
import io.github.dimitrysaf.provenio.core.tracking.TrackingProviderId
import io.github.dimitrysaf.provenio.core.tracking.TrackingProviderRegistry
import io.github.dimitrysaf.provenio.core.tracking.TrackingSettingsRepository
import io.github.dimitrysaf.provenio.core.tracking.WatchProgressSource
import io.github.dimitrysaf.provenio.core.tracking.effectiveWatchProgressSource
import io.github.dimitrysaf.provenio.core.tracking.providerId
import io.github.dimitrysaf.provenio.core.watch.watching.sync.WatchedDeltaEvent
import io.github.dimitrysaf.provenio.core.watch.watching.sync.WatchedSyncAdapter
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal enum class WatchedTrackerHistorySync {
    Mirror,
    Skip,
}

internal data class WatchedPushOutcome(
    val accountSyncSucceeded: Boolean = false,
    val succeededTrackerProviderIds: Set<TrackingProviderId> = emptySet(),
)

internal fun shouldMirrorWatchedMarkToTrackers(
    sync: WatchedTrackerHistorySync,
    hasConnectedTracker: Boolean,
): Boolean = sync == WatchedTrackerHistorySync.Mirror && hasConnectedTracker

internal data class WatchedSourceOperation(
    val source: WatchProgressSource,
    val generation: Long,
)

internal fun isWatchedSourceOperationCurrent(
    operation: WatchedSourceOperation,
    activeSource: WatchProgressSource,
    activeGeneration: Long,
): Boolean = operation.source == activeSource && operation.generation == activeGeneration

internal fun watchedItemsForSource(
    source: WatchProgressSource,
    accountItems: Collection<WatchedItem>,
    providerItems: Map<TrackingProviderId, Collection<WatchedItem>>,
): Collection<WatchedItem> = source.providerId
    ?.let { providerId -> providerItems[providerId].orEmpty() }
    ?: accountItems

internal fun shouldAcknowledgeAccountWatchedPush(
    source: WatchProgressSource,
    outcome: WatchedPushOutcome,
): Boolean = source.providerId == null && outcome.accountSyncSucceeded

internal fun replaceWatchedItemsForSource(
    source: WatchProgressSource,
    accountItems: MutableMap<String, WatchedItem>,
    providerItems: MutableMap<TrackingProviderId, MutableMap<String, WatchedItem>>,
    replacement: Map<String, WatchedItem>,
) {
    val target = source.providerId
        ?.let { providerId -> providerItems.getOrPut(providerId, ::mutableMapOf) }
        ?: accountItems
    target.clear()
    target.putAll(replacement)
}

internal suspend fun <T> watchedProviderRefreshOrNull(
    refresh: suspend () -> T,
    onFailure: (Throwable) -> Unit,
): T? = try {
    refresh()
} catch (error: CancellationException) {
    throw error
} catch (error: Throwable) {
    onFailure(error)
    null
}

internal fun extraWatchedKeysChanged(
    previous: Set<String>?,
    current: Set<String>,
): Boolean = previous.orEmpty() != current

private const val maxRestorableWatchedPayloadChars = 4 * 1024 * 1024

internal fun shouldRestoreWatchedPayload(payloadLength: Int): Boolean =
    payloadLength <= maxRestorableWatchedPayloadChars

object WatchedRepository {
    private data class WatchedRefreshOperation(
        val profileId: Int,
        val profileGeneration: Long,
        val sourceOperation: WatchedSourceOperation,
    )

    private const val watchedItemsPageSize = 900
    private const val watchedItemsDeltaPageSize = 900
    private const val watchedDeltaOperationUpsert = "upsert"
    private const val watchedDeltaOperationDelete = "delete"
    private const val watchedDiagnosticSampleLimit = 10

    private val accountScopeLock = SynchronizedObject()
    private var accountScopeJob: Job = SupervisorJob()
    private var accountScope = CoroutineScope(accountScopeJob + Dispatchers.Default)
    private val log = Logger.withTag("WatchedRepository")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    private val _uiState = MutableStateFlow(WatchedUiState())
    val uiState: StateFlow<WatchedUiState> = _uiState.asStateFlow()
    private val _fullyWatchedSeriesKeys = MutableStateFlow<Set<String>>(emptySet())
    val fullyWatchedSeriesKeys: StateFlow<Set<String>> = _fullyWatchedSeriesKeys.asStateFlow()

    private var hasLoaded = false
    private var currentProfileId: Int = 1
    private var profileGeneration: Long = 0L
    private var activeSource: WatchProgressSource = WatchProgressSource.ACCOUNT_SYNC
    private var sourceGeneration: Long = 0L
    private val itemsStore = WatchedItemsStore()
    private var accountFullyWatchedSeriesKeys: Set<String> = emptySet()
    private var providerFullyWatchedSeriesKeys: MutableMap<TrackingProviderId, Set<String>> = mutableMapOf()
    private var expandedSiblingKeys: Set<String> = emptySet()
    private var providerExtraWatchedKeys: MutableMap<TrackingProviderId, Set<String>> = mutableMapOf()
    private var accountHasLoaded: Boolean = false
    private var loadedProviders: MutableSet<TrackingProviderId> = mutableSetOf()
    private var accountHasLoadedRemote: Boolean = false
    private var providersLoadedFromRemote: MutableSet<TrackingProviderId> = mutableSetOf()
    private var lastSuccessfulPushEpochMs: Long = 0L
    private var deltaCursorEventId: Long = 0L
    private var deltaInitialized: Boolean = false
    private var extraKeysObserverJob: Job? = null

    fun ensureLoaded() {
        ensureTrackingProvidersRegistered()
        TrackingProviderRegistry.ensureLoaded()
        TrackingSettingsRepository.ensureLoaded()
        if (!hasLoaded) {
            loadFromDisk(ProfileRepository.activeProfileId)
            activateEffectiveSource(
                effectiveWatchedSource(
                    requestedSource = TrackingSettingsRepository.uiState.value.watchProgressSource,
                    connectedProviderIds = connectedWatchedProviderIds(),
                ),
            )
        }
        startExtraKeysObserverIfNeeded()
    }

    fun onProfileChanged(profileId: Int) {
        if (profileId == currentProfileId && hasLoaded) return
        loadFromDisk(profileId)
    }

    fun clearLocalState() {
        val previousAccountJob = synchronized(accountScopeLock) {
            accountScopeJob.also {
                accountScopeJob = SupervisorJob()
                accountScope = CoroutineScope(accountScopeJob + Dispatchers.Default)
            }
        }
        previousAccountJob.cancel()
        extraKeysObserverJob = null
        hasLoaded = false
        currentProfileId = 1
        profileGeneration += 1L
        activeSource = WatchProgressSource.ACCOUNT_SYNC
        sourceGeneration += 1L
        itemsStore.update { accountItems, providerItems, dirtyAccountKeys, dirtyProviderKeys ->
            accountItems.clear()
            providerItems.clear()
            dirtyAccountKeys.clear()
            dirtyProviderKeys.clear()
        }
        accountFullyWatchedSeriesKeys = emptySet()
        providerFullyWatchedSeriesKeys.clear()
        expandedSiblingKeys = emptySet()
        providerExtraWatchedKeys.clear()
        accountHasLoaded = false
        loadedProviders.clear()
        accountHasLoadedRemote = false
        providersLoadedFromRemote.clear()
        lastSuccessfulPushEpochMs = 0L
        deltaCursorEventId = 0L
        deltaInitialized = false
        _fullyWatchedSeriesKeys.value = emptySet()
        _uiState.value = WatchedUiState()
    }

    private fun loadFromDisk(profileId: Int) {
        currentProfileId = profileId
        profileGeneration += 1L
        activeSource = WatchProgressSource.ACCOUNT_SYNC
        sourceGeneration += 1L
        hasLoaded = true
        itemsStore.update { accountItems, providerItems, dirtyAccountKeys, dirtyProviderKeys ->
            accountItems.clear()
            providerItems.clear()
            dirtyAccountKeys.clear()
            dirtyProviderKeys.clear()
        }
        accountFullyWatchedSeriesKeys = emptySet()
        providerFullyWatchedSeriesKeys.clear()
        expandedSiblingKeys = emptySet()
        providerExtraWatchedKeys.clear()
        accountHasLoaded = true
        loadedProviders.clear()
        accountHasLoadedRemote = false
        providersLoadedFromRemote.clear()

        val payload = WatchedStorage.loadPayload(profileId).orEmpty().trim()
        if (payload.isNotEmpty()) {
            val storedPayload = if (shouldRestoreWatchedPayload(payload.length)) {
                runCatching {
                    json.decodeFromString<StoredWatchedPayload>(payload)
                }.getOrDefault(StoredWatchedPayload())
            } else {
                WatchedStorage.savePayload(profileId, "")
                StoredWatchedPayload()
            }
            lastSuccessfulPushEpochMs = storedPayload.lastSuccessfulPushEpochMs
            deltaCursorEventId = storedPayload.deltaCursorEventId
            deltaInitialized = storedPayload.deltaInitialized
            val restoredItems = storedPayload.items
                .map(WatchedItem::normalizedMarkedAt)
                .associateBy { watchedItemKey(it.type, it.id, it.season, it.episode) }
            val restoredProviderPayloads = storedPayload.providerPayloads.mapNotNull { (storageId, providerPayload) ->
                TrackingProviderId.fromStorage(storageId)?.let { providerId -> providerId to providerPayload }
            }.toMap()
            itemsStore.update { accountItems, providerItems, dirtyAccountKeys, dirtyProviderKeys ->
                accountItems.putAll(restoredItems)
                dirtyAccountKeys += storedPayload.dirtyWatchedKeys.filter { key -> key in restoredItems }
                restoredProviderPayloads.forEach { (providerId, providerPayload) ->
                    val providerItemsByKey = (providerPayload.items + expandProviderWatchedItems(providerPayload.itemGroups))
                        .map(WatchedItem::normalizedMarkedAt)
                        .associateBy { watchedItemKey(it.type, it.id, it.season, it.episode) }
                    providerItems[providerId] = providerItemsByKey.toMutableMap()
                    dirtyProviderKeys[providerId] = providerPayload.dirtyWatchedKeys
                        .filterTo(mutableSetOf()) { key -> key in providerItemsByKey }
                }
            }
            accountFullyWatchedSeriesKeys = storedPayload.fullyWatchedSeriesKeys
            expandedSiblingKeys = storedPayload.expandedSiblingKeys
            restoredProviderPayloads.forEach { (providerId, providerPayload) ->
                providerFullyWatchedSeriesKeys[providerId] = providerPayload.fullyWatchedSeriesKeys
                providerExtraWatchedKeys[providerId] =
                    providerPayload.extraWatchedKeys + expandExtraWatchedKeys(providerPayload.extraWatchedKeyGroups)
            }
            loadedProviders += restoredProviderPayloads.keys
        } else {
            lastSuccessfulPushEpochMs = 0L
            deltaCursorEventId = 0L
            deltaInitialized = false
            accountFullyWatchedSeriesKeys = emptySet()
        }

        publish()
    }

    internal fun activateSource(source: WatchProgressSource): WatchProgressSource {
        if (!hasLoaded) {
            loadFromDisk(ProfileRepository.activeProfileId)
        }
        return activateEffectiveSource(source)
    }

    private fun activateEffectiveSource(source: WatchProgressSource): WatchProgressSource {
        if (activeSource == source) {
            log.i {
                "Watched source activation unchanged source=$source generation=$sourceGeneration " +
                    "items=${itemCountForSource(source)} loaded=${hasLoadedSource(source)}"
            }
            return source
        }
        val previousSource = activeSource
        activeSource = source
        sourceGeneration += 1L
        stopExtraKeysObserver()
        log.i {
            "Watched source activated previous=$previousSource current=$source generation=$sourceGeneration " +
                "provider=${source.providerId?.storageId}"
        }
        publish()
        startExtraKeysObserverIfNeeded()
        return source
    }

    private fun newRefreshOperation(profileId: Int): WatchedRefreshOperation? {
        if (ProfileRepository.activeProfileId != profileId) return null
        if (!hasLoaded || currentProfileId != profileId) return null
        return WatchedRefreshOperation(
            profileId = profileId,
            profileGeneration = profileGeneration,
            sourceOperation = WatchedSourceOperation(
                source = activeSource,
                generation = sourceGeneration,
            ),
        )
    }

    private fun isActiveOperation(operation: WatchedRefreshOperation): Boolean =
        currentProfileId == operation.profileId &&
            profileGeneration == operation.profileGeneration &&
            ProfileRepository.activeProfileId == operation.profileId &&
            isWatchedSourceOperationCurrent(
                operation = operation.sourceOperation,
                activeSource = activeSource,
                activeGeneration = sourceGeneration,
            )

    suspend fun pullFromServer(profileId: Int) {
        TrackingProviderRegistry.ensureLoaded()
        TrackingSettingsRepository.ensureLoaded()
        refreshForSource(
            profileId = profileId,
            source = effectiveWatchedSource(
                requestedSource = TrackingSettingsRepository.uiState.value.watchProgressSource,
                connectedProviderIds = connectedWatchedProviderIds(),
            ),
            forceSnapshot = false,
        )
    }

    suspend fun forceSnapshotRefreshFromServer(profileId: Int) {
        TrackingProviderRegistry.ensureLoaded()
        TrackingSettingsRepository.ensureLoaded()
        refreshForSource(
            profileId = profileId,
            source = effectiveWatchedSource(
                requestedSource = TrackingSettingsRepository.uiState.value.watchProgressSource,
                connectedProviderIds = connectedWatchedProviderIds(),
            ),
            forceSnapshot = true,
        )
    }

    internal suspend fun refreshForSource(
        profileId: Int,
        source: WatchProgressSource,
        forceSnapshot: Boolean = true,
    ): Boolean {
        TrackingProviderRegistry.ensureLoaded()
        TrackingSettingsRepository.ensureLoaded()
        if (ProfileRepository.activeProfileId != profileId) {
            log.d { "Skipping watched refresh for inactive profile $profileId" }
            return false
        }
        if (!hasLoaded || currentProfileId != profileId) {
            loadFromDisk(profileId)
        }

        val effectiveSource = activateEffectiveSource(source)
        val operation = newRefreshOperation(profileId) ?: return false
        log.i {
            "Watched refresh request profile=$profileId requestedSource=$source effectiveSource=$effectiveSource " +
                "forceSnapshot=$forceSnapshot profileGeneration=$profileGeneration " +
                "sourceGeneration=$sourceGeneration"
        }
        val providerId = effectiveSource.providerId ?: run {
            // Watched history on this device is the only copy when no tracker is the source.
            accountHasLoaded = true
            accountHasLoadedRemote = true
            publish()
            return true
        }
        return try {
            val provider = TrackingProviderRegistry.watchedProvider(providerId)
                ?: run {
                    log.w { "Watched provider missing provider=${providerId.storageId} source=$effectiveSource" }
                    return false
                }
            pullSnapshotFromAdapter(
                adapter = provider,
                operation = operation,
                profileId = profileId,
                resetDeltaState = true,
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            log.e(error) { "Failed to refresh watched items from $effectiveSource" }
            false
        }
    }

    private suspend fun pullSnapshotFromAdapter(
        adapter: WatchedSyncAdapter,
        operation: WatchedRefreshOperation,
        profileId: Int,
        resetDeltaState: Boolean,
    ): Boolean {
        val serverItems = adapter.pull(
            profileId = profileId,
            pageSize = watchedItemsPageSize,
        )
        val fullyWatchedSeriesKeys = adapter.pullFullyWatchedSeriesKeys(profileId)
        val extraWatchedKeys = adapter.pullExtraWatchedKeys(profileId)
        val source = operation.sourceOperation.source
        log.i {
            "Watched adapter result source=$source provider=${source.providerId?.storageId ?: "provenio"} " +
                "profile=$profileId serverItems=${serverItems.size} " +
                "movies=${serverItems.count { it.type == "movie" }} " +
                "episodes=${serverItems.count { it.season != null && it.episode != null }} " +
                "seriesSummaries=${serverItems.count { it.type != "movie" && it.season == null }} " +
                "fullyWatchedSeries=${fullyWatchedSeriesKeys?.size} " +
                "itemKeys=[${diagnosticItemKeySample(serverItems)}] " +
                "fullyWatchedKeys=[${fullyWatchedSeriesKeys.orEmpty().take(watchedDiagnosticSampleLimit).joinToString(",")}]"
        }
        if (!isActiveOperation(operation)) {
            log.w {
                "Watched adapter result discarded source=$source profile=$profileId " +
                    "operationProfileGeneration=${operation.profileGeneration} currentProfileGeneration=$profileGeneration " +
                    "operationSourceGeneration=${operation.sourceOperation.generation} currentSourceGeneration=$sourceGeneration " +
                    "activeSource=$activeSource"
            }
            return false
        }
        itemsStore.update { accountItems, providerItems, dirtyAccountKeys, dirtyProviderKeys ->
            val items = source.providerId
                ?.let { providerId -> providerItems[providerId]?.values.orEmpty() }
                ?: accountItems.values
            val dirtyKeys = source.providerId
                ?.let { providerId -> dirtyProviderKeys.getOrPut(providerId, ::mutableSetOf) }
                ?: dirtyAccountKeys
            val merged = mergeWatchedSnapshot(
                serverItems = serverItems,
                localItems = items.toList(),
                dirtyKeys = dirtyKeys,
                acknowledgeDirtyByPresence = source.providerId != null,
            )
            replaceWatchedItemsForSource(
                source = source,
                accountItems = accountItems,
                providerItems = providerItems,
                replacement = merged.items,
            )
            dirtyKeys.clear()
            dirtyKeys += merged.dirtyKeys
        }
        fullyWatchedSeriesKeys?.let { keys ->
            setFullyWatchedSeriesKeysForSource(source, keys)
        }
        source.providerId?.let { providerId ->
            providerExtraWatchedKeys[providerId] = extraWatchedKeys
            loadedProviders += providerId
            providersLoadedFromRemote += providerId
        } ?: run {
            accountHasLoaded = true
            accountHasLoadedRemote = true
            if (resetDeltaState) {
                deltaCursorEventId = 0L
                deltaInitialized = false
            }
        }
        publish()
        persist()
        return true
    }

    private fun itemsForSourceSnapshot(source: WatchProgressSource): List<WatchedItem> =
        itemsStore.read { accountItems, providerItems, _, _ ->
            val items = source.providerId
                ?.let { providerId -> providerItems[providerId]?.values.orEmpty() }
                ?: accountItems.values
            items.toList()
        }

    private fun fullyWatchedSeriesKeysForSource(source: WatchProgressSource): Set<String> =
        source.providerId
            ?.let { providerId -> providerFullyWatchedSeriesKeys[providerId].orEmpty() }
            ?: accountFullyWatchedSeriesKeys

    private fun setFullyWatchedSeriesKeysForSource(
        source: WatchProgressSource,
        keys: Set<String>,
    ) {
        source.providerId?.let { providerId ->
            providerFullyWatchedSeriesKeys[providerId] = keys
        } ?: run {
            accountFullyWatchedSeriesKeys = keys
        }
    }

    private fun hasLoadedSource(source: WatchProgressSource): Boolean =
        source.providerId?.let(loadedProviders::contains) ?: accountHasLoaded

    private fun itemCountForSource(source: WatchProgressSource): Int =
        itemsStore.read { accountItems, providerItems, _, _ ->
            source.providerId
                ?.let { providerId -> providerItems[providerId]?.size ?: 0 }
                ?: accountItems.size
        }

    fun toggleWatched(item: WatchedItem) {
        ensureLoaded()
        val isMarked = isWatched(
            id = item.id,
            type = item.type,
            season = item.season,
            episode = item.episode,
        )
        if (isMarked) {
            unmarkWatched(item)
        } else {
            markWatched(item)
        }
    }

    fun markWatched(item: WatchedItem) {
        markWatched(listOf(item))
    }

    fun markWatched(items: Collection<WatchedItem>) {
        markWatched(items = items, trackerHistorySync = WatchedTrackerHistorySync.Mirror)
    }

    internal fun markWatchedFromPlaybackCompletion(item: WatchedItem, syncRemote: Boolean = true) {
        markWatched(
            items = listOf(item),
            trackerHistorySync = WatchedTrackerHistorySync.Skip,
            syncRemote = syncRemote,
        )
    }

    private fun markWatched(
        items: Collection<WatchedItem>,
        trackerHistorySync: WatchedTrackerHistorySync,
        syncRemote: Boolean = true,
    ) {
        ensureLoaded()
        if (items.isEmpty()) return
        val source = activeSource
        val markedAt = WatchedClock.nowEpochMs()
        val timestampedItems = items.map { watchedItem ->
            watchedItem.copy(markedAtEpochMs = markedAt)
        }
        itemsStore.update { accountItems, providerItems, dirtyAccountKeys, dirtyProviderKeys ->
            val targetItems = source.providerId
                ?.let { providerId -> providerItems.getOrPut(providerId, ::mutableMapOf) }
                ?: accountItems
            val dirtyKeys = source.providerId
                ?.let { providerId -> dirtyProviderKeys.getOrPut(providerId, ::mutableSetOf) }
                ?: dirtyAccountKeys
            timestampedItems.forEach { watchedItem ->
                val key = watchedItemKey(watchedItem.type, watchedItem.id, watchedItem.season, watchedItem.episode)
                targetItems[key] = watchedItem
                dirtyKeys += key
            }
        }
        publish()
        persist()
        if (syncRemote) {
            pushMarksToServer(
                items = timestampedItems,
                trackerHistorySync = trackerHistorySync,
                source = source,
            )
        }
    }

    fun unmarkWatched(item: WatchedItem) {
        unmarkWatched(listOf(item))
    }

    fun unmarkWatched(
        id: String,
        type: String,
        season: Int? = null,
        episode: Int? = null,
    ) {
        unmarkWatched(
            listOf(
                WatchedItem(
                    id = id,
                    type = type,
                    name = "",
                    season = season,
                    episode = episode,
                    markedAtEpochMs = 0L,
                ),
            ),
        )
    }

    fun unmarkWatched(items: Collection<WatchedItem>) {
        ensureLoaded()
        if (items.isEmpty()) return
        val source = activeSource
        val (removedItems, removedExtraKeys) = itemsStore.update { accountItems, providerItems, dirtyAccountKeys, dirtyProviderKeys ->
            val targetItems = source.providerId
                ?.let { providerId -> providerItems.getOrPut(providerId, ::mutableMapOf) }
                ?: accountItems
            val dirtyKeys = source.providerId
                ?.let { providerId -> dirtyProviderKeys.getOrPut(providerId, ::mutableSetOf) }
                ?: dirtyAccountKeys
            var extraKeysChanged = false
            val removed = items.mapNotNull { watchedItem ->
                val keys = watchedItemKeys(
                    type = watchedItem.type,
                    id = watchedItem.id,
                    season = watchedItem.season,
                    episode = watchedItem.episode,
                )
                val matchingKey = keys.firstOrNull(targetItems::containsKey)
                source.providerId?.let { providerId ->
                    providerExtraWatchedKeys[providerId]?.let { extraKeys ->
                        val updated = extraKeys - keys
                        if (updated != extraKeys) {
                            providerExtraWatchedKeys[providerId] = updated
                            extraKeysChanged = true
                        }
                    }
                }
                matchingKey?.let(targetItems::remove)?.let { storeItem ->
                    if (watchedItem.videoId != null && storeItem.videoId == null) {
                        storeItem.copy(videoId = watchedItem.videoId)
                    } else {
                        storeItem
                    }
                }?.also { dirtyKeys.remove(matchingKey) }
            }
            removed to extraKeysChanged
        }
        if (removedItems.isNotEmpty()) {
            publish()
            persist()
            pushDeleteToServer(items = removedItems, source = source)
        } else if (source.providerId != null) {
            if (removedExtraKeys) {
                publish()
                persist()
            }
            pushDeleteToServer(items = items.toList(), source = source)
        }
    }

    fun isWatched(
        id: String,
        type: String,
        season: Int? = null,
        episode: Int? = null,
    ): Boolean {
        ensureLoaded()
        val source = activeSource
        val keys = watchedItemKeys(type = type, id = id, season = season, episode = episode)
        val stored = itemsStore.read { accountItems, providerItems, _, _ ->
            source.providerId?.let { providerId ->
                providerItems[providerId]?.let { itemsByKey -> keys.any(itemsByKey::containsKey) } == true
            } ?: keys.any(accountItems::containsKey)
        }
        if (stored) return true
        val providerId = source.providerId ?: return false
        return providerExtraWatchedKeys[providerId]?.let { extraKeys -> keys.any(extraKeys::contains) } == true
    }

    fun isFullyWatchedSeries(id: String, type: String): Boolean {
        val keys = watchedItemKeys(type = type, id = id)
        return keys.any(_fullyWatchedSeriesKeys.value::contains)
    }

    fun reconcileSeriesWatchedState(
        meta: MetaDetails,
        todayIsoDate: String,
        isEpisodeCompleted: (io.github.dimitrysaf.provenio.core.metadata.MetaVideo) -> Boolean = { false },
    ) {
        if (!meta.type.isSeriesLikeWatchedType()) return

        ensureLoaded()
        val shouldMarkSeriesWatched = reconcileFullyWatchedSeriesState(
            meta = meta,
            todayIsoDate = todayIsoDate,
            isEpisodeCompleted = isEpisodeCompleted,
        )
        val seriesWatchedItem = meta.toSeriesWatchedItem()
        val hasSeriesWatchedMarker = isWatched(id = meta.id, type = meta.type)
        log.i {
            "Watched series reconciliation source=$activeSource content=${meta.type}:${meta.id} " +
                "episodes=${meta.videos.size} shouldMarkSeries=$shouldMarkSeriesWatched " +
                "hasSeriesMarker=$hasSeriesWatchedMarker " +
                "matchingItems=${itemsForSourceSnapshot(activeSource).count { it.id == meta.id }}"
        }
        if (shouldMarkSeriesWatched) {
            if (!hasSeriesWatchedMarker) {
                markWatched(seriesWatchedItem)
            }
        } else if (hasSeriesWatchedMarker) {
            unmarkWatched(seriesWatchedItem)
        }
    }

    fun reconcileFullyWatchedSeriesState(
        meta: MetaDetails,
        todayIsoDate: String,
        isEpisodeWatched: (MetaVideo) -> Boolean = { episode ->
            val keys = watchedItemKeys(meta.type, meta.id, episode.season, episode.episode)
            if (keys.any(_uiState.value.watchedKeys::contains)) {
                true
            } else {
                val episodeNumber = episode.episode
                if (episodeNumber != null) {
                    io.github.dimitrysaf.provenio.core.tracking.simkl.SimklAnimeWatchedFallback.isWatched(episode.id, episodeNumber)
                } else {
                    false
                }
            }
        },
        isEpisodeCompleted: (MetaVideo) -> Boolean = { false },
    ): Boolean {
        if (!meta.type.isSeriesLikeWatchedType()) return false

        val shouldMarkSeriesWatched = calculateFullyWatchedSeriesState(
            meta = meta,
            todayIsoDate = todayIsoDate,
            isEpisodeWatched = isEpisodeWatched,
            isEpisodeCompleted = isEpisodeCompleted,
        )
        updateFullyWatchedSeriesStates(
            mapOf(watchedItemKey(meta.type, meta.id) to shouldMarkSeriesWatched),
        )
        return shouldMarkSeriesWatched
    }

    internal fun calculateFullyWatchedSeriesState(
        meta: MetaDetails,
        todayIsoDate: String,
        isEpisodeWatched: (MetaVideo) -> Boolean,
        isEpisodeCompleted: (MetaVideo) -> Boolean,
    ): Boolean {
        if (!meta.type.isSeriesLikeWatchedType()) return false

        ensureLoaded()
        return meta.hasWatchedAllMainSeasonEpisodes(todayIsoDate) { episode ->
            isEpisodeWatched(episode) || isEpisodeCompleted(episode)
        }
    }

    fun updateFullyWatchedSeries(
        id: String,
        type: String,
        isFullyWatched: Boolean,
    ) {
        if (!type.isSeriesLikeWatchedType()) return
        ensureLoaded()
        updateFullyWatchedSeriesKey(
            key = watchedItemKey(type, id),
            isFullyWatched = isFullyWatched,
        )
    }

    private fun updateFullyWatchedSeriesKey(
        key: String,
        isFullyWatched: Boolean,
    ) {
        updateFullyWatchedSeriesStates(mapOf(key to isFullyWatched))
    }

    internal fun updateFullyWatchedSeriesStates(states: Map<String, Boolean>) {
        if (states.isEmpty()) return
        ensureLoaded()
        val source = activeSource
        val current = fullyWatchedSeriesKeysForSource(source)
        val updated = current.toMutableSet().apply {
            states.forEach { (key, isFullyWatched) ->
                if (isFullyWatched) add(key) else remove(key)
            }
        }
        if (updated == current) return
        setFullyWatchedSeriesKeysForSource(source = source, keys = updated)
        publish()
        persist()
    }

    fun setExpandedFullyWatchedSeriesKeys(keys: Set<String>) {
        if (expandedSiblingKeys == keys) return
        expandedSiblingKeys = keys
        publish()
        persist()
    }

    fun currentExpandedSiblingKeys(): Set<String> = expandedSiblingKeys

    /**
     * Returns the base fully-watched series keys from the active source,
     * without sibling expansion. Used by sibling expansion to avoid feedback loops.
     */
    fun baseFullyWatchedSeriesKeys(): Set<String> = fullyWatchedSeriesKeysForSource(activeSource)

    private fun pushMarksToServer(
        items: Collection<WatchedItem>,
        trackerHistorySync: WatchedTrackerHistorySync,
        source: WatchProgressSource,
    ) {
        val profileId = currentProfileId
        val operationGeneration = profileGeneration
        accountScopeSnapshot().launch {
            runCatching {
                if (items.isEmpty()) return@runCatching
                val outcome = pushToTargetsForSource(
                    profileId = profileId,
                    items = items,
                    trackerHistorySync = trackerHistorySync,
                    source = source,
                )
                if (shouldAcknowledgeAccountWatchedPush(source = source, outcome = outcome)) {
                    recordSuccessfulPush(
                        profileId = profileId,
                        operationGeneration = operationGeneration,
                        items = items,
                    )
                }
            }.onFailure { e ->
                log.e(e) { "Failed to push watched items" }
            }
        }
    }

    private fun pushDeleteToServer(
        items: Collection<WatchedItem>,
        source: WatchProgressSource,
    ) {
        val profileId = currentProfileId
        accountScopeSnapshot().launch {
            runCatching {
                if (items.isEmpty()) return@runCatching
                deleteFromTargetsForSource(
                    profileId = profileId,
                    items = items,
                    source = source,
                )
            }.onFailure { e ->
                log.e(e) { "Failed to push watched item delete" }
            }
        }
    }

    private fun publish() {
        val (accountItems, providerItems) = itemsStore.read { storedAccountItems, storedProviderItems, _, _ ->
            storedAccountItems.values.toList() to storedProviderItems.mapValues { (_, itemsByKey) ->
                itemsByKey.values.toList()
            }
        }
        val items = watchedItemsForSource(
            source = activeSource,
            accountItems = accountItems,
            providerItems = providerItems,
        )
            .map(WatchedItem::normalizedMarkedAt)
            .sortedByDescending { it.markedAtEpochMs }
        val fullyWatchedSeriesKeys = fullyWatchedSeriesKeysForSource(activeSource)
        val watchedKeys = items.mapTo(linkedSetOf()) {
            watchedItemKey(it.type, it.id, it.season, it.episode)
        }
        // Merge extra watched keys from providers (e.g. Simkl anime alternate IDs)
        activeSource.providerId?.let { providerId ->
            providerExtraWatchedKeys[providerId]?.let { extraKeys -> watchedKeys += extraKeys }
        }
        val isLoaded = hasLoadedSource(activeSource)
        val hasLoadedRemoteItems = activeSource.providerId
            ?.let(providersLoadedFromRemote::contains)
            ?: accountHasLoadedRemote
        _fullyWatchedSeriesKeys.value = fullyWatchedSeriesKeys + expandedSiblingKeys
        _uiState.value = WatchedUiState(
            items = items,
            watchedKeys = watchedKeys,
            isLoaded = isLoaded,
            hasLoadedRemoteItems = hasLoadedRemoteItems,
        )
        log.i {
            "Watched publish source=$activeSource provider=${activeSource.providerId?.storageId ?: "provenio"} " +
                "items=${items.size} keys=${watchedKeys.size} fullyWatchedSeries=${fullyWatchedSeriesKeys.size} " +
                "isLoaded=$isLoaded hasLoadedRemote=$hasLoadedRemoteItems " +
                "itemKeys=[${diagnosticItemKeySample(items)}] " +
                "fullyWatchedKeys=[${fullyWatchedSeriesKeys.take(watchedDiagnosticSampleLimit).joinToString(",")}]"
        }
    }

    private fun diagnosticItemKeySample(items: Collection<WatchedItem>): String = items
        .asSequence()
        .take(watchedDiagnosticSampleLimit)
        .joinToString(separator = ",") { item ->
            watchedItemKey(item.type, item.id, item.season, item.episode)
        }

    /**
     * Observes provider extra watched keys (e.g. Simkl anime alternate IDs).
     * When the provider's snapshot changes (after mutations, syncs), recomputes
     * extra keys, re-pulls watched items, and re-publishes so watchedKeys and
     * items stay reactive and current.
     */
    private fun startExtraKeysObserverIfNeeded() {
        if (extraKeysObserverJob != null) return
        val providerId = activeSource.providerId ?: return
        val adapter = TrackingProviderRegistry.connectedWatchedProviders()
            .firstOrNull { it.providerId == providerId } ?: return
        extraKeysObserverJob = accountScopeSnapshot().launch {
            adapter.observeExtraWatchedKeys(currentProfileId)
                .distinctUntilChanged()
                .collectLatest { extraKeys ->
                    val keysChanged = extraWatchedKeysChanged(
                        previous = providerExtraWatchedKeys[providerId],
                        current = extraKeys,
                    )
                    if (keysChanged) {
                        val freshItems = watchedProviderRefreshOrNull(
                            refresh = {
                                adapter.pull(
                                    profileId = currentProfileId,
                                    pageSize = watchedItemsPageSize,
                                )
                            },
                            onFailure = { error ->
                                log.w(error) { "Failed to refresh watched items from ${providerId.storageId}" }
                            },
                        ) ?: return@collectLatest
                        providerExtraWatchedKeys[providerId] = extraKeys
                        itemsStore.update { _, providerItems, _, dirtyProviderKeys ->
                            val dirtyKeys = dirtyProviderKeys.getOrPut(providerId, ::mutableSetOf)
                            val merged = mergeWatchedSnapshot(
                                serverItems = freshItems,
                                localItems = providerItems[providerId]?.values.orEmpty().toList(),
                                dirtyKeys = dirtyKeys,
                                acknowledgeDirtyByPresence = true,
                            )
                            providerItems[providerId] = merged.items.toMutableMap()
                            dirtyKeys.clear()
                            dirtyKeys += merged.dirtyKeys
                        }
                        loadedProviders += providerId
                        providersLoadedFromRemote += providerId
                        publish()
                        persist()
                    }
                }
        }
    }

    private fun stopExtraKeysObserver() {
        extraKeysObserverJob?.cancel()
        extraKeysObserverJob = null
    }

    private fun persist() {
        val storedPayload = itemsStore.read { accountItems, providerItems, dirtyAccountKeys, dirtyProviderKeys ->
            val providerIds = buildSet {
                addAll(providerItems.keys)
                addAll(dirtyProviderKeys.keys)
                addAll(providerFullyWatchedSeriesKeys.keys)
                addAll(providerExtraWatchedKeys.keys)
                addAll(loadedProviders)
            }
            StoredWatchedPayload(
                items = accountItems.values
                    .map(WatchedItem::normalizedMarkedAt)
                    .sortedByDescending { it.markedAtEpochMs },
                fullyWatchedSeriesKeys = accountFullyWatchedSeriesKeys,
                expandedSiblingKeys = expandedSiblingKeys,
                lastSuccessfulPushEpochMs = lastSuccessfulPushEpochMs,
                deltaCursorEventId = deltaCursorEventId,
                deltaInitialized = deltaInitialized,
                dirtyWatchedKeys = dirtyAccountKeys.toSet(),
                providerPayloads = providerIds.associate { providerId ->
                    val items = providerItems[providerId]
                        .orEmpty()
                        .values
                        .map(WatchedItem::normalizedMarkedAt)
                    providerId.storageId to StoredProviderWatchedPayload(
                        itemGroups = compactProviderWatchedItems(items),
                        fullyWatchedSeriesKeys = providerFullyWatchedSeriesKeys[providerId].orEmpty(),
                        extraWatchedKeyGroups = compactExtraWatchedKeys(
                            providerExtraWatchedKeys[providerId].orEmpty(),
                        ),
                        dirtyWatchedKeys = dirtyProviderKeys[providerId].orEmpty(),
                    )
                },
            )
        }
        WatchedStorage.savePayload(
            currentProfileId,
            json.encodeToString(storedPayload),
        )
    }

    private fun recordSuccessfulPush(
        profileId: Int,
        operationGeneration: Long,
        items: Collection<WatchedItem>,
    ) {
        if (profileId != currentProfileId || operationGeneration != profileGeneration) return
        val latestPushed = items
            .asSequence()
            .map { item -> normalizeWatchedMarkedAtEpochMs(item.markedAtEpochMs) }
            .maxOrNull()
            ?: return
        val changed = itemsStore.update { accountItems, _, dirtyAccountKeys, _ ->
            val acknowledgedDirtyKeys = acknowledgeSuccessfulWatchedPush(
                currentItems = accountItems,
                dirtyKeys = dirtyAccountKeys,
                pushedItems = items,
            )
            val updatedLastSuccessfulPushEpochMs = maxOf(lastSuccessfulPushEpochMs, latestPushed)
            if (
                acknowledgedDirtyKeys == dirtyAccountKeys &&
                updatedLastSuccessfulPushEpochMs == lastSuccessfulPushEpochMs
            ) {
                false
            } else {
                dirtyAccountKeys.clear()
                dirtyAccountKeys += acknowledgedDirtyKeys
                lastSuccessfulPushEpochMs = updatedLastSuccessfulPushEpochMs
                true
            }
        }
        if (changed) persist()
    }

    private suspend fun pushToTargetsForSource(
        profileId: Int,
        items: Collection<WatchedItem>,
        trackerHistorySync: WatchedTrackerHistorySync,
        source: WatchProgressSource,
    ): WatchedPushOutcome {
        val succeededTrackerProviderIds = linkedSetOf<TrackingProviderId>()

        if (trackerHistorySync == WatchedTrackerHistorySync.Mirror) {
            TrackingProviderRegistry.connectedWatchedProviders().forEach { provider ->
                try {
                    provider.push(profileId = profileId, items = items)
                    succeededTrackerProviderIds += provider.providerId
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Throwable) {
                    log.e(error) { "Failed to push watched items to ${provider.providerId.storageId}" }
                }
            }
        }
        return WatchedPushOutcome(
            accountSyncSucceeded = false,
            succeededTrackerProviderIds = succeededTrackerProviderIds,
        )
    }

    private suspend fun deleteFromTargetsForSource(
        profileId: Int,
        items: Collection<WatchedItem>,
        source: WatchProgressSource,
    ) {
        TrackingProviderRegistry.connectedWatchedProviders().forEach { provider ->
            try {
                provider.delete(profileId = profileId, items = items)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                log.e(error) { "Failed to delete watched items from ${provider.providerId.storageId}" }
            }
        }
    }

    private fun accountScopeSnapshot(): CoroutineScope =
        synchronized(accountScopeLock) {
            accountScope
        }

    private fun connectedWatchedProviderIds(): Set<TrackingProviderId> =
        TrackingProviderRegistry.connectedWatchedProviders()
            .mapTo(linkedSetOf()) { provider -> provider.providerId }
}

internal data class WatchedSnapshotMerge(
    val items: Map<String, WatchedItem>,
    val dirtyKeys: Set<String>,
)

internal fun mergeWatchedSnapshot(
    serverItems: Collection<WatchedItem>,
    localItems: Collection<WatchedItem>,
    dirtyKeys: Set<String>,
    acknowledgeDirtyByPresence: Boolean = false,
): WatchedSnapshotMerge {
    val remoteByKey = serverItems
        .map(WatchedItem::normalizedMarkedAt)
        .associateBy { watchedItemKey(it.type, it.id, it.season, it.episode) }
        .toMutableMap()
    val localByKey = localItems
        .map(WatchedItem::normalizedMarkedAt)
        .associateBy { watchedItemKey(it.type, it.id, it.season, it.episode) }
    val remainingDirtyKeys = dirtyKeys
        .filterTo(mutableSetOf()) { key -> key in localByKey }

    remainingDirtyKeys.toList().forEach { key ->
        val localItem = localByKey.getValue(key)
        val remoteItem = remoteByKey[key]
        if (remoteItem == null) {
            remoteByKey[key] = localItem
        } else if (acknowledgeDirtyByPresence || remoteItem.markedAtEpochMs >= localItem.markedAtEpochMs) {
            remainingDirtyKeys -= key
        } else {
            remoteByKey[key] = localItem
        }
    }

    return WatchedSnapshotMerge(
        items = remoteByKey,
        dirtyKeys = remainingDirtyKeys,
    )
}

internal fun acknowledgeSuccessfulWatchedPush(
    currentItems: Map<String, WatchedItem>,
    dirtyKeys: Set<String>,
    pushedItems: Collection<WatchedItem>,
): Set<String> {
    val remainingDirtyKeys = dirtyKeys.toMutableSet()
    pushedItems
        .map(WatchedItem::normalizedMarkedAt)
        .forEach { pushedItem ->
            val key = watchedItemKey(
                type = pushedItem.type,
                id = pushedItem.id,
                season = pushedItem.season,
                episode = pushedItem.episode,
            )
            val currentItem = currentItems[key]?.normalizedMarkedAt()
            if (currentItem == null || currentItem.markedAtEpochMs <= pushedItem.markedAtEpochMs) {
                remainingDirtyKeys -= key
            }
        }
    return remainingDirtyKeys
}

internal fun effectiveWatchedSource(
    requestedSource: WatchProgressSource,
    connectedProviderIds: Set<TrackingProviderId>,
): WatchProgressSource = effectiveWatchProgressSource(
    requestedSource = requestedSource,
    isProviderAuthenticated = { providerId -> providerId in connectedProviderIds },
)

private fun String.isSeriesLikeWatchedType(): Boolean =
    trim().lowercase() in setOf("series", "show", "tv", "tvshow")
