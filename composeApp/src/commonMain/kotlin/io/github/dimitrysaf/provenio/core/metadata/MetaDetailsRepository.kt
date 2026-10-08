package io.github.dimitrysaf.provenio.core.metadata

import co.touchlab.kermit.Logger
import io.github.dimitrysaf.provenio.core.addons.AddonManifest
import io.github.dimitrysaf.provenio.core.addons.AddonRepository
import io.github.dimitrysaf.provenio.core.addons.buildAddonResourceUrl
import io.github.dimitrysaf.provenio.core.addons.enabledAddons
import io.github.dimitrysaf.provenio.core.addons.fetchAddonResponseText
import io.github.dimitrysaf.provenio.core.home.HomeCatalogSettingsRepository
import io.github.dimitrysaf.provenio.core.home.MetaPreview
import io.github.dimitrysaf.provenio.core.home.filterReleasedItems
import io.github.dimitrysaf.provenio.core.metadata.mdblist.MdbListMetadataService
import io.github.dimitrysaf.provenio.core.metadata.mdblist.MdbListSettingsRepository
import io.github.dimitrysaf.provenio.core.metadata.tmdb.TmdbMetadataService
import io.github.dimitrysaf.provenio.core.metadata.tmdb.TmdbService
import io.github.dimitrysaf.provenio.core.metadata.tmdb.TmdbSettingsRepository
import io.github.dimitrysaf.provenio.core.tracking.trakt.TraktAuthRepository
import io.github.dimitrysaf.provenio.core.tracking.trakt.TraktConnectionMode
import io.github.dimitrysaf.provenio.core.tracking.trakt.TraktRelatedRepository
import io.github.dimitrysaf.provenio.core.tracking.TrackingSettingsRepository
import io.github.dimitrysaf.provenio.core.tracking.trakt.shouldUseTraktMoreLikeThis
import io.github.dimitrysaf.provenio.core.watch.progress.CurrentDateProvider
import io.github.dimitrysaf.provenio.core.network.LoadFailure
import io.github.dimitrysaf.provenio.core.network.LoadFailureKind
import io.github.dimitrysaf.provenio.core.network.toLoadFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

object MetaDetailsRepository {
    private data class CachedMetaEntry(
        val baseMeta: MetaDetails,
        val metaScreenMeta: MetaDetails? = null,
        val metaScreenSettingsFingerprint: String? = null,
    )

    private val log = Logger.withTag("MetaDetailsRepo")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _uiState = MutableStateFlow(MetaDetailsUiState())
    val uiState: StateFlow<MetaDetailsUiState> = _uiState.asStateFlow()
    private var activeRequestKey: String? = null
    private var loadJob: Job? = null
    private val cachedMetaByRequestKey = mutableMapOf<String, CachedMetaEntry>()
    private val previews = MutableStateFlow<Map<String, MetaPreview>>(emptyMap())

    fun load(type: String, id: String) {
        log.d { "load() called — type=$type id=$id" }
        val requestKey = "$type:$id"
        val currentState = _uiState.value
        val mdbListSettings = MdbListSettingsRepository.snapshot()
        val metaScreenSettingsFingerprint = buildMetaScreenSettingsFingerprint(mdbListSettings)

        cachedMetaByRequestKey[requestKey]?.let { cachedEntry ->
            cachedEntry.metaScreenMeta
                ?.takeIf { cachedEntry.metaScreenSettingsFingerprint == metaScreenSettingsFingerprint }
                ?.let { cachedMeta ->
                    startRequest(requestKey)
                    _uiState.value = MetaDetailsUiState(meta = cachedMeta.withUnreleasedFilter(), requestKey = requestKey)
                    return
                }

            val cachedBaseMeta = cachedEntry.baseMeta
            if (!shouldEnrichForMetaScreen(cachedBaseMeta, id, mdbListSettings)) {
                startRequest(requestKey)
                _uiState.value = MetaDetailsUiState(meta = cachedBaseMeta.withUnreleasedFilter(), requestKey = requestKey)
                return
            }

            if (currentState.isLoading && activeRequestKey == requestKey) {
                log.d { "Meta screen enrichment already in flight — type=$type id=$id" }
                return
            }

            startRequest(requestKey)
            _uiState.value = MetaDetailsUiState(
                isLoading = true,
                meta = cachedBaseMeta,
                requestKey = requestKey,
            )

            loadJob = scope.launch {
                val enrichedMeta = withContext(Dispatchers.Default) {
                    enrichForMetaScreen(
                        requestKey = requestKey,
                        meta = cachedBaseMeta,
                        fallbackItemId = id,
                        fallbackItemType = type,
                        settings = mdbListSettings,
                        settingsFingerprint = metaScreenSettingsFingerprint,
                    )
                }
                if (activeRequestKey != requestKey) return@launch
                _uiState.value = MetaDetailsUiState(meta = enrichedMeta.withUnreleasedFilter(), requestKey = requestKey)
            }
            return
        }

        if (currentState.meta?.type == type && currentState.meta.id == id && !currentState.isLoading) {
            log.d { "Skipping reload for cached meta — type=$type id=$id" }
            activeRequestKey = requestKey
            return
        }

        if (currentState.isLoading && activeRequestKey == requestKey) {
            log.d { "Request already in flight — type=$type id=$id" }
            return
        }

        startRequest(requestKey)
        _uiState.value = MetaDetailsUiState(isLoading = true, requestKey = requestKey)

        loadJob = scope.launch {
            val metaLookupId = resolveMetaLookupId(itemId = id, itemType = type)
            val manifests = findReadyMetaManifests(type = type, id = metaLookupId)

            var addonMeta: MetaDetails? = null
            var failures = emptyList<LoadFailure>()
            if (manifests.isNotEmpty()) {
                for (attempt in 1..META_LOAD_ATTEMPTS) {
                    val attemptResult = fetchFirstAvailableMeta(manifests, type, metaLookupId) { parsed ->
                        if (activeRequestKey == requestKey && _uiState.value.meta == null) {
                            _uiState.value = MetaDetailsUiState(
                                isLoading = true,
                                meta = parsed.withUnreleasedFilter(),
                                requestKey = requestKey,
                            )
                        }
                    }
                    addonMeta = attemptResult.meta
                    failures = attemptResult.failures
                    if (addonMeta != null || attempt == META_LOAD_ATTEMPTS) break
                    log.w { "No addon answered for type=$type id=$id, retrying" }
                    delay(META_RETRY_DELAY_MS)
                }
            }
            if (activeRequestKey != requestKey) return@launch

            if (addonMeta != null) {
                publishLoadedMeta(
                    requestKey = requestKey,
                    meta = addonMeta,
                    fallbackItemId = metaLookupId,
                    fallbackItemType = type,
                    mdbListSettings = mdbListSettings,
                    metaScreenSettingsFingerprint = metaScreenSettingsFingerprint,
                )
                return@launch
            }

            val tmdbMeta = tryFetchTmdbFallbackMeta(type = type, id = id)
            if (activeRequestKey != requestKey) return@launch
            if (tmdbMeta != null) {
                publishLoadedMeta(
                    requestKey = requestKey,
                    meta = tmdbMeta,
                    fallbackItemId = id,
                    fallbackItemType = type,
                    mdbListSettings = mdbListSettings,
                    metaScreenSettingsFingerprint = metaScreenSettingsFingerprint,
                )
                return@launch
            }

            val previewMeta = previews.value[requestKey]?.toFallbackMeta()
            if (previewMeta != null) {
                log.w { "Showing the catalog preview for type=$type id=$id, no addon provided details" }
                _uiState.value = MetaDetailsUiState(meta = previewMeta.withUnreleasedFilter(), requestKey = requestKey)
                return@launch
            }

            if (manifests.isEmpty()) log.w { "No addon provides meta for type=$type id=$id" }
            _uiState.value = MetaDetailsUiState(
                failure = if (manifests.isEmpty()) MetaLoadFailure.NoMetaAddon else MetaLoadFailure.AddonsFailed(failures),
                requestKey = requestKey,
            )
            activeRequestKey = null
        }
    }

    /** Makes [requestKey] the only request allowed to publish, cancelling the one before it. */
    private fun startRequest(requestKey: String) {
        if (activeRequestKey != requestKey) {
            loadJob?.cancel()
            loadJob = null
        }
        activeRequestKey = requestKey
    }

    /** Remembers catalog previews so a title no addon can describe still opens with what the catalog showed. */
    fun rememberPreviews(items: List<MetaPreview>) {
        if (items.isEmpty()) return
        previews.update { current ->
            val merged = current + items.associateBy { "${it.type}:${it.id}" }
            if (merged.size <= MAX_REMEMBERED_PREVIEWS) merged else merged.entries.toList().takeLast(MAX_REMEMBERED_PREVIEWS).associate { it.toPair() }
        }
    }

    private class MetaAttempt(val meta: MetaDetails?, val failures: List<LoadFailure>)

    /**
     * Asks every addon at once and returns the answer from the highest-priority addon that has one,
     * so one slow or broken addon no longer holds up the others. Without an answer, the failures
     * say why each addon had none.
     */
    private suspend fun fetchFirstAvailableMeta(
        manifests: List<AddonManifest>,
        type: String,
        id: String,
        onParsed: (MetaDetails) -> Unit,
    ): MetaAttempt = coroutineScope {
        val requests = manifests.map { manifest ->
            async(Dispatchers.Default) {
                withTimeoutOrNull(META_REQUEST_TIMEOUT_MS) { tryFetchRawMeta(manifest, type, id) }
                    ?: MetaFetchOutcome.Failed(LoadFailure(sourceName = manifest.name, kind = LoadFailureKind.Timeout))
            }
        }
        var found: MetaDetails? = null
        val failures = mutableListOf<LoadFailure>()
        for (request in requests) {
            when (val outcome = request.await()) {
                is MetaFetchOutcome.Loaded -> found = outcome.meta
                is MetaFetchOutcome.Failed -> failures += outcome.failure
            }
            if (found != null) break
        }
        requests.forEach { it.cancel() }
        val meta = found?.let { raw ->
            onParsed(raw)
            withContext(Dispatchers.Default) { enrichFetchedMeta(raw, id, includeMdbList = false) }
        }
        MetaAttempt(meta = meta, failures = failures)
    }

    fun peek(type: String, id: String): MetaDetails? {
        val requestKey = "$type:$id"
        val currentMeta = _uiState.value.meta?.takeIf { it.type == type && it.id == id }
        if (currentMeta != null) return currentMeta

        val metaScreenSettingsFingerprint = buildMetaScreenSettingsFingerprint(MdbListSettingsRepository.snapshot())
        val cachedEntry = cachedMetaByRequestKey[requestKey] ?: return null
        return cachedEntry.metaScreenMeta
            ?.takeIf { cachedEntry.metaScreenSettingsFingerprint == metaScreenSettingsFingerprint }
            ?: cachedEntry.baseMeta
    }

    fun clear() {
        loadJob?.cancel()
        loadJob = null
        activeRequestKey = null
        cachedMetaByRequestKey.clear()
        _uiState.value = MetaDetailsUiState()
    }

    suspend fun fetch(type: String, id: String, cacheResult: Boolean = true): MetaDetails? {
        val requestKey = "$type:$id"
        cachedMetaByRequestKey[requestKey]?.let { return it.baseMeta }

        // Callers asking for the same title at the same time share one request. Only the request
        // in flight is shared; once it finishes, failed or not, the next caller asks again.
        val request = inFlightFetchesLock.withLock {
            inFlightFetches.getOrPut(requestKey) {
                fetchScope.async {
                    try {
                        fetchUncached(type, id)
                    } finally {
                        withContext(NonCancellable) {
                            inFlightFetchesLock.withLock { inFlightFetches.remove(requestKey) }
                        }
                    }
                }
            }
        }
        val result = request.await()
        if (result != null && cacheResult) {
            cachedMetaByRequestKey[requestKey] = CachedMetaEntry(baseMeta = result)
        }
        return result
    }

    private suspend fun fetchUncached(type: String, id: String): MetaDetails? {
        val metaLookupId = resolveMetaLookupId(itemId = id, itemType = type)
        val manifests = findReadyMetaManifests(type = type, id = metaLookupId)

        for (manifest in manifests) {
            val result = withTimeoutOrNull(FETCH_TIMEOUT_MS) {
                withContext(Dispatchers.Default) {
                    tryFetchMeta(manifest, type, metaLookupId, includeMdbList = false)
                }
            }
            if (result != null) return result
        }

        return tryFetchTmdbFallbackMeta(type = type, id = id)
    }

    private sealed interface MetaFetchOutcome {
        data class Loaded(val meta: MetaDetails) : MetaFetchOutcome
        data class Failed(val failure: LoadFailure) : MetaFetchOutcome
    }

    private val fetchScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val inFlightFetchesLock = Mutex()
    private val inFlightFetches = mutableMapOf<String, Deferred<MetaDetails?>>()

    private const val FETCH_TIMEOUT_MS = 5_000L
    private const val META_REQUEST_TIMEOUT_MS = 15_000L
    private const val META_LOAD_ATTEMPTS = 2
    private const val META_RETRY_DELAY_MS = 800L
    private const val MAX_REMEMBERED_PREVIEWS = 600
    private const val METADATA_PROVIDER_READY_TIMEOUT_MS = 10_000L
    private const val TMDB_ENRICH_TIMEOUT_MS = 5_000L
    private const val MDBLIST_ENRICH_TIMEOUT_MS = 5_000L

    private suspend fun tryFetchMeta(
        manifest: AddonManifest,
        type: String,
        id: String,
        includeMdbList: Boolean,
        onParsed: ((MetaDetails) -> Unit)? = null,
    ): MetaDetails? {
        val result = (tryFetchRawMeta(manifest, type, id) as? MetaFetchOutcome.Loaded)?.meta ?: return null
        onParsed?.invoke(result)
        return enrichFetchedMeta(result, id, includeMdbList)
    }

    private suspend fun tryFetchRawMeta(
        manifest: AddonManifest,
        type: String,
        id: String,
    ): MetaFetchOutcome {
        val url = buildAddonResourceUrl(
            manifestUrl = manifest.transportUrl,
            resource = "meta",
            type = type,
            id = id,
        )

        return try {
            log.d { "Fetching meta from: $url" }
            val payload = fetchAddonResponseText(url)
            log.d { "Raw payload length=${payload.length}, first 500 chars: ${payload.take(500)}" }
            MetaFetchOutcome.Loaded(MetaDetailsParser.parse(payload))
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            val failure = e.toLoadFailure(sourceName = manifest.name)
            when (failure.kind) {
                // Expected answers from a healthy add-on; a stack trace adds nothing.
                LoadFailureKind.NotFound,
                LoadFailureKind.RateLimited,
                -> log.w { "Meta unavailable from $url: ${failure.kind} (${e.message})" }
                else -> log.e(e) { "Failed to fetch/parse meta from $url (manifest=${manifest.transportUrl})" }
            }
            MetaFetchOutcome.Failed(failure)
        }
    }

    private suspend fun enrichFetchedMeta(
        result: MetaDetails,
        id: String,
        includeMdbList: Boolean,
    ): MetaDetails {
        TmdbSettingsRepository.ensureLoaded()
        val tmdbEnriched = runCatching {
            withTimeoutOrNull(TMDB_ENRICH_TIMEOUT_MS) {
                TmdbMetadataService.enrichMeta(
                    meta = result,
                    fallbackItemId = id,
                    settings = TmdbSettingsRepository.snapshot(),
                )
            }
        }.onFailure { if (it is CancellationException) throw it }.getOrNull() ?: result
        val enriched = if (includeMdbList) {
            MdbListSettingsRepository.ensureLoaded()
            runCatching {
                withTimeoutOrNull(MDBLIST_ENRICH_TIMEOUT_MS) {
                    MdbListMetadataService.enrichMeta(
                        meta = tmdbEnriched,
                        fallbackItemId = id,
                        settings = MdbListSettingsRepository.snapshot(),
                    )
                }
            }.onFailure { if (it is CancellationException) throw it }.getOrNull() ?: tmdbEnriched
        } else {
            tmdbEnriched
        }
        log.d { "Parsed meta: type=${enriched.type}, name=${enriched.name}, videos=${enriched.videos.size}" }
        return enriched
    }

    private suspend fun findReadyMetaManifests(type: String, id: String): List<AddonManifest> {
        AddonRepository.initialize()

        findMetaManifests(AddonRepository.uiState.value, type, id).takeIf { it.isNotEmpty() }?.let { return it }

        if (!AddonRepository.uiState.value.hasPendingEnabledAddonManifests()) {
            return emptyList()
        }

        val readyState = withTimeoutOrNull(METADATA_PROVIDER_READY_TIMEOUT_MS) {
            AddonRepository.uiState.first { state ->
                findMetaManifests(state, type, id).isNotEmpty() ||
                    !state.hasPendingEnabledAddonManifests()
            }
        } ?: AddonRepository.uiState.value

        return findMetaManifests(readyState, type, id)
    }

    private fun findMetaManifests(state: io.github.dimitrysaf.provenio.core.addons.AddonsUiState, type: String, id: String): List<AddonManifest> =
        state.addons
            .enabledAddons()
            .mapNotNull { it.manifest }
            .filter { manifest ->
                manifest.resources.any { resource ->
                    resource.name == "meta" &&
                        resource.types.contains(type) &&
                        (resource.idPrefixes.isEmpty() || resource.idPrefixes.any { id.startsWith(it) })
                }
            }

    private fun io.github.dimitrysaf.provenio.core.addons.AddonsUiState.hasPendingEnabledAddonManifests(): Boolean =
        addons.enabledAddons().any { addon -> addon.manifest == null && addon.isRefreshing }

    private suspend fun resolveMetaLookupId(itemId: String, itemType: String): String {
        val tmdbId = itemId
            .takeIf { it.startsWith("tmdb:", ignoreCase = true) }
            ?.substringAfter(':')
            ?.substringBefore(':')
            ?.toIntOrNull()
            ?: return itemId

        return withTimeoutOrNull(FETCH_TIMEOUT_MS) {
            TmdbService.tmdbToImdb(tmdbId = tmdbId, mediaType = itemType)
        }
            ?.takeIf { it.isNotBlank() }
            ?: itemId
    }

    private suspend fun tryFetchTmdbFallbackMeta(type: String, id: String): MetaDetails? =
        withTimeoutOrNull(TMDB_ENRICH_TIMEOUT_MS) {
            TmdbMetadataService.fetchStandaloneMeta(
                type = type,
                id = id,
                settings = TmdbSettingsRepository.snapshot(),
            )
        }

    private suspend fun publishLoadedMeta(
        requestKey: String,
        meta: MetaDetails,
        fallbackItemId: String,
        fallbackItemType: String,
        mdbListSettings: io.github.dimitrysaf.provenio.core.metadata.mdblist.MdbListSettings,
        metaScreenSettingsFingerprint: String,
    ) {
        val cachedEntry = CachedMetaEntry(baseMeta = meta)
        cachedMetaByRequestKey[requestKey] = cachedEntry
        if (activeRequestKey != requestKey) return

        if (!shouldEnrichForMetaScreen(meta, fallbackItemId, mdbListSettings)) {
            _uiState.value = MetaDetailsUiState(meta = meta.withUnreleasedFilter(), requestKey = requestKey)
            return
        }

        _uiState.value = MetaDetailsUiState(
            isLoading = true,
            meta = meta,
            requestKey = requestKey,
        )
        val enrichedMeta = withContext(Dispatchers.Default) {
            enrichForMetaScreen(
                requestKey = requestKey,
                meta = meta,
                fallbackItemId = fallbackItemId,
                fallbackItemType = fallbackItemType,
                settings = mdbListSettings,
                settingsFingerprint = metaScreenSettingsFingerprint,
            )
        }
        cachedMetaByRequestKey[requestKey] = cachedEntry.copy(
            metaScreenMeta = enrichedMeta,
            metaScreenSettingsFingerprint = metaScreenSettingsFingerprint,
        )
        if (activeRequestKey != requestKey) return
        _uiState.value = MetaDetailsUiState(meta = enrichedMeta.withUnreleasedFilter(), requestKey = requestKey)
    }

    private suspend fun enrichForMetaScreen(
        requestKey: String,
        meta: MetaDetails,
        fallbackItemId: String,
        fallbackItemType: String,
        settings: io.github.dimitrysaf.provenio.core.metadata.mdblist.MdbListSettings,
        settingsFingerprint: String,
    ): MetaDetails {
        val mdbListEnrichedMeta = withTimeoutOrNull(MDBLIST_ENRICH_TIMEOUT_MS) {
            MdbListMetadataService.enrichMeta(
                meta = meta,
                fallbackItemId = fallbackItemId,
                settings = settings,
            )
        } ?: meta
        val enrichedMeta = applyMoreLikeThisSource(
            meta = mdbListEnrichedMeta,
            fallbackItemId = fallbackItemId,
            fallbackItemType = fallbackItemType,
        )

        cachedMetaByRequestKey[requestKey] = cachedMetaByRequestKey[requestKey]
            ?.copy(
                metaScreenMeta = enrichedMeta,
                metaScreenSettingsFingerprint = settingsFingerprint,
            )
            ?: CachedMetaEntry(
                baseMeta = meta,
                metaScreenMeta = enrichedMeta,
                metaScreenSettingsFingerprint = settingsFingerprint,
            )

        return enrichedMeta
    }

    private suspend fun applyMoreLikeThisSource(
        meta: MetaDetails,
        fallbackItemId: String,
        fallbackItemType: String,
    ): MetaDetails {
        TrackingSettingsRepository.ensureLoaded()
        TraktAuthRepository.ensureLoaded()
        TmdbSettingsRepository.ensureLoaded()

        val trackingSettings = TrackingSettingsRepository.uiState.value
        val isTraktAuthenticated = TraktAuthRepository.uiState.value.mode == TraktConnectionMode.CONNECTED
        val shouldUseTrakt = shouldUseTraktMoreLikeThis(
            isAuthenticated = isTraktAuthenticated,
            source = trackingSettings.moreLikeThisSource,
        ) && supportsMoreLikeThis(meta, fallbackItemType)

        if (shouldUseTrakt) {
            val items = runCatching {
                TraktRelatedRepository.getRelated(
                    meta = meta,
                    fallbackItemId = fallbackItemId,
                    fallbackItemType = fallbackItemType,
                )
            }.onFailure { error ->
                log.w { "Failed to load Trakt related titles for ${meta.id}: ${error.message}" }
            }.getOrDefault(emptyList())

            return meta.copy(
                moreLikeThis = items,
                moreLikeThisSource = MoreLikeThisSource.TRAKT.takeIf { items.isNotEmpty() },
            )
        }

        val tmdbSettings = TmdbSettingsRepository.snapshot()
        if (!tmdbSettings.enabled || !tmdbSettings.useMoreLikeThis) {
            return meta.copy(moreLikeThis = emptyList(), moreLikeThisSource = null)
        }

        return meta.copy(
            moreLikeThisSource = MoreLikeThisSource.TMDB.takeIf { meta.moreLikeThis.isNotEmpty() },
        )
    }

    private fun shouldFetchMdbListOnMetaScreen(
        meta: MetaDetails,
        fallbackItemId: String,
        settings: io.github.dimitrysaf.provenio.core.metadata.mdblist.MdbListSettings,
    ): Boolean = MdbListMetadataService.shouldFetchForMeta(
        meta = meta,
        fallbackItemId = fallbackItemId,
        settings = settings,
    )

    private fun shouldEnrichForMetaScreen(
        meta: MetaDetails,
        fallbackItemId: String,
        settings: io.github.dimitrysaf.provenio.core.metadata.mdblist.MdbListSettings,
    ): Boolean {
        if (shouldFetchMdbListOnMetaScreen(meta, fallbackItemId, settings)) return true
        return shouldApplyMoreLikeThisSource(meta)
    }

    private fun shouldApplyMoreLikeThisSource(meta: MetaDetails): Boolean {
        TrackingSettingsRepository.ensureLoaded()
        TraktAuthRepository.ensureLoaded()
        TmdbSettingsRepository.ensureLoaded()

        val trackingSettings = TrackingSettingsRepository.uiState.value
        val isTraktAuthenticated = TraktAuthRepository.uiState.value.mode == TraktConnectionMode.CONNECTED
        val tmdbSettings = TmdbSettingsRepository.snapshot()
        return shouldUseTraktMoreLikeThis(
            isAuthenticated = isTraktAuthenticated,
            source = trackingSettings.moreLikeThisSource,
        ) || !tmdbSettings.enabled || !tmdbSettings.useMoreLikeThis || meta.moreLikeThisSource == null && meta.moreLikeThis.isNotEmpty()
    }

    private fun buildMetaScreenSettingsFingerprint(
        settings: io.github.dimitrysaf.provenio.core.metadata.mdblist.MdbListSettings,
    ): String {
        TrackingSettingsRepository.ensureLoaded()
        TraktAuthRepository.ensureLoaded()
        TmdbSettingsRepository.ensureLoaded()
        val providers = settings.enabledProvidersInPriorityOrder().joinToString(",")
        val trackingSettings = TrackingSettingsRepository.uiState.value
        val traktAuthMode = TraktAuthRepository.uiState.value.mode
        val tmdbSettings = TmdbSettingsRepository.snapshot()
        return buildString {
            append("${settings.enabled}:${settings.apiKey.trim()}:$providers")
            append("|more_like=${trackingSettings.moreLikeThisSource}:$traktAuthMode")
            append("|tmdb=${tmdbSettings.enabled}:${tmdbSettings.useMoreLikeThis}:${tmdbSettings.language}")
        }
    }

    private fun supportsMoreLikeThis(meta: MetaDetails, fallbackItemType: String): Boolean =
        normalizeMoreLikeThisType(meta.type) != null || normalizeMoreLikeThisType(fallbackItemType) != null

    private fun normalizeMoreLikeThisType(value: String?): String? =
        when (value?.trim()?.lowercase()) {
            "movie", "film" -> "movie"
            "series", "show", "tv", "tvshow" -> "series"
            else -> null
        }

    private fun MetaDetails.withUnreleasedFilter(): MetaDetails {
        if (!HomeCatalogSettingsRepository.snapshot().hideUnreleasedContent) return this
        val todayIsoDate = CurrentDateProvider.todayIsoDate()
        val releasedMoreLikeThis = moreLikeThis.filterReleasedItems(todayIsoDate)
        return copy(
            moreLikeThis = releasedMoreLikeThis,
            moreLikeThisSource = moreLikeThisSource.takeIf { releasedMoreLikeThis.isNotEmpty() },
            collectionItems = collectionItems.filterReleasedItems(todayIsoDate),
        )
    }

   
    fun findEmbeddedStreams(
        videoId: String,
        type: String? = null,
        parentMetaId: String? = null,
    ): List<io.github.dimitrysaf.provenio.core.streams.StreamItem> {
        val currentMeta = _uiState.value.meta?.takeIf { parentMetaId == null || it.id == parentMetaId }
        val parentMeta = if (type != null && parentMetaId != null) peek(type, parentMetaId) else null
        return listOfNotNull(currentMeta, parentMeta)
            .firstNotNullOfOrNull { meta -> meta.embeddedStreamsFor(videoId).takeIf { it.isNotEmpty() } }
            .orEmpty()
    }

    fun isMetaKnown(type: String, id: String): Boolean = peek(type, id) != null

    suspend fun fetchEmbeddedStreams(
        type: String,
        videoId: String,
        parentMetaId: String,
    ): List<io.github.dimitrysaf.provenio.core.streams.StreamItem> =
        fetch(type, parentMetaId)?.embeddedStreamsFor(videoId).orEmpty()

    private fun MetaDetails.embeddedStreamsFor(videoId: String): List<io.github.dimitrysaf.provenio.core.streams.StreamItem> {
        val videosWithStreams = videos.filter { it.streams.isNotEmpty() }
        if (videosWithStreams.isEmpty()) return emptyList()

        val directMatch = videosWithStreams.firstOrNull { it.id == videoId }
        if (directMatch != null) return directMatch.streams

        val parts = videoId.split(":")
        if (parts.size >= 3) {
            val season = parts[parts.size - 2].toIntOrNull()
            val episode = parts[parts.size - 1].toIntOrNull()
            if (season != null && episode != null) {
                val episodeMatch = videosWithStreams.firstOrNull { it.season == season && it.episode == episode }
                if (episodeMatch != null) return episodeMatch.streams
            }
        }

        val prefixMatch = videosWithStreams.firstOrNull { it.id.startsWith("$videoId:") }
        if (prefixMatch != null) return prefixMatch.streams

        if (videoId == id && videosWithStreams.size == 1) {
            return videosWithStreams.first().streams
        }

        if (videoId == id && videosWithStreams.isNotEmpty()) {
            return videosWithStreams.flatMap { it.streams }
        }

        return emptyList()
    }
}

private fun MetaPreview.toFallbackMeta(): MetaDetails =
    MetaDetails(
        id = id,
        type = type,
        name = name,
        poster = poster,
        background = banner,
        logo = logo,
        description = description,
        releaseInfo = releaseInfo ?: rawReleaseDate?.take(4),
        imdbRating = imdbRating,
        genres = genres,
    )
