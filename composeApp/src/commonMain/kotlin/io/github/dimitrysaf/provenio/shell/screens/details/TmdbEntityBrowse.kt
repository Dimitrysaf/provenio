package io.github.dimitrysaf.provenio.shell.screens.details

import io.github.dimitrysaf.provenio.shell.components.rememberPosterCardStyleUiState
import io.github.dimitrysaf.provenio.shell.components.rememberPosterCellWidth
import io.github.dimitrysaf.provenio.core.home.HomeCatalogSettingsRepository
import io.github.dimitrysaf.provenio.core.home.HomeShelfLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import provenio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import io.github.dimitrysaf.provenio.shell.components.ScreenScaffold
import io.github.dimitrysaf.provenio.shell.components.SkeletonBlock
import io.github.dimitrysaf.provenio.shell.screens.home.components.HomeSkeletonRow
import io.github.dimitrysaf.provenio.shell.screens.details.components.CompanyLogo
import io.github.dimitrysaf.provenio.shell.screens.details.components.DetailInfoRows
import io.github.dimitrysaf.provenio.shell.screens.details.components.DetailPosterRailSection
import io.github.dimitrysaf.provenio.core.home.MetaPreview
import io.github.dimitrysaf.provenio.core.metadata.tmdb.TmdbEntityBrowseData
import io.github.dimitrysaf.provenio.core.metadata.tmdb.TmdbEntityHeader
import io.github.dimitrysaf.provenio.core.metadata.tmdb.TmdbEntityKind
import io.github.dimitrysaf.provenio.core.metadata.tmdb.TmdbEntityMediaType
import io.github.dimitrysaf.provenio.core.metadata.tmdb.TmdbEntityRail
import io.github.dimitrysaf.provenio.core.metadata.tmdb.TmdbEntityRailType
import io.github.dimitrysaf.provenio.core.metadata.tmdb.TmdbMetadataService
import io.github.dimitrysaf.provenio.core.watch.watched.WatchedRepository
import io.github.dimitrysaf.provenio.shell.nav.LocalUseNativeNavigation

private sealed interface EntityBrowseUiState {
    data object Loading : EntityBrowseUiState
    data class Error(val message: String) : EntityBrowseUiState
    data class Success(val data: TmdbEntityBrowseData) : EntityBrowseUiState
}

private val EntityHeaderWideMinWidth = 600.dp
private val EntityHorizontalPadding = 16.dp

@Composable
fun TmdbEntityBrowseScreen(
    entityKind: TmdbEntityKind,
    entityId: Int,
    entityName: String,
    sourceType: String,
    onBack: () -> Unit,
    onOpenMeta: (MetaPreview) -> Unit,
    modifier: Modifier = Modifier,
) {
    var attempt by remember(entityKind, entityId) { mutableIntStateOf(0) }
    var uiState by remember(entityKind, entityId) {
        mutableStateOf<EntityBrowseUiState>(EntityBrowseUiState.Loading)
    }
    val watchedUiState by remember {
        WatchedRepository.ensureLoaded()
        WatchedRepository.uiState
    }.collectAsStateWithLifecycle()
    val fullyWatchedSeriesKeys by WatchedRepository.fullyWatchedSeriesKeys.collectAsStateWithLifecycle()
    val loadFailedMessage = stringResource(Res.string.details_browse_load_failed, entityName)

    LaunchedEffect(entityKind, entityId, attempt) {
        uiState = EntityBrowseUiState.Loading
        val data = TmdbMetadataService.fetchEntityBrowse(
            entityKind = entityKind,
            entityId = entityId,
            sourceType = sourceType,
            fallbackName = entityName,
        )
        uiState = if (data != null) {
            EntityBrowseUiState.Success(data)
        } else {
            EntityBrowseUiState.Error(loadFailedMessage)
        }
    }

    val state = uiState
    val title = (state as? EntityBrowseUiState.Success)?.data?.header?.name ?: entityName
    val shelfSettings by remember {
        HomeCatalogSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val posterCellWidth = rememberPosterCellWidth()
    val posterCardStyle = rememberPosterCardStyleUiState()
    BoxWithConstraints(modifier = modifier) {
        val railShelfGrid = if (shelfSettings.shelfLayout == HomeShelfLayout.Grid) {
            DetailShelfGridContext(
                contentWidth = maxWidth - EntityHorizontalPadding * 2,
                horizontalPadding = EntityHorizontalPadding,
                contentMaxWidth = Dp.Unspecified,
                expandedByDefault = shelfSettings.shelvesExpandedByDefault,
                posterCellWidth = posterCellWidth,
                posterCardStyle = posterCardStyle,
            )
        } else {
            null
        }
        ScreenScaffold(
            modifier = Modifier.fillMaxSize(),
            title = title,
            subtitle = entityKindLabel(entityKind),
            onBack = onBack.takeUnless { LocalUseNativeNavigation.current },
            horizontalPadding = 0.dp,
        ) {
            when (state) {
                is EntityBrowseUiState.Loading -> entityBrowseSkeleton()
                is EntityBrowseUiState.Error -> item(key = "error") {
                    EntityBrowseError(
                        message = state.message,
                        onRetry = { attempt++ },
                    )
                }
                is EntityBrowseUiState.Success -> entityBrowseContent(
                    data = state.data,
                    watchedKeys = watchedUiState.watchedKeys,
                    fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                    railShelfGrid = railShelfGrid,
                    onOpenMeta = onOpenMeta,
                )
            }
        }
    }
}

private fun LazyListScope.entityBrowseContent(
    data: TmdbEntityBrowseData,
    watchedKeys: Set<String>,
    fullyWatchedSeriesKeys: Set<String>,
    railShelfGrid: DetailShelfGridContext?,
    onOpenMeta: (MetaPreview) -> Unit,
) {
    item(key = "header") {
        EntityHeader(
            header = data.header,
            catalogueCount = data.rails.sumOf { it.items.size },
        )
    }
    if (data.rails.isEmpty()) {
        item(key = "empty") {
            Text(
                text = stringResource(Res.string.catalog_empty_title),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = EntityHorizontalPadding, vertical = 32.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    data.rails.forEach { rail ->
        if (railShelfGrid != null) {
            detailPosterShelfGrid(
                key = "rail-${rail.mediaType}-${rail.railType}",
                title = { entityRailTitle(rail) },
                items = rail.items,
                context = railShelfGrid,
                watchedKeys = watchedKeys,
                fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                onPosterClick = onOpenMeta,
            )
            return@forEach
        }
        item(key = "rail-${rail.mediaType}-${rail.railType}") {
            DetailPosterRailSection(
                title = entityRailTitle(rail),
                items = rail.items,
                watchedKeys = watchedKeys,
                fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                headerHorizontalPadding = EntityHorizontalPadding,
                onPosterClick = onOpenMeta,
            )
        }
    }
}

@Composable
private fun EntityHeader(
    header: TmdbEntityHeader,
    catalogueCount: Int,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = EntityHorizontalPadding),
    ) {
        val logo: @Composable () -> Unit = { EntityLogo(header = header) }
        val details: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                EntityFacts(header = header, catalogueCount = catalogueCount)
                header.description?.takeIf { it.isNotBlank() }?.let { description ->
                    EntityAbout(description = description)
                }
            }
        }
        if (maxWidth >= EntityHeaderWideMinWidth) {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                logo()
                Box(modifier = Modifier.weight(1f)) { details() }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                logo()
                details()
            }
        }
    }
}

@Composable
private fun EntityLogo(header: TmdbEntityHeader) {
    val logo = header.logo?.takeIf { it.isNotBlank() }
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = if (logo != null) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.primaryContainer
        },
    ) {
        Box(
            modifier = Modifier
                .width(EntityLogoWidth)
                .height(EntityLogoHeight)
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (logo != null) {
                CompanyLogo(
                    url = logo,
                    contentDescription = header.name,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    text = header.name.initials(),
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun EntityFacts(
    header: TmdbEntityHeader,
    catalogueCount: Int,
) {
    val rows = listOfNotNull(
        header.originCountry?.takeIf { it.isNotBlank() }?.let { stringResource(Res.string.entity_browse_country) to it },
        header.secondaryLabel?.takeIf { it.isNotBlank() }?.let { stringResource(Res.string.entity_browse_headquarters) to it },
        catalogueCount.takeIf { it > 0 }?.let {
            stringResource(Res.string.entity_browse_catalogue) to
                pluralStringResource(Res.plurals.entity_browse_title_count, it, it)
        },
    )
    if (rows.isNotEmpty()) {
        DetailInfoRows(rows = rows)
    }
}

@Composable
private fun EntityAbout(description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(Res.string.entity_browse_about),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 8,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun entityKindLabel(kind: TmdbEntityKind): String = when (kind) {
    TmdbEntityKind.COMPANY -> stringResource(Res.string.details_browse_kind_company)
    TmdbEntityKind.NETWORK -> stringResource(Res.string.details_browse_kind_network)
}

@Composable
private fun entityRailTitle(rail: TmdbEntityRail): String {
    val mediaLabel = when (rail.mediaType) {
        TmdbEntityMediaType.MOVIE -> stringResource(Res.string.media_movies)
        TmdbEntityMediaType.TV -> stringResource(Res.string.media_series)
    }
    val railLabel = when (rail.railType) {
        TmdbEntityRailType.POPULAR -> stringResource(Res.string.details_browse_rail_popular)
        TmdbEntityRailType.TOP_RATED -> stringResource(Res.string.details_browse_rail_top_rated)
        TmdbEntityRailType.RECENT -> stringResource(Res.string.details_browse_rail_recent)
    }
    return stringResource(Res.string.details_browse_rail_title, mediaLabel, railLabel)
}

private fun LazyListScope.entityBrowseSkeleton() {
    item(key = "header-skeleton") {
        Column(
            modifier = Modifier.padding(horizontal = EntityHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SkeletonBlock(width = EntityLogoWidth, height = EntityLogoHeight, cornerRadius = 28.dp)
            SkeletonBlock(modifier = Modifier.fillMaxWidth(), height = 128.dp, cornerRadius = 12.dp)
        }
    }
    items(3) {
        HomeSkeletonRow(horizontalPadding = EntityHorizontalPadding)
    }
}

@Composable
private fun EntityBrowseError(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = EntityHorizontalPadding, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FilledTonalButton(onClick = onRetry) {
            Text(stringResource(Res.string.action_retry))
        }
    }
}

private val EntityLogoWidth = 200.dp
private val EntityLogoHeight = 112.dp

private fun String.initials(): String {
    val parts = trim()
        .split(" ")
        .filter { it.isNotBlank() }
    return parts
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .joinToString("")
        .ifBlank { firstOrNull()?.uppercase() ?: "?" }
}
