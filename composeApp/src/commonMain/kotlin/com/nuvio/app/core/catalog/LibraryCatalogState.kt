package com.nuvio.app.core.catalog

import com.nuvio.app.core.library.LibraryUiState
import com.nuvio.app.core.library.sortLibraryItems
import com.nuvio.app.core.library.toMetaPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal fun Flow<LibraryUiState>.libraryCatalogStates(
    target: CatalogTarget.Library,
): Flow<CatalogUiState> =
    map { libraryState ->
        val items = libraryState.sections
            .firstOrNull { it.type == target.sectionType }
            ?.items
            .orEmpty()
        CatalogUiState(
            items = sortLibraryItems(
                items = items,
                selected = target.sortOption,
                sourceMode = libraryState.sourceMode,
            ).map { it.toMetaPreview() }.let(::dedupeCatalogItems),
            isLoading = libraryState.isLoading,
            errorMessage = libraryState.errorMessage,
        )
    }.distinctUntilChanged()
