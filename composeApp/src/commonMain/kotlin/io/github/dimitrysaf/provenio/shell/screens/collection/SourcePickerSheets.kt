package io.github.dimitrysaf.provenio.shell.screens.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.core.collection.CollectionEditorRepository
import io.github.dimitrysaf.provenio.core.collection.CollectionEditorUiState
import io.github.dimitrysaf.provenio.core.collection.CollectionSource
import io.github.dimitrysaf.provenio.core.collection.TmdbBuilderMode
import io.github.dimitrysaf.provenio.core.collection.TmdbCollectionMediaType
import io.github.dimitrysaf.provenio.core.collection.TmdbCollectionSourceResolver
import io.github.dimitrysaf.provenio.core.collection.TmdbCollectionSourceType
import io.github.dimitrysaf.provenio.core.collection.TraktSortHow
import io.github.dimitrysaf.provenio.core.collection.collectionSourceKey
import io.github.dimitrysaf.provenio.core.tracking.trakt.TraktPublicListSearchResult
import io.github.dimitrysaf.provenio.shell.components.ExpandableListHeader
import io.github.dimitrysaf.provenio.shell.components.ModalSheet
import io.github.dimitrysaf.provenio.shell.components.SearchField
import io.github.dimitrysaf.provenio.shell.components.SelectableListRow
import io.github.dimitrysaf.provenio.shell.components.SheetNavigation
import io.github.dimitrysaf.provenio.shell.components.SheetNavigationButton
import io.github.dimitrysaf.provenio.shell.components.SingleChoiceBottomSheet
import io.github.dimitrysaf.provenio.shell.components.SingleChoiceOption
import io.github.dimitrysaf.provenio.shell.components.safeBottomPadding
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.*

/*
 * The TMDB, Trakt and Simkl source pickers, built as the catalog picker is: a sheet over the
 * folder being edited, each kind of source a collapsible segmented group, and every source a
 * checkbox row that adds it or takes it away again. The sheet stays open, so several can be
 * picked, and says how many of its kind the folder already has.
 */

// ---- TMDB -------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun TmdbSourceSheet(state: CollectionEditorUiState, onDismiss: () -> Unit) {
    val tmdbSources = state.editingFolder?.resolvedSources.orEmpty().filter { it.isTmdb }
    val tmdbKeys = tmdbSources.mapTo(mutableSetOf(), ::collectionSourceKey)
    val presets = remember { TmdbCollectionSourceResolver.presets() }
    val presetKeys = remember(presets) { presets.mapTo(mutableSetOf()) { collectionSourceKey(it.source) } }
    // One kind open at a time: the sheet's fields belong to whichever is open.
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }
    val labels = TmdbBuilderMode.entries.associateWith { tmdbBuilderModeLabel(it) }

    fun addedOf(mode: TmdbBuilderMode): List<CollectionSource> = when (mode) {
        TmdbBuilderMode.PRESETS -> tmdbSources.filter { collectionSourceKey(it) in presetKeys }
        TmdbBuilderMode.DISCOVER -> tmdbSources.filter {
            it.tmdbSourceType == TmdbCollectionSourceType.DISCOVER.name && collectionSourceKey(it) !in presetKeys
        }
        else -> tmdbSources.filter {
            it.tmdbSourceType == mode.sourceType().name && collectionSourceKey(it) !in presetKeys
        }
    }

    SourceSheet(
        title = stringResource(Res.string.collections_editor_tmdb_sources),
        selectedCount = tmdbSources.size,
        onDismiss = onDismiss,
    ) {
        TmdbBuilderMode.entries.forEach { mode ->
            val open = expanded == mode.name
            sourceGroup(
                key = "tmdb-${mode.name}",
                title = labels.getValue(mode),
                expanded = open,
                count = addedOf(mode).size,
                onToggle = {
                    if (open) {
                        expanded = null
                    } else {
                        expanded = mode.name
                        CollectionEditorRepository.setTmdbBuilderMode(mode)
                    }
                },
                body = {
                    when (mode) {
                        TmdbBuilderMode.PRESETS -> presets.forEach { preset ->
                            val key = collectionSourceKey(preset.source)
                            val selected = key in tmdbKeys
                            row { shape ->
                                SourceCheckRow(
                                    selected = selected,
                                    title = preset.label,
                                    subtitle = tmdbSourceSubtitle(preset.source),
                                    shape = shape,
                                    onToggle = {
                                        if (selected) {
                                            CollectionEditorRepository.removeSources { collectionSourceKey(it) == key }
                                        } else {
                                            CollectionEditorRepository.addTmdbPreset(preset.source)
                                        }
                                    },
                                )
                            }
                        }

                        TmdbBuilderMode.PRODUCTION, TmdbBuilderMode.COLLECTION -> {
                            row { shape ->
                                FormFieldRow(shape) {
                                    SearchField(
                                        query = state.tmdbInput,
                                        onQueryChange = { CollectionEditorRepository.setTmdbInput(it) },
                                        placeholder = tmdbInputPlaceholder(mode),
                                        onSearch = {
                                            if (mode == TmdbBuilderMode.PRODUCTION) {
                                                CollectionEditorRepository.searchTmdbCompanies()
                                            } else {
                                                CollectionEditorRepository.searchTmdbCollections()
                                            }
                                        },
                                    )
                                    if (mode == TmdbBuilderMode.PRODUCTION) {
                                        TmdbOptionChips(state = state, showMedia = true, showSort = true)
                                    }
                                }
                            }
                            state.tmdbSearchError?.let { error -> row { shape -> SourceMessageRow(error, shape) } }
                            if (mode == TmdbBuilderMode.PRODUCTION) {
                                tmdbCompanyRows(state, tmdbSources)
                            } else {
                                tmdbCollectionRows(state, tmdbSources)
                            }
                        }

                        else -> {
                            row { shape ->
                                FormFieldRow(shape) {
                                    if (mode != TmdbBuilderMode.DISCOVER) {
                                        OutlinedTextField(
                                            value = state.tmdbInput,
                                            onValueChange = { CollectionEditorRepository.setTmdbInput(it) },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            label = { Text(tmdbInputLabel(mode)) },
                                            placeholder = { Text(tmdbInputPlaceholder(mode)) },
                                            supportingText = tmdbInputHelper(mode).takeIf { it.isNotBlank() }?.let { { Text(it) } },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                                        )
                                    }
                                    SourceTitleField(
                                        value = state.tmdbTitleInput,
                                        onValueChange = { CollectionEditorRepository.setTmdbTitleInput(it) },
                                        placeholder = tmdbTitlePlaceholder(mode),
                                    )
                                    val showMedia = mode in setOf(TmdbBuilderMode.PERSON, TmdbBuilderMode.DIRECTOR, TmdbBuilderMode.DISCOVER)
                                    val showSort = mode != TmdbBuilderMode.LIST
                                    if (showMedia || showSort) TmdbOptionChips(state = state, showMedia = showMedia, showSort = showSort)
                                    if (mode == TmdbBuilderMode.DISCOVER) {
                                        Text(
                                            text = stringResource(Res.string.collections_editor_tmdb_filters_helper),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                            state.tmdbSearchError?.let { error -> row { shape -> SourceMessageRow(error, shape) } }
                            row { shape ->
                                SourceAddButtonRow(
                                    shape = shape,
                                    enabled = mode == TmdbBuilderMode.DISCOVER || state.tmdbInput.isNotBlank(),
                                    onClick = { CollectionEditorRepository.addTmdbSourceFromInput() },
                                )
                            }
                            addedOf(mode).forEach { source -> addedSourceRow(source) { tmdbSourceSubtitle(source) } }
                        }
                    }
                },
                // The custom discover filters are a segmented group of their own, so they follow the
                // open group rather than sitting inside it.
                after = if (open && mode == TmdbBuilderMode.DISCOVER) {
                    { TmdbDiscoverFilters(state = state) }
                } else {
                    null
                },
            )
        }
    }
}

private fun TmdbBuilderMode.sourceType(): TmdbCollectionSourceType = when (this) {
    TmdbBuilderMode.PRESETS, TmdbBuilderMode.DISCOVER -> TmdbCollectionSourceType.DISCOVER
    TmdbBuilderMode.LIST -> TmdbCollectionSourceType.LIST
    TmdbBuilderMode.COLLECTION -> TmdbCollectionSourceType.COLLECTION
    TmdbBuilderMode.PRODUCTION -> TmdbCollectionSourceType.COMPANY
    TmdbBuilderMode.NETWORK -> TmdbCollectionSourceType.NETWORK
    TmdbBuilderMode.PERSON -> TmdbCollectionSourceType.PERSON
    TmdbBuilderMode.DIRECTOR -> TmdbCollectionSourceType.DIRECTOR
}

@Composable
private fun CollectionEditorGroupScope.tmdbCompanyRows(state: CollectionEditorUiState, tmdbSources: List<CollectionSource>) {
    val movieSuffix = stringResource(Res.string.collections_editor_tmdb_movies)
    val seriesSuffix = stringResource(Res.string.collections_editor_tmdb_series)
    state.tmdbCompanyResults.forEach { result ->
        val title = result.name ?: stringResource(Res.string.collections_editor_tmdb_company_fallback, result.id)
        val matches: (CollectionSource) -> Boolean = {
            it.isTmdb && it.tmdbSourceType == TmdbCollectionSourceType.COMPANY.name && it.tmdbId == result.id
        }
        val selected = tmdbSources.any(matches)
        val subtitle = listOfNotNull(
            stringResource(Res.string.collections_editor_tmdb_subtitle_production),
            result.originCountry,
        ).joinToString(" • ")
        row { shape ->
            SourceCheckRow(
                selected = selected,
                title = title,
                subtitle = subtitle,
                shape = shape,
                onToggle = {
                    if (selected) {
                        CollectionEditorRepository.removeSources(matches)
                    } else {
                        CollectionEditorRepository.addTmdbSourcesFromPicker(
                            tmdbSelectedMediaTypes(state).map { mediaType ->
                                CollectionSource(
                                    provider = "tmdb",
                                    tmdbSourceType = TmdbCollectionSourceType.COMPANY.name,
                                    title = tmdbTitleForMedia(title, mediaType, state.tmdbMediaBoth, movieSuffix, seriesSuffix),
                                    tmdbId = result.id,
                                    mediaType = mediaType.name,
                                    sortBy = state.tmdbSortBy,
                                    filters = state.tmdbFilters,
                                )
                            },
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun CollectionEditorGroupScope.tmdbCollectionRows(state: CollectionEditorUiState, tmdbSources: List<CollectionSource>) {
    val subtitle = stringResource(Res.string.collections_editor_tmdb_collection)
    state.tmdbCollectionResults.forEach { result ->
        val title = result.name ?: stringResource(Res.string.collections_editor_tmdb_collection_fallback, result.id)
        val matches: (CollectionSource) -> Boolean = {
            it.isTmdb && it.tmdbSourceType == TmdbCollectionSourceType.COLLECTION.name && it.tmdbId == result.id
        }
        val selected = tmdbSources.any(matches)
        row { shape ->
            SourceCheckRow(
                selected = selected,
                title = title,
                subtitle = subtitle,
                shape = shape,
                onToggle = {
                    if (selected) {
                        CollectionEditorRepository.removeSources(matches)
                    } else {
                        CollectionEditorRepository.addTmdbSource(
                            CollectionSource(
                                provider = "tmdb",
                                tmdbSourceType = TmdbCollectionSourceType.COLLECTION.name,
                                title = title,
                                tmdbId = result.id,
                                mediaType = TmdbCollectionMediaType.MOVIE.name,
                                sortBy = state.tmdbSortBy,
                            ),
                        )
                    }
                },
            )
        }
    }
}

/** Movies, series or both as filter chips, and the sort as a chip that opens its choices. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TmdbOptionChips(state: CollectionEditorUiState, showMedia: Boolean, showSort: Boolean) {
    val mediaOptions = tmdbMediaChoices()
    val currentMedia = tmdbCurrentMediaChoice(state)
    val sortOptions = tmdbSortChoices(state)
    var showSortSheet by remember { mutableStateOf(false) }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showMedia) {
            mediaOptions.forEach { option ->
                FilterChip(
                    selected = option.value == currentMedia,
                    onClick = {
                        CollectionEditorRepository.setTmdbMediaBoth(option.value == TmdbMediaChoice.BOTH)
                        when (option.value) {
                            TmdbMediaChoice.MOVIE -> CollectionEditorRepository.setTmdbMediaType(TmdbCollectionMediaType.MOVIE)
                            TmdbMediaChoice.TV -> CollectionEditorRepository.setTmdbMediaType(TmdbCollectionMediaType.TV)
                            TmdbMediaChoice.BOTH -> Unit
                        }
                    },
                    label = { Text(option.label) },
                )
            }
        }
        if (showSort) {
            DropdownChip(
                label = sortOptions.firstOrNull { it.value == state.tmdbSortBy }?.label ?: sortOptions.first().label,
                onClick = { showSortSheet = true },
            )
        }
    }
    if (showSortSheet) {
        SingleChoiceBottomSheet(
            title = stringResource(Res.string.collections_editor_tmdb_sort),
            options = sortOptions,
            isSelected = { it == state.tmdbSortBy },
            onSelected = { CollectionEditorRepository.setTmdbSortBy(it) },
            onDismiss = { showSortSheet = false },
        )
    }
}

// ---- Trakt ------------------------------------------------------------------------------------

@Composable
internal fun TraktSourceSheet(state: CollectionEditorUiState, onDismiss: () -> Unit) {
    val traktSources = state.editingFolder?.resolvedSources.orEmpty().filter { it.isTrakt }
    val editing = state.editingTraktSourceIndex != null
    var expanded by rememberSaveable { mutableStateOf(if (editing) "search" else "trending") }
    val searchTitle = stringResource(Res.string.collections_editor_trakt_search_results)
    val trendingTitle = stringResource(Res.string.collections_editor_trakt_trending)
    val popularTitle = stringResource(Res.string.collections_editor_trakt_popular)

    SourceSheet(
        title = stringResource(
            if (editing) Res.string.collections_editor_edit_trakt_source else Res.string.collections_editor_trakt_sources,
        ),
        selectedCount = traktSources.size,
        onDismiss = onDismiss,
    ) {
        // How a list is read applies to every list picked below, so it leads the sheet.
        item(key = "trakt-options") {
            CollectionEditorGroup {
                row { shape -> FormFieldRow(shape) { TraktOptionChips(state) } }
            }
        }
        traktGroup("search", searchTitle, expanded, { expanded = it }) {
            row { shape ->
                FormFieldRow(shape) {
                    SearchField(
                        query = state.traktInput,
                        onQueryChange = { CollectionEditorRepository.setTraktInput(it) },
                        placeholder = stringResource(Res.string.collections_editor_trakt_input_placeholder),
                        onSearch = { CollectionEditorRepository.searchTraktLists() },
                    )
                    SourceTitleField(
                        value = state.traktTitleInput,
                        onValueChange = { CollectionEditorRepository.setTraktTitleInput(it) },
                        placeholder = stringResource(Res.string.collections_editor_trakt_title_placeholder),
                    )
                }
            }
            state.traktSearchError?.let { error -> row { shape -> SourceMessageRow(error, shape) } }
            traktResultRows(state.traktSearchResults, traktSources)
        }
        if (!editing) {
            traktGroup("trending", trendingTitle, expanded, { expanded = it }) {
                traktResultRows(state.traktTrendingResults, traktSources)
            }
            traktGroup("popular", popularTitle, expanded, { expanded = it }) {
                traktResultRows(state.traktPopularResults, traktSources)
            }
        }
    }
}

private fun LazyListScope.traktGroup(
    key: String,
    title: String,
    expanded: String?,
    onExpand: (String?) -> Unit,
    body: @Composable CollectionEditorGroupScope.() -> Unit,
) {
    val open = expanded == key
    sourceGroup(
        key = "trakt-$key",
        title = title,
        expanded = open,
        count = null,
        onToggle = { onExpand(if (open) null else key) },
        body = body,
    )
}

@Composable
private fun CollectionEditorGroupScope.traktResultRows(
    results: List<TraktPublicListSearchResult>,
    traktSources: List<CollectionSource>,
) {
    results.forEach { result ->
        val matches: (CollectionSource) -> Boolean = { it.isTrakt && it.traktListId == result.traktListId }
        val selected = traktSources.any(matches)
        row { shape ->
            SourceCheckRow(
                selected = selected,
                title = result.title,
                subtitle = result.subtitle,
                shape = shape,
                onToggle = {
                    if (selected) {
                        CollectionEditorRepository.removeSources(matches)
                    } else {
                        CollectionEditorRepository.addTraktSourceFromResult(result)
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TraktOptionChips(state: CollectionEditorUiState) {
    val mediaOptions = tmdbMediaChoices()
    val currentMedia = when {
        state.traktMediaBoth -> TmdbMediaChoice.BOTH
        state.traktMediaType == TmdbCollectionMediaType.TV -> TmdbMediaChoice.TV
        else -> TmdbMediaChoice.MOVIE
    }
    val sortOptions = traktSortOptions().map { (value, label) -> SingleChoiceOption(value = value, label = label) }
    val ascending = stringResource(Res.string.collections_editor_trakt_ascending)
    val descending = stringResource(Res.string.collections_editor_trakt_descending)
    var showSortSheet by remember { mutableStateOf(false) }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        mediaOptions.forEach { option ->
            FilterChip(
                selected = option.value == currentMedia,
                onClick = {
                    CollectionEditorRepository.setTraktMediaBoth(option.value == TmdbMediaChoice.BOTH)
                    when (option.value) {
                        TmdbMediaChoice.MOVIE -> CollectionEditorRepository.setTraktMediaType(TmdbCollectionMediaType.MOVIE)
                        TmdbMediaChoice.TV -> CollectionEditorRepository.setTraktMediaType(TmdbCollectionMediaType.TV)
                        TmdbMediaChoice.BOTH -> Unit
                    }
                },
                label = { Text(option.label) },
            )
        }
        DropdownChip(
            label = sortOptions.firstOrNull { it.value == state.traktSortBy }?.label ?: sortOptions.first().label,
            onClick = { showSortSheet = true },
        )
        // Two directions, so one chip that flips between them.
        FilterChip(
            selected = false,
            onClick = {
                CollectionEditorRepository.setTraktSortHow(
                    if (state.traktSortHow == TraktSortHow.DESC.value) TraktSortHow.ASC.value else TraktSortHow.DESC.value,
                )
            },
            label = { Text(if (state.traktSortHow == TraktSortHow.DESC.value) descending else ascending) },
        )
    }
    if (showSortSheet) {
        SingleChoiceBottomSheet(
            title = stringResource(Res.string.collections_editor_tmdb_sort),
            options = sortOptions,
            isSelected = { it == state.traktSortBy },
            onSelected = { CollectionEditorRepository.setTraktSortBy(it) },
            onDismiss = { showSortSheet = false },
        )
    }
}

// ---- Simkl ------------------------------------------------------------------------------------

@Composable
internal fun SimklSourceSheet(state: CollectionEditorUiState, onDismiss: () -> Unit) {
    val simklSources = state.editingFolder?.resolvedSources.orEmpty().filter { it.isSimkl }
    SourceSheet(
        title = stringResource(Res.string.collections_editor_simkl_sources),
        selectedCount = simklSources.size,
        onDismiss = onDismiss,
        description = stringResource(Res.string.collections_simkl_premium_only),
    ) {
        item(key = "simkl-add") {
            CollectionEditorGroup {
                row { shape ->
                    FormFieldRow(shape) {
                        OutlinedTextField(
                            value = state.simklInput,
                            onValueChange = { CollectionEditorRepository.setSimklInput(it) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text(stringResource(Res.string.collections_editor_add_by_link)) },
                            placeholder = { Text(stringResource(Res.string.collections_editor_simkl_input_placeholder)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        )
                        SourceTitleField(
                            value = state.simklTitleInput,
                            onValueChange = { CollectionEditorRepository.setSimklTitleInput(it) },
                            placeholder = null,
                        )
                    }
                }
                state.simklError?.let { error -> row { shape -> SourceMessageRow(error, shape) } }
                row { shape ->
                    SourceAddButtonRow(
                        shape = shape,
                        enabled = state.simklInput.isNotBlank(),
                        onClick = { CollectionEditorRepository.addSimklSourceFromInput() },
                    )
                }
            }
        }
        if (simklSources.isNotEmpty()) {
            item(key = "simkl-added") {
                val subtitle = stringResource(Res.string.collections_folder_simkl_list)
                CollectionEditorGroup {
                    simklSources.forEach { source -> addedSourceRow(source) { "$subtitle · ${source.simklListId}" } }
                }
            }
        }
    }
}

// ---- Shared pieces ----------------------------------------------------------------------------

/** The frame of every source sheet: a title and its close, what it is for, and how many are picked. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SourceSheet(
    title: String,
    selectedCount: Int,
    onDismiss: () -> Unit,
    description: String? = null,
    content: LazyListScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "sheet-header") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        SheetNavigationButton(navigation = SheetNavigation.Close, onClick = onDismiss)
                    }
                    description?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        text = stringResource(Res.string.collections_editor_selected_count, selectedCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            content()
            item(key = "sheet-bottom") { Spacer(modifier = Modifier.height(safeBottomPadding(12.dp))) }
        }
    }
}

/** One kind of source: a header that opens and closes it, and what it holds under it in the same group. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun LazyListScope.sourceGroup(
    key: String,
    title: String,
    expanded: Boolean,
    count: Int?,
    onToggle: () -> Unit,
    body: @Composable CollectionEditorGroupScope.() -> Unit,
    after: (@Composable () -> Unit)? = null,
) {
    item(key = key) {
        CollectionEditorGroup {
            row { shape ->
                ExpandableListHeader(
                    title = title,
                    expanded = expanded,
                    shapes = ListItemDefaults.shapes(
                        shape = shape,
                        selectedShape = shape,
                        pressedShape = shape,
                        focusedShape = shape,
                        hoveredShape = shape,
                    ),
                    onClick = onToggle,
                    supporting = count?.takeIf { it > 0 }?.let { stringResource(Res.string.collections_editor_selected_count, it) },
                    titleMaxLines = Int.MAX_VALUE,
                )
            }
            if (expanded) body()
        }
    }
    if (after != null) item(key = "$key-after") { after() }
}

/** A source to pick: checked when the folder has it, and tapping it again takes it away. */
@Composable
private fun SourceCheckRow(
    selected: Boolean,
    title: String,
    subtitle: String?,
    shape: RoundedCornerShape,
    onToggle: () -> Unit,
) {
    SelectableListRow(
        selected = selected,
        onClick = onToggle,
        headline = title,
        supporting = subtitle,
        unselectedShape = shape,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        // Null handler: the row owns the click, so the box is an indicator rather than a second target.
        trailingContent = { Checkbox(checked = selected, onCheckedChange = null) },
    )
}

/** A source the folder has, from a field rather than a list of results; unticking it removes it. */
private fun CollectionEditorGroupScope.addedSourceRow(source: CollectionSource, subtitle: @Composable () -> String) {
    val key = collectionSourceKey(source)
    row { shape ->
        SourceCheckRow(
            selected = true,
            title = source.title?.takeIf { it.isNotBlank() } ?: subtitle(),
            subtitle = subtitle(),
            shape = shape,
            onToggle = { CollectionEditorRepository.removeSources { collectionSourceKey(it) == key } },
        )
    }
}

@Composable
private fun SourceTitleField(value: String, onValueChange: (String) -> Unit, placeholder: String?) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(stringResource(Res.string.collections_editor_tmdb_display_title)) },
        placeholder = placeholder?.let { { Text(it) } },
        supportingText = { Text(stringResource(Res.string.collections_editor_tmdb_title_helper)) },
    )
}

@Composable
private fun SourceAddButtonRow(shape: RoundedCornerShape, enabled: Boolean, onClick: () -> Unit) {
    FormFieldRow(shape) {
        FilledTonalButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.collections_editor_add_source))
        }
    }
}

@Composable
private fun SourceMessageRow(text: String, shape: RoundedCornerShape) {
    FormFieldRow(shape) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
    }
}

/** A chip that shows the current choice and opens the rest. */
@Composable
private fun DropdownChip(label: String, onClick: () -> Unit) {
    FilterChip(
        selected = false,
        onClick = onClick,
        label = { Text(label) },
        trailingIcon = {
            Icon(
                imageVector = Icons.Rounded.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(FilterChipDefaults.IconSize),
            )
        },
    )
}
