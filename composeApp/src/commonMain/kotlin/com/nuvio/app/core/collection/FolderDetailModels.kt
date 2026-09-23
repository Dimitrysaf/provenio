package com.nuvio.app.core.collection

import com.nuvio.app.core.catalog.supportsPagination
import com.nuvio.app.core.home.MetaPreview

data class FolderTab(
    val label: String,
    val typeLabel: String = "",
    val source: CollectionSource? = null,
    val sourceKey: String? = null,
    val manifestUrl: String? = null,
    val type: String = "",
    val catalogId: String = "",
    val genre: String? = null,
    val supportsPagination: Boolean = false,
    val items: List<MetaPreview> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val nextSkip: Int? = null,
    val consecutiveDuplicatePages: Int = 0,
    val error: String? = null,
    val isAllTab: Boolean = false,
) {
    val canLoadMore: Boolean
        get() = supportsPagination && nextSkip != null
}

data class FolderDetailUiState(
    val folder: CollectionFolder? = null,
    val collectionTitle: String = "",
    val viewMode: FolderViewMode = FolderViewMode.TABBED_GRID,
    val tabs: List<FolderTab> = emptyList(),
    val selectedTabIndex: Int = 0,
    val isLoading: Boolean = true,
    val showAllTab: Boolean = true,
) {
    val selectedTab: FolderTab?
        get() = tabs.getOrNull(selectedTabIndex)

    val selectedTabCanLoadMore: Boolean
        get() {
            val currentTab = selectedTab ?: return false
            return if (currentTab.isAllTab) {
                tabs.any { !it.isAllTab && it.canLoadMore }
            } else {
                currentTab.canLoadMore
            }
        }

    val selectedTabIsLoadingMore: Boolean
        get() {
            val currentTab = selectedTab ?: return false
            return if (currentTab.isAllTab) {
                tabs.any { !it.isAllTab && it.isLoadingMore }
            } else {
                currentTab.isLoadingMore
            }
        }
}

private fun Boolean?.orFalse(): Boolean = this == true
