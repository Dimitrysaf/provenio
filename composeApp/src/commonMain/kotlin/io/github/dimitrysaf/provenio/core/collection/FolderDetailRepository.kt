package io.github.dimitrysaf.provenio.core.collection

import co.touchlab.kermit.Logger
import io.github.dimitrysaf.provenio.core.addons.AddonRepository
import io.github.dimitrysaf.provenio.core.catalog.CATALOG_PAGE_SIZE
import io.github.dimitrysaf.provenio.core.catalog.CatalogPage
import io.github.dimitrysaf.provenio.core.catalog.CatalogTarget
import io.github.dimitrysaf.provenio.core.catalog.fetchCatalogPage
import io.github.dimitrysaf.provenio.core.catalog.mergeCatalogItems
import io.github.dimitrysaf.provenio.core.catalog.nextCatalogPaginationState
import io.github.dimitrysaf.provenio.core.catalog.supportsPagination
import io.github.dimitrysaf.provenio.core.i18n.localizedMediaTypeLabel
import io.github.dimitrysaf.provenio.core.home.HomeCatalogSettingsRepository
import io.github.dimitrysaf.provenio.core.home.HomeCatalogSection
import io.github.dimitrysaf.provenio.core.home.MetaPreview
import io.github.dimitrysaf.provenio.core.home.filterReleasedItems
import io.github.dimitrysaf.provenio.core.home.stableKey
import io.github.dimitrysaf.provenio.core.tracking.trakt.TraktPublicListSourceResolver
import io.github.dimitrysaf.provenio.core.watch.progress.CurrentDateProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.collections_folder_addon_not_found
import provenio.composeapp.generated.resources.collections_folder_trakt_movie_list
import provenio.composeapp.generated.resources.collections_folder_trakt_series_list
import provenio.composeapp.generated.resources.collections_tab_all
import org.jetbrains.compose.resources.getString

object FolderDetailRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val log = Logger.withTag("FolderDetailRepository")

    private val _uiState = MutableStateFlow(FolderDetailUiState())
    val uiState: StateFlow<FolderDetailUiState> = _uiState.asStateFlow()

    private val loadJobs = mutableMapOf<Int, Job>()
    private var activeCollectionId: String? = null
    private var activeFolderId: String? = null

    fun initialize(collectionId: String, folderId: String) {
        val current = _uiState.value
        if (
            activeCollectionId == collectionId &&
            activeFolderId == folderId &&
            current.folder?.id == folderId &&
            current.tabs.isNotEmpty()
        ) {
            return
        }

        clear()
        activeCollectionId = collectionId
        activeFolderId = folderId

        val collection = CollectionRepository.getCollection(collectionId)
        if (collection == null) {
            _uiState.value = FolderDetailUiState(isLoading = false)
            return
        }

        val folder = collection.folders.find { it.id == folderId }
        if (folder == null) {
            _uiState.value = FolderDetailUiState(isLoading = false)
            return
        }

        val sources = folder.resolvedSources
        val showAll = collection.showAllTab && sources.size > 1
        val addons = AddonRepository.uiState.value.addons

        val tabs = buildList {
            if (showAll) {
                add(
                    FolderTab(
                        label = runBlocking { getString(Res.string.collections_tab_all) },
                        isAllTab = true,
                        isLoading = true,
                    ),
                )
            }
            sources.forEachIndexed { sourceIndex, source ->
                if (source.isTmdb) {
                    val mediaType = TmdbCollectionMediaType.fromString(source.mediaType)
                    val type = if (mediaType == TmdbCollectionMediaType.TV) "series" else "movie"
                    add(
                        FolderTab(
                            label = source.title?.takeIf { it.isNotBlank() } ?: "TMDB",
                            typeLabel = "TMDB",
                            source = source,
                            sourceKey = source.catalogRouteKey(),
                            type = type,
                            catalogId = tmdbCatalogId(source),
                            supportsPagination = source.tmdbSourceType !in setOf(
                                TmdbCollectionSourceType.COLLECTION.name,
                                TmdbCollectionSourceType.PERSON.name,
                                TmdbCollectionSourceType.DIRECTOR.name,
                            ),
                            isLoading = true,
                        ),
                    )
                } else if (source.isTrakt) {
                    val mediaType = TmdbCollectionMediaType.fromString(source.mediaType)
                    val type = if (mediaType == TmdbCollectionMediaType.TV) "series" else "movie"
                    val typeLabel = runBlocking {
                        getString(
                            if (mediaType == TmdbCollectionMediaType.TV) {
                                Res.string.collections_folder_trakt_series_list
                            } else {
                                Res.string.collections_folder_trakt_movie_list
                            },
                        )
                    }
                    add(
                        FolderTab(
                            label = source.title?.takeIf { it.isNotBlank() } ?: "Trakt",
                            typeLabel = typeLabel,
                            source = source,
                            sourceKey = source.catalogRouteKey(),
                            type = type,
                            catalogId = traktCatalogId(source),
                            supportsPagination = true,
                            isLoading = true,
                        ),
                    )
                } else {
                    val catalogSource = source.addonCatalogSource() ?: return@forEachIndexed
                    val resolvedCatalog = addons.findCollectionCatalog(catalogSource)
                    val addon = resolvedCatalog?.addon
                    val catalog = resolvedCatalog?.catalog
                    val label = catalog?.name ?: catalogSource.catalogId
                    val typeLabel = localizedMediaTypeLabel(catalogSource.type)
                    val genreSuffix = if (catalogSource.genre != null) " · ${catalogSource.genre}" else ""
                    add(
                        FolderTab(
                            label = "$label ($typeLabel)$genreSuffix",
                            typeLabel = typeLabel,
                            source = source,
                            sourceKey = source.catalogRouteKey(),
                            manifestUrl = addon?.manifestUrl,
                            type = catalogSource.type,
                            catalogId = catalogSource.catalogId,
                            genre = catalogSource.genre,
                            supportsPagination = catalog?.supportsPagination() == true,
                            isLoading = true,
                        ),
                    )
                }
            }
        }

        _uiState.value = FolderDetailUiState(
            folder = folder,
            collectionTitle = collection.title,
            viewMode = collection.folderViewMode,
            tabs = tabs,
            selectedTabIndex = 0,
            isLoading = true,
            showAllTab = showAll,
        )

        // Load catalog data for each source
        sources.forEachIndexed { sourceIndex, source ->
            val tabIndex = if (showAll) sourceIndex + 1 else sourceIndex
            val catalogSource = source.addonCatalogSource()
            val resolvedCatalog = catalogSource?.let { addons.findCollectionCatalog(it) }
            if (!source.isTmdb && !source.isTrakt && resolvedCatalog == null) {
                updateTab(tabIndex) {
                    it.copy(
                        isLoading = false,
                        error = runBlocking {
                            getString(Res.string.collections_folder_addon_not_found, catalogSource?.addonId.orEmpty())
                        },
                    )
                }
                return@forEachIndexed
            }

            loadTabPage(tabIndex, reset = true)
        }

        // If no sources, mark as done
        if (sources.isEmpty()) {
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun selectTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTabIndex = index)
    }

    fun clear() {
        loadJobs.values.forEach { it.cancel() }
        loadJobs.clear()
        activeCollectionId = null
        activeFolderId = null
        _uiState.value = FolderDetailUiState()
    }

    fun loadMoreSelectedTab() {
        val current = _uiState.value
        val selectedTab = current.selectedTab ?: return
        if (selectedTab.isAllTab) {
            current.tabs.forEachIndexed { index, tab ->
                if (!tab.isAllTab && tab.canLoadMore && !tab.isLoading && !tab.isLoadingMore) {
                    loadTabPage(index, reset = false)
                }
            }
            return
        }

        if (selectedTab.canLoadMore && !selectedTab.isLoading && !selectedTab.isLoadingMore) {
            loadTabPage(current.selectedTabIndex, reset = false)
        }
    }

    private fun updateTab(index: Int, transform: (FolderTab) -> FolderTab) {
        val current = _uiState.value
        val updatedTabs = current.tabs.toMutableList()
        if (index !in updatedTabs.indices) return
        updatedTabs[index] = transform(updatedTabs[index])

        val allDone = updatedTabs.none { !it.isAllTab && it.isLoading }
        _uiState.value = current.copy(
            tabs = updatedTabs,
            isLoading = !allDone,
        )
    }

    private fun loadTabPage(index: Int, reset: Boolean) {
        val currentTab = _uiState.value.tabs.getOrNull(index) ?: return
        val requestedSkip = if (reset) 0 else currentTab.nextSkip ?: return
        val currentSource = currentTab.source
        if (
            currentSource?.isTmdb != true &&
            currentSource?.isTrakt != true &&
            currentTab.manifestUrl == null
        ) return

        updateTab(index) { tab ->
            if (reset) {
                tab.copy(
                    items = emptyList(),
                    isLoading = true,
                    isLoadingMore = false,
                    nextSkip = null,
                    consecutiveDuplicatePages = 0,
                    error = null,
                )
            } else {
                tab.copy(
                    isLoadingMore = true,
                    error = null,
                )
            }
        }

        loadJobs.remove(index)?.cancel()
        val job = scope.launch {
            runCatching {
                val source = currentTab.source
                when {
                    source?.isTmdb == true -> TmdbCollectionSourceResolver.resolve(
                        source = source,
                        page = if (reset) 1 else requestedSkip,
                    )

                    source?.isTrakt == true -> TraktPublicListSourceResolver.resolve(
                        source = source,
                        page = if (reset) 1 else requestedSkip,
                    )

                    else -> fetchCatalogPage(
                        manifestUrl = requireNotNull(currentTab.manifestUrl),
                        type = currentTab.type,
                        catalogId = currentTab.catalogId,
                        genre = currentTab.genre,
                        skip = requestedSkip.takeIf { it > 0 },
                    )
                }.withUnreleasedFilter()
            }.onSuccess { page ->
                updateTab(index) { tab ->
                    val mergedItems = if (reset) {
                        page.items
                    } else {
                        mergeCatalogItems(tab.items, page.items)
                    }
                    val supportsPagination = tab.supportsPagination || page.rawItemCount >= CATALOG_PAGE_SIZE
                    val loadedNewItems = reset || mergedItems.size > tab.items.size
                    val paginationState = nextCatalogPaginationState(
                        supportsPagination = supportsPagination,
                        requestedSkip = requestedSkip,
                        page = page,
                        loadedNewItems = loadedNewItems,
                        consecutiveDuplicatePages = if (reset) 0 else tab.consecutiveDuplicatePages,
                    )
                    tab.copy(
                        items = mergedItems,
                        supportsPagination = supportsPagination,
                        isLoading = false,
                        isLoadingMore = false,
                        nextSkip = paginationState.nextSkip,
                        consecutiveDuplicatePages = paginationState.consecutiveDuplicatePages,
                        error = null,
                    )
                }
                rebuildAllTab()
            }.onFailure { error ->
                log.e(error) { "Failed to load source ${currentTab.catalogId}" }
                updateTab(index) { tab ->
                    tab.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        nextSkip = if (reset) null else tab.nextSkip,
                        error = error.message,
                    )
                }
                rebuildAllTab()
            }
        }
        loadJobs[index] = job
    }

    private fun rebuildAllTab() {
        val current = _uiState.value
        if (!current.showAllTab) return
        val sourceTabs = current.tabs.filter { !it.isAllTab }

        // Round-robin merge
        val merged = mutableListOf<MetaPreview>()
        val seenKeys = mutableSetOf<String>()
        val iterators = sourceTabs.map { it.items.iterator() }
        var hasMore = true
        while (hasMore) {
            hasMore = false
            for (iterator in iterators) {
                if (iterator.hasNext()) {
                    val item = iterator.next()
                    if (seenKeys.add(item.stableKey())) {
                        merged.add(item)
                    }
                    hasMore = true
                }
            }
        }

        val updatedTabs = current.tabs.toMutableList()
        val allTabIndex = updatedTabs.indexOfFirst { it.isAllTab }
        if (allTabIndex >= 0) {
            val hasInitialLoads = sourceTabs.any { it.isLoading }
            val hasLoadMore = sourceTabs.any { it.isLoadingMore }
            val errorMessage = sourceTabs.firstOrNull { it.error != null }?.error
            updatedTabs[allTabIndex] = updatedTabs[allTabIndex].copy(
                items = merged,
                isLoading = hasInitialLoads,
                isLoadingMore = hasLoadMore,
                error = errorMessage.takeIf { merged.isEmpty() },
            )
        }
        _uiState.value = current.copy(tabs = updatedTabs)
    }

    fun getCatalogSectionsForRows(): List<HomeCatalogSection> {
        val current = _uiState.value
        val folder = current.folder ?: return emptyList()
        val collectionId = activeCollectionId ?: return emptyList()

        return current.tabs.filter { !it.isAllTab && it.items.isNotEmpty() }.mapNotNull { tab ->
            val directSource = tab.source?.let { it.isTmdb || it.isTrakt } == true
            val target = if (directSource) {
                val sourceKey = tab.sourceKey ?: return@mapNotNull null
                CatalogTarget.CollectionSource(
                    collectionId = collectionId,
                    folderId = folder.id,
                    sourceKey = sourceKey,
                    contentType = tab.type,
                    supportsPagination = tab.supportsPagination,
                )
            } else {
                val manifestUrl = tab.manifestUrl ?: return@mapNotNull null
                CatalogTarget.Addon(
                    manifestUrl = manifestUrl,
                    contentType = tab.type,
                    catalogId = tab.catalogId,
                    genre = tab.genre,
                    supportsPagination = tab.supportsPagination,
                )
            }
            HomeCatalogSection(
                key = "folder_${folder.id}_${tab.label}",
                title = tab.label,
                subtitle = tab.typeLabel,
                addonName = "",
                target = target,
                items = tab.items,
                availableItemCount = tab.items.size,
                hasMore = tab.canLoadMore,
            )
        }
    }
}

private fun CatalogPage.withUnreleasedFilter(): CatalogPage {
    if (!HomeCatalogSettingsRepository.snapshot().hideUnreleasedContent) return this
    val filteredItems = items.filterReleasedItems(CurrentDateProvider.todayIsoDate())
    return if (filteredItems.size == items.size) this else copy(items = filteredItems)
}

private fun tmdbCatalogId(source: CollectionSource): String =
    buildString {
        append("tmdb_")
        append(source.tmdbSourceType?.lowercase().orEmpty())
        source.tmdbId?.let {
            append("_")
            append(it)
        }
        append("_")
        append(source.mediaType?.lowercase().orEmpty())
    }

private fun traktCatalogId(source: CollectionSource): String =
    listOf(
        "trakt",
        "list",
        source.traktListId?.toString().orEmpty(),
        source.mediaType?.lowercase().orEmpty(),
        TraktListSort.normalize(source.sortBy),
        TraktSortHow.normalize(source.sortHow),
    ).joinToString("_")
