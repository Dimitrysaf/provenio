package io.github.dimitrysaf.provenio.core.home

import io.github.dimitrysaf.provenio.core.addons.ManagedAddon
import io.github.dimitrysaf.provenio.core.addons.AddonRepository
import io.github.dimitrysaf.provenio.core.addons.enabledAddons
import io.github.dimitrysaf.provenio.core.catalog.CatalogTarget
import io.github.dimitrysaf.provenio.core.catalog.fetchCatalogPage
import io.github.dimitrysaf.provenio.core.collection.Collection
import io.github.dimitrysaf.provenio.core.collection.CollectionRepository
import io.github.dimitrysaf.provenio.core.collection.CollectionSource
import io.github.dimitrysaf.provenio.core.collection.TmdbCollectionSourceResolver
import io.github.dimitrysaf.provenio.core.collection.catalogRouteKey
import io.github.dimitrysaf.provenio.core.collection.findCollectionCatalog
import io.github.dimitrysaf.provenio.core.tracking.trakt.TraktPublicListSourceResolver
import io.github.dimitrysaf.provenio.core.watch.progress.CurrentDateProvider
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.getAndUpdate
import kotlinx.coroutines.CancellationException
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.absoluteValue
import kotlin.random.Random

object HomeRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var activeJob: Job? = null
    private var activeRequestKey: String? = null
    private var currentRequestKey: String? = null
    private var currentDefinitions: List<HomeCatalogDefinition> = emptyList()
    private var cachedSections: Map<String, HomeCatalogSection> = emptyMap()
    private var cachedCollectionHeroItems: List<MetaPreview> = emptyList()
    private var collectionHeroJob: Job? = null
    private var collectionHeroRequestKey: String? = null
    private var lastPublishedCatalogHeroEmpty: Boolean = true
    private var lastErrorMessage: String? = null
    private var attemptedCacheKeys: Set<String> = emptySet()
    private val requestedCacheKeys = atomic<Set<String>>(emptySet())
    private var sectionRequests: Job = SupervisorJob()
    private val sectionLoadLock = Mutex()

    /**
     * Loads the catalogs Home needs now: the first one that has titles, the hero's own sources, and
     * on a forced refresh every catalog already on screen. Every other catalog waits until Home
     * scrolls down to it and asks through [requestSection], one at a time in Home's order, so
     * catalogs nobody scrolls to are never fetched.
     */
    fun refresh(addons: List<ManagedAddon>, force: Boolean = false) {
        val activeAddons = addons.enabledAddons()
        val requests = buildHomeCatalogDefinitions(activeAddons)
        currentDefinitions = requests
        val requestCacheKeys = requests.mapTo(mutableSetOf(), HomeCatalogDefinition::cacheKey)
        cachedSections = cachedSections.filterKeys(requestCacheKeys::contains)
        attemptedCacheKeys = attemptedCacheKeys.filterTo(mutableSetOf(), requestCacheKeys::contains)
        val requestKey = requests.joinToString(separator = "|", transform = HomeCatalogDefinition::cacheKey)
        currentRequestKey = requestKey

        if (!force && activeRequestKey == requestKey && _uiState.value.isLoading) return
        activeRequestKey = requestKey

        sectionRequests.cancel()
        sectionRequests = SupervisorJob()
        requestedCacheKeys.value = emptySet()

        if (requests.isEmpty()) {
            activeJob?.cancel()
            activeJob = null
            activeRequestKey = null
            cachedSections = emptyMap()
            attemptedCacheKeys = emptySet()
            lastErrorMessage = null
            publishCurrentState(
                isLoading = false,
                requestKey = requestKey,
            )
            ensureCollectionHeroFallback(
                addons = activeAddons,
                forceRefresh = force,
                refreshSources = true,
                requestKey = requestKey,
            )
            return
        }

        activeJob?.cancel()
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        val reloadCacheKeys = if (force) attemptedCacheKeys else emptySet()
        activeJob = scope.launch {
            val snapshot = HomeCatalogSettingsRepository.snapshot()
            val ordered = prioritizeDefinitions(definitions = requests, snapshot = snapshot)
            val shown = ordered.filter { definition -> snapshot.preferences[definition.key]?.enabled != false }
            val heroOnly = if (snapshot.heroEnabled) {
                ordered.filter { definition ->
                    val preference = snapshot.preferences[definition.key]
                    preference?.enabled == false && preference.heroSourceEnabled
                }
            } else {
                emptyList()
            }
            lastErrorMessage = null

            // Already on screen, so a forced refresh fetches them again, in Home's order.
            shown.filter { it.cacheKey in reloadCacheKeys }.forEach { definition ->
                loadSection(definition, forceRefresh = true, requestKey = requestKey)
            }
            // The first catalog with titles; empty or failed ones before it are passed over.
            for (definition in shown) {
                if (_uiState.value.sections.isNotEmpty()) break
                if (definition.cacheKey in attemptedCacheKeys) continue
                loadSection(definition, forceRefresh = force, requestKey = requestKey)
            }
            // The hero is on screen from the start, so its own sources load now too.
            heroOnly.filter { force || it.cacheKey !in attemptedCacheKeys }.forEach { definition ->
                loadSection(definition, forceRefresh = force, requestKey = requestKey)
            }

            if (activeRequestKey != requestKey) return@launch
            activeRequestKey = null
            publishCurrentState(
                isLoading = false,
                requestKey = requestKey,
            )
            ensureCollectionHeroFallback(
                addons = activeAddons,
                forceRefresh = force,
                refreshSources = true,
                requestKey = requestKey,
            )
        }
    }

    /**
     * Home has scrolled down to the catalog with [key] and shows its placeholder: fetch it now.
     * Requests queue behind each other, so catalogs load one at a time in the order they come.
     */
    fun requestSection(key: String) {
        val definition = currentDefinitions.firstOrNull { it.key == key } ?: return
        val cacheKey = definition.cacheKey
        if (cacheKey in attemptedCacheKeys) return
        val previous = requestedCacheKeys.getAndUpdate { it + cacheKey }
        if (cacheKey in previous) return
        val requestKey = currentRequestKey
        scope.launch(sectionRequests) {
            loadSection(definition, forceRefresh = false, requestKey = requestKey)
        }
    }

    /** Fetches one catalog and publishes it; loads never overlap, so they finish in request order. */
    private suspend fun loadSection(
        definition: HomeCatalogDefinition,
        forceRefresh: Boolean,
        requestKey: String?,
    ) {
        sectionLoadLock.withLock {
            if (currentRequestKey != requestKey) return
            if (!forceRefresh && definition.cacheKey in attemptedCacheKeys) return
            val result = runCatching { definition.toSection(forceRefresh = forceRefresh) }
            result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
            if (currentRequestKey != requestKey) return
            result.getOrNull()?.let { section -> cachedSections = cachedSections + (definition.cacheKey to section) }
            attemptedCacheKeys = attemptedCacheKeys + definition.cacheKey
            if (lastErrorMessage == null) lastErrorMessage = result.exceptionOrNull()?.message
            publishCurrentState(
                isLoading = _uiState.value.isLoading,
                requestKey = requestKey,
            )
        }
    }

    fun applyCurrentSettings() {
        publishCurrentState(
            isLoading = _uiState.value.isLoading,
            requestKey = currentRequestKey,
        )
        ensureCollectionHeroFallback(
            addons = AddonRepository.uiState.value.addons.enabledAddons(),
            forceRefresh = false,
            refreshSources = false,
            requestKey = currentRequestKey,
        )
    }

    fun clear() {
        activeJob?.cancel()
        activeJob = null
        sectionRequests.cancel()
        sectionRequests = SupervisorJob()
        requestedCacheKeys.value = emptySet()
        attemptedCacheKeys = emptySet()
        activeRequestKey = null
        currentRequestKey = null
        currentDefinitions = emptyList()
        cachedSections = emptyMap()
        cachedCollectionHeroItems = emptyList()
        collectionHeroJob?.cancel()
        collectionHeroJob = null
        collectionHeroRequestKey = null
        lastPublishedCatalogHeroEmpty = true
        lastErrorMessage = null
        _uiState.value = HomeUiState()
    }

    private fun publishCurrentState(
        isLoading: Boolean,
        requestKey: String?,
    ) {
        val snapshot = HomeCatalogSettingsRepository.snapshot()
        val preferences = snapshot.preferences
        val todayIsoDate = if (snapshot.hideUnreleasedContent) CurrentDateProvider.todayIsoDate() else null
        fun HomeCatalogSection.withReleaseFilter(): HomeCatalogSection =
            if (todayIsoDate == null) this else filterReleasedItems(todayIsoDate)

        val sections = currentDefinitions
            .sortedBy { definition -> preferences[definition.key]?.order ?: Int.MAX_VALUE }
            .mapNotNull { definition ->
                val preference = preferences[definition.key]
                if (preference?.enabled == false) return@mapNotNull null

                val section = cachedSections[definition.cacheKey]?.withReleaseFilter() ?: return@mapNotNull null
                if (section.items.isEmpty()) return@mapNotNull null
                val customTitle = preference?.customTitle.orEmpty()
                section.copy(
                    title = customTitle.ifBlank { definition.titleFor(snapshot.showCatalogType) },
                )
            }

        val catalogHeroItems = if (snapshot.heroEnabled) {
            val heroRandom = Random((requestKey?.hashCode() ?: 0).absoluteValue + 1)
            currentDefinitions
                .filter { definition -> preferences[definition.key]?.heroSourceEnabled != false }
                .mapNotNull { definition -> cachedSections[definition.cacheKey] }
                .map { section -> section.withReleaseFilter() }
                .flatMap { section -> section.items }
                .distinctBy { item -> "${item.type}:${item.id}" }
                .shuffled(heroRandom)
                .take(HOME_HERO_ITEM_LIMIT)
        } else {
            emptyList()
        }
        lastPublishedCatalogHeroEmpty = snapshot.heroEnabled && catalogHeroItems.isEmpty()
        val heroItems = if (snapshot.heroEnabled) {
            catalogHeroItems.ifEmpty { cachedCollectionHeroItems }
        } else {
            emptyList()
        }

        val definitionsByCacheKey = currentDefinitions.associateBy(HomeCatalogDefinition::cacheKey)
        _uiState.value = HomeUiState(
            isLoading = isLoading,
            heroItems = heroItems,
            sections = sections,
            errorMessage = if (sections.isEmpty()) lastErrorMessage else null,
            catalogKeys = currentDefinitions.mapTo(mutableSetOf(), HomeCatalogDefinition::key),
            attemptedCatalogKeys = attemptedCacheKeys.mapNotNullTo(mutableSetOf()) { cacheKey ->
                definitionsByCacheKey[cacheKey]?.key
            },
        )
    }

    private suspend fun HomeCatalogDefinition.toSection(forceRefresh: Boolean): HomeCatalogSection {
        val page = fetchCatalogPage(
            manifestUrl = manifestUrl,
            type = type,
            catalogId = catalogId,
            maxItems = HOME_CATALOG_PREVIEW_FETCH_LIMIT,
            forceRefresh = forceRefresh,
        )
        val items = page.items
        if (items.isEmpty()) {
            return HomeCatalogSection(
                key = key,
                title = defaultTitle,
                subtitle = addonName,
                addonName = addonName,
                target = CatalogTarget.Addon(
                    manifestUrl = manifestUrl,
                    contentType = type,
                    catalogId = catalogId,
                    supportsPagination = supportsPagination,
                ),
                items = emptyList(),
                availableItemCount = 0,
                hasMore = false,
            )
        }

        return HomeCatalogSection(
            key = key,
            title = defaultTitle,
            subtitle = addonName,
            addonName = addonName,
            target = CatalogTarget.Addon(
                manifestUrl = manifestUrl,
                contentType = type,
                catalogId = catalogId,
                supportsPagination = supportsPagination,
            ),
            items = items,
            availableItemCount = page.rawItemCount,
            hasMore = supportsPagination && page.nextSkip != null,
        )
    }

    private fun ensureCollectionHeroFallback(
        addons: List<ManagedAddon>,
        forceRefresh: Boolean,
        refreshSources: Boolean,
        requestKey: String?,
    ) {
        if (!lastPublishedCatalogHeroEmpty) return
        val snapshot = HomeCatalogSettingsRepository.snapshot()
        if (!snapshot.heroEnabled) return
        val collections = enabledCollectionsForHero(snapshot)
        if (collections.isEmpty()) {
            cachedCollectionHeroItems = emptyList()
            collectionHeroRequestKey = null
            return
        }

        val nextRequestKey = collectionHeroRequestKey(
            collections = collections,
            addons = addons,
            snapshot = snapshot,
            requestKey = requestKey,
        )
        if (!refreshSources && collectionHeroRequestKey == nextRequestKey) return

        collectionHeroJob?.cancel()
        collectionHeroRequestKey = nextRequestKey
        cachedCollectionHeroItems = emptyList()
        publishCurrentState(
            isLoading = _uiState.value.isLoading,
            requestKey = requestKey,
        )

        collectionHeroJob = scope.launch {
            val sources = collectionHeroSources(collections)
            val sourceResults = sources.map { source ->
                async {
                    runCatching {
                        source.resolveCollectionHeroItems(
                            addons = addons,
                            forceRefresh = forceRefresh,
                        )
                    }.getOrDefault(emptyList())
                }
            }.awaitAll()
            val random = Random((nextRequestKey.hashCode()).absoluteValue + 7)
            cachedCollectionHeroItems = roundRobinCollectionHeroItems(sourceResults)
                .distinctBy { item -> item.stableKey() }
                .shuffled(random)
                .take(HOME_HERO_ITEM_LIMIT)
            publishCurrentState(
                isLoading = _uiState.value.isLoading,
                requestKey = requestKey,
            )
        }
    }

    private fun enabledCollectionsForHero(snapshot: HomeCatalogSettingsSnapshot): List<Collection> {
        val preferences = snapshot.preferences
        return CollectionRepository.collections.value
            .filter { collection ->
                collection.folders.isNotEmpty() &&
                    preferences["collection_${collection.id}"]?.enabled != false
            }
            .sortedBy { collection ->
                preferences["collection_${collection.id}"]?.order ?: Int.MAX_VALUE
            }
    }

    private fun collectionHeroSources(collections: List<Collection>): List<CollectionSource> =
        collections
            .flatMap { collection -> collection.folders }
            .flatMap { folder -> folder.resolvedSources }
            .take(HOME_COLLECTION_HERO_SOURCE_LIMIT)

    private suspend fun CollectionSource.resolveCollectionHeroItems(
        addons: List<ManagedAddon>,
        forceRefresh: Boolean,
    ): List<MetaPreview> {
        val page = when {
            isTmdb -> TmdbCollectionSourceResolver.resolve(source = this, page = 1)
            isTrakt -> TraktPublicListSourceResolver.resolve(source = this, page = 1)
            else -> {
                val catalogSource = addonCatalogSource() ?: return emptyList()
                val resolvedCatalog = addons.findCollectionCatalog(catalogSource) ?: return emptyList()
                fetchCatalogPage(
                    manifestUrl = resolvedCatalog.addon.manifestUrl,
                    type = catalogSource.type,
                    catalogId = catalogSource.catalogId,
                    genre = catalogSource.genre,
                    maxItems = HOME_COLLECTION_HERO_SOURCE_ITEM_LIMIT,
                    forceRefresh = forceRefresh,
                )
            }
        }
        val items = page.items
        return if (HomeCatalogSettingsRepository.snapshot().hideUnreleasedContent) {
            items.filterReleasedItems(CurrentDateProvider.todayIsoDate())
        } else {
            items
        }
    }

    private fun roundRobinCollectionHeroItems(sourceResults: List<List<MetaPreview>>): List<MetaPreview> {
        val iterators = sourceResults.filter { it.isNotEmpty() }.map { it.iterator() }
        if (iterators.isEmpty()) return emptyList()
        val merged = mutableListOf<MetaPreview>()
        var hasMore = true
        while (hasMore && merged.size < HOME_COLLECTION_HERO_SOURCE_LIMIT * HOME_COLLECTION_HERO_SOURCE_ITEM_LIMIT) {
            hasMore = false
            iterators.forEach { iterator ->
                if (iterator.hasNext()) {
                    merged.add(iterator.next())
                    hasMore = true
                }
            }
        }
        return merged
    }

    private fun collectionHeroRequestKey(
        collections: List<Collection>,
        addons: List<ManagedAddon>,
        snapshot: HomeCatalogSettingsSnapshot,
        requestKey: String?,
    ): String = buildString {
        append(requestKey.orEmpty())
        append("|hideUnreleased=")
        append(snapshot.hideUnreleasedContent)
        append("|collections=")
        collections.forEach { collection ->
            val preference = snapshot.preferences["collection_${collection.id}"]
            append(collection.id)
            append(":")
            append(preference?.order ?: Int.MAX_VALUE)
            append(":")
            collection.folders.forEach { folder ->
                append(folder.id)
                append("[")
                folder.resolvedSources.forEach { source ->
                    append(collectionSourceKey(source))
                    append(",")
                }
                append("]")
            }
            append(";")
        }
        append("|addons=")
        addons.forEach { addon ->
            append(addon.manifest?.id.orEmpty())
            append(":")
            append(addon.manifestUrl)
            append(":")
            append(addon.manifest?.catalogs?.size ?: 0)
            append(";")
        }
    }

    private fun collectionSourceKey(source: CollectionSource): String =
        source.catalogRouteKey()
}

private const val HOME_HERO_ITEM_LIMIT = 8

private const val HOME_COLLECTION_HERO_SOURCE_LIMIT = 6

private const val HOME_COLLECTION_HERO_SOURCE_ITEM_LIMIT = 8

private const val HOME_CATALOG_PREVIEW_FETCH_LIMIT = 18


private fun prioritizeDefinitions(
    definitions: List<HomeCatalogDefinition>,
    snapshot: HomeCatalogSettingsSnapshot,
): List<HomeCatalogDefinition> {
    val orderedDefinitions = definitions.sortedBy { definition ->
        snapshot.preferences[definition.key]?.order ?: Int.MAX_VALUE
    }
    val (priority, remainder) = orderedDefinitions.partition { definition ->
        val preference = snapshot.preferences[definition.key]
        if (preference == null) {
            true
        } else {
            preference.enabled || (snapshot.heroEnabled && preference.heroSourceEnabled)
        }
    }
    return priority + remainder
}
