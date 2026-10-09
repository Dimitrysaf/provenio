package io.github.dimitrysaf.provenio.shell.screens.details

import androidx.compose.runtime.CompositionLocalProvider
import io.github.dimitrysaf.provenio.shell.components.LocalPosterAreaWidth
import io.github.dimitrysaf.provenio.shell.components.ShapedArtworkImage
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.carousel.HorizontalCenteredHeroCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.provenio.core.build.isIos
import io.github.dimitrysaf.provenio.core.home.MetaPreview
import io.github.dimitrysaf.provenio.core.i18n.localizedShortMonthName
import io.github.dimitrysaf.provenio.core.metadata.MetaScreenSettingsRepository
import io.github.dimitrysaf.provenio.core.metadata.PersonDetail
import io.github.dimitrysaf.provenio.core.metadata.tmdb.TmdbMetadataService
import io.github.dimitrysaf.provenio.core.watch.progress.CurrentDateProvider
import io.github.dimitrysaf.provenio.core.watch.watched.WatchedRepository
import io.github.dimitrysaf.provenio.shell.components.BackButton
import io.github.dimitrysaf.provenio.shell.components.LoadErrorDialog
import io.github.dimitrysaf.provenio.shell.components.PageScrollbar
import io.github.dimitrysaf.provenio.shell.components.rememberPosterCellWidth
import io.github.dimitrysaf.provenio.core.home.HomeCatalogSettingsRepository
import io.github.dimitrysaf.provenio.core.home.HomeShelfLayout
import io.github.dimitrysaf.provenio.shell.components.loadErrorIcon
import io.github.dimitrysaf.provenio.core.network.LoadFailure
import io.github.dimitrysaf.provenio.core.network.NetworkStatusRepository
import io.github.dimitrysaf.provenio.core.network.toLoadFailure
import io.github.dimitrysaf.provenio.shell.components.DeceasedPhotoFilter
import io.github.dimitrysaf.provenio.shell.components.ImageViewer
import io.github.dimitrysaf.provenio.shell.components.viewerImageOf
import io.github.dimitrysaf.provenio.shell.components.SkeletonPosterRow
import io.github.dimitrysaf.provenio.shell.components.landscapePosterHeightForWidth
import io.github.dimitrysaf.provenio.shell.components.landscapePosterWidth
import io.github.dimitrysaf.provenio.shell.components.platformPhysicalTopInset
import io.github.dimitrysaf.provenio.shell.components.rememberPosterCardStyleUiState
import io.github.dimitrysaf.provenio.shell.components.safeBottomPadding
import io.github.dimitrysaf.provenio.shell.components.shapedClickable
import io.github.dimitrysaf.provenio.shell.components.skeleton
import io.github.dimitrysaf.provenio.shell.nav.LocalUseNativeNavigation
import io.github.dimitrysaf.provenio.shell.screens.details.components.DetailInfoRows
import io.github.dimitrysaf.provenio.shell.screens.details.components.DetailPosterRailSection
import io.github.dimitrysaf.provenio.shell.screens.details.components.DetailSection
import io.github.dimitrysaf.provenio.shell.screens.details.components.ExpandableDescription
import io.github.dimitrysaf.provenio.shell.screens.home.components.HeroMinSmallItemWidth
import io.github.dimitrysaf.provenio.shell.screens.home.components.HeroOnArtworkColor
import io.github.dimitrysaf.provenio.shell.screens.home.components.HeroOnArtworkVariantColor
import io.github.dimitrysaf.provenio.shell.screens.home.components.HomeHeroLayout
import io.github.dimitrysaf.provenio.shell.screens.home.components.heroCarouselTopInset
import io.github.dimitrysaf.provenio.shell.screens.home.components.heroItemContentAlpha
import io.github.dimitrysaf.provenio.shell.screens.home.components.homeHeroLayout
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.*

private sealed interface PersonDetailUiState {
    data object Loading : PersonDetailUiState
    data class Success(val personDetail: PersonDetail) : PersonDetailUiState
    data class Error(val failure: LoadFailure) : PersonDetailUiState
}

@Composable
fun PersonDetailScreen(
    personId: Int,
    personName: String,
    initialProfilePhoto: String? = null,
    preferCrew: Boolean = false,
    onBack: () -> Unit,
    onOpenMeta: (MetaPreview) -> Unit,
    modifier: Modifier = Modifier,
) {
    var uiState by remember(personId) {
        mutableStateOf(
            TmdbMetadataService.peekPersonDetail(personId = personId, preferCrewCredits = preferCrew)
                ?.let { PersonDetailUiState.Success(it) }
                ?: PersonDetailUiState.Loading,
        )
    }
    var loadAttempt by remember(personId) { mutableIntStateOf(0) }
    val watchedUiState by remember {
        WatchedRepository.ensureLoaded()
        WatchedRepository.uiState
    }.collectAsStateWithLifecycle()
    val fullyWatchedSeriesKeys by WatchedRepository.fullyWatchedSeriesKeys.collectAsStateWithLifecycle()

    LaunchedEffect(personId, loadAttempt) {
        if (uiState !is PersonDetailUiState.Success) {
            uiState = PersonDetailUiState.Loading
        }
        val result = TmdbMetadataService.fetchPersonDetail(
            personId = personId,
            preferCrewCredits = preferCrew,
        )
        uiState = result.fold(
            onSuccess = { PersonDetailUiState.Success(it) },
            onFailure = { error ->
                uiState.takeIf { it is PersonDetailUiState.Success }
                    ?: PersonDetailUiState.Error(error.toLoadFailure(sourceName = "TMDB", request = "person/$personId"))
            },
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when (val state = uiState) {
            is PersonDetailUiState.Loading -> PersonDetailSkeleton(
                personId = personId,
                personName = personName,
                profilePhoto = initialProfilePhoto,
                onBack = onBack,
            )
            is PersonDetailUiState.Error -> PersonDetailError(
                personId = personId,
                personName = personName,
                profilePhoto = initialProfilePhoto,
                failure = state.failure,
                onBack = onBack,
                onRetry = { loadAttempt++ },
            )
            is PersonDetailUiState.Success -> PersonDetailContent(
                person = state.personDetail,
                watchedKeys = watchedUiState.watchedKeys,
                fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                onBack = onBack,
                onOpenMeta = onOpenMeta,
                initialProfilePhoto = initialProfilePhoto,
            )
        }
    }
}

internal class DetailPageMetrics(
    val horizontalPadding: Dp,
    val contentMaxWidth: Dp,
    /** How the rails lay out as grid shelves; null while shelves are horizontal rows. */
    val railShelfGrid: DetailShelfGridContext? = null,
)

/**
 * A details page: the artwork carousel with the title over it, then [infoItems], with [railItems]
 * beside them on wide screens. Without [images], the carousel shows [placeholderIcon] instead.
 */
@Composable
internal fun DetailPage(
    pageKey: String,
    name: String,
    subtitle: String?,
    images: List<String>,
    deceased: Boolean,
    onBack: () -> Unit,
    infoItems: LazyListScope.(DetailPageMetrics) -> Unit,
    railItems: LazyListScope.(DetailPageMetrics) -> Unit,
    placeholderIcon: ImageVector? = null,
) {
    val metaScreenSettingsUiState by remember {
        MetaScreenSettingsRepository.ensureLoaded()
        MetaScreenSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val scroll = rememberDetailScrollState(pageKey)
    val sidePaneListState = rememberLazyListState()
    val shelfSettings by remember {
        HomeCatalogSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val posterCellWidth = rememberPosterCellWidth()
    val posterCardStyle = rememberPosterCardStyleUiState()
    var viewerIndex by remember(pageKey) { mutableStateOf<Int?>(null) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTwoPane = maxWidth >= DetailTwoPaneMinWidth
        val isTablet = maxWidth >= 720.dp
        val viewportHeight = maxHeight
        val horizontalPadding = when {
            isTwoPane -> 24.dp
            isTablet -> 32.dp
            else -> 18.dp
        }
        val primaryPaneWeight = if (isTwoPane) DetailPrimaryPaneWeight else 1f
        val primaryPaneWidth = maxWidth * primaryPaneWeight
        val sidePaneWidth = maxWidth * (1f - primaryPaneWeight)
        val metrics = DetailPageMetrics(
            horizontalPadding = horizontalPadding,
            contentMaxWidth = if (isTablet && !isTwoPane) {
                (maxWidth * 0.6f).coerceIn(520.dp, 680.dp)
            } else {
                Dp.Unspecified
            },
            railShelfGrid = if (shelfSettings.shelfLayout == HomeShelfLayout.Grid) {
                DetailShelfGridContext(
                    // Rails fill their pane: the side pane on wide screens, the page otherwise.
                    contentWidth = (if (isTwoPane) maxWidth * (1f - primaryPaneWeight) else maxWidth) -
                        horizontalPadding * 2,
                    horizontalPadding = horizontalPadding,
                    contentMaxWidth = Dp.Unspecified,
                    expandedByDefault = shelfSettings.shelvesExpandedByDefault,
                    posterCellWidth = posterCellWidth,
                    posterCardStyle = posterCardStyle,
                )
            } else {
                null
            },
        )
        val backdropUrl = images.firstOrNull()
        val backgroundMode = metaScreenSettingsUiState.backgroundMode

        Box(modifier = Modifier.fillMaxSize()) {
            DetailBackdrop(
                mode = backgroundMode,
                backdropUrl = backdropUrl,
                visible = true,
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f),
            ) {
                CompositionLocalProvider(LocalPosterAreaWidth provides primaryPaneWidth) {
                LazyColumn(
                    state = scroll.listState,
                    modifier = Modifier
                        .weight(primaryPaneWeight)
                        .fillMaxHeight(),
                ) {
                    item(key = "person-hero") {
                        PersonHero(
                            name = name,
                            subtitle = subtitle,
                            images = images,
                            deceased = deceased,
                            placeholderIcon = placeholderIcon,
                            viewportHeight = viewportHeight,
                            onOpenImage = { viewerIndex = it },
                            onHeightChanged = { scroll.heroHeightPx.intValue = it },
                        )
                    }

                    infoItems(metrics)

                    if (!isTwoPane) {
                        railItems(metrics)
                    }

                    item(key = "person-bottom-spacer") {
                        Spacer(modifier = Modifier.height(safeBottomPadding(32.dp)))
                    }
                }
                }

                if (isTwoPane) CompositionLocalProvider(LocalPosterAreaWidth provides sidePaneWidth) {
                    LazyColumn(
                        state = sidePaneListState,
                        modifier = Modifier
                            .weight(1f - primaryPaneWeight)
                            .fillMaxHeight(),
                        contentPadding = PaddingValues(
                            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() +
                                TopAppBarDefaults.TopAppBarExpandedHeight,
                        ),
                    ) {
                        railItems(metrics)

                        item(key = "person-side-bottom-spacer") {
                            Spacer(modifier = Modifier.height(safeBottomPadding(32.dp)))
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth(primaryPaneWeight)
                    .fillMaxHeight()
                    .zIndex(1f),
            ) {
                PageScrollbar(state = scroll.listState, modifier = Modifier.align(Alignment.TopEnd))
            }
            if (isTwoPane) {
                PageScrollbar(
                    state = sidePaneListState,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .zIndex(1f),
                )
            }

            PersonHeaderOverlay(
                title = name,
                isHeroCollapsed = scroll.isHeroCollapsed,
                onBack = onBack,
                modifier = Modifier.fillMaxWidth(primaryPaneWeight),
            )
        }
    }

    viewerIndex?.let { index ->
        ImageViewer(
            images = remember(images) {
                images.map(::viewerImageOf)
            },
            initialIndex = index,
            title = name,
            colorFilter = if (deceased) DeceasedPhotoFilter else null,
            onDismiss = { viewerIndex = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonHero(
    name: String,
    subtitle: String?,
    images: List<String>,
    deceased: Boolean,
    placeholderIcon: ImageVector?,
    viewportHeight: Dp,
    onOpenImage: (Int) -> Unit,
    onHeightChanged: (Int) -> Unit,
) {
    val pages = images.ifEmpty { listOf("") }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val layout = homeHeroLayout(
            maxWidthDp = maxWidth.value,
            viewportHeightDp = viewportHeight.value,
        )
        val topInset = heroCarouselTopInset()
        val sectionHeightPx = with(LocalDensity.current) {
            (topInset + layout.heroHeight + layout.contentVerticalPadding).roundToPx()
        }
        LaunchedEffect(sectionHeightPx) { onHeightChanged(sectionHeightPx) }
        val carouselState = rememberCarouselState(itemCount = { pages.size })
        val coroutineScope = rememberCoroutineScope()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topInset),
        ) {
            if (pages.size == 1) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = layout.contentHorizontalPadding)
                        .height(layout.heroHeight)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .shapedClickable(MaterialTheme.shapes.extraLarge, enabled = pages[0].isNotBlank()) {
                            onOpenImage(0)
                        },
                ) {
                    PersonHeroPage(
                        url = pages[0],
                        name = name,
                        subtitle = subtitle,
                        deceased = deceased,
                        placeholderIcon = placeholderIcon,
                        layout = layout,
                        contentAlpha = { 1f },
                    )
                }
            } else {
                HorizontalCenteredHeroCarousel(
                    state = carouselState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(layout.heroHeight),
                    itemSpacing = layout.itemSpacing,
                    minSmallItemWidth = minOf(HeroMinSmallItemWidth, layout.smallItemWidth),
                    maxSmallItemWidth = layout.smallItemWidth,
                    contentPadding = PaddingValues(horizontal = layout.contentHorizontalPadding),
                ) { index ->
                    val drawInfo = carouselItemDrawInfo
                    val isFocal = index == carouselState.currentItem
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .maskClip(MaterialTheme.shapes.extraLarge)
                            .shapedClickable(MaterialTheme.shapes.extraLarge) {
                                if (isFocal) {
                                    onOpenImage(index)
                                } else {
                                    coroutineScope.launch { carouselState.animateScrollToItem(index) }
                                }
                            },
                    ) {
                        PersonHeroPage(
                            url = pages[index],
                            name = name,
                            subtitle = subtitle,
                            deceased = deceased,
                            placeholderIcon = placeholderIcon,
                            layout = layout,
                            contentAlpha = { heroItemContentAlpha(drawInfo) },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(layout.contentVerticalPadding))
        }
    }
}

@Composable
private fun PersonHeroPage(
    url: String,
    name: String,
    subtitle: String?,
    deceased: Boolean,
    placeholderIcon: ImageVector?,
    layout: HomeHeroLayout,
    contentAlpha: () -> Float,
) {
    val centerTitle = layout.centerTitle

    Box(modifier = Modifier.fillMaxSize()) {
        if (url.isBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                if (placeholderIcon != null) {
                    Icon(
                        imageVector = placeholderIcon,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            ShapedArtworkImage(
                candidates = listOf(url),
                contentDescription = name,
                alignment = Alignment.TopCenter,
                colorFilter = if (deceased) DeceasedPhotoFilter else null,
                showSkeleton = true,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f),
                            MaterialTheme.colorScheme.scrim.copy(alpha = 0.86f),
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(layout.contentWidthFraction)
                .widthIn(max = layout.contentMaxWidth)
                .padding(
                    horizontal = layout.contentHorizontalPadding,
                    vertical = layout.contentVerticalPadding,
                )
                .graphicsLayer { alpha = contentAlpha() },
            horizontalAlignment = if (centerTitle) Alignment.CenterHorizontally else Alignment.Start,
        ) {
            Text(
                text = name,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.displaySmallEmphasized,
                color = HeroOnArtworkColor,
                textAlign = if (centerTitle) TextAlign.Center else TextAlign.Start,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelLarge,
                    color = HeroOnArtworkVariantColor,
                    textAlign = if (centerTitle) TextAlign.Center else TextAlign.Start,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonHeaderOverlay(
    title: String,
    isHeroCollapsed: State<Boolean>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val target = if (isHeroCollapsed.value) 1f else 0f
    val progress by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(
            durationMillis = if (target > 0f) 150 else 100,
            easing = LinearOutSlowInEasing,
        ),
        label = "person_header_progress",
    )
    val useNativeNavigation = LocalUseNativeNavigation.current
    val safeAreaTop = if (useNativeNavigation) {
        platformPhysicalTopInset()
    } else {
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    }
    val headerTopPadding = (safeAreaTop - 6.dp).coerceAtLeast(safeAreaTop * 0.8f)
    val surfaceColor = if (isIos) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.background
    }

    Box(
        modifier = modifier
            .zIndex(2f)
            .graphicsLayer {
                shadowElevation = 4.dp.toPx() * progress
                shape = RectangleShape
            }
            .background(surfaceColor.copy(alpha = progress)),
    ) {
        CenterAlignedTopAppBar(
            modifier = Modifier.padding(top = headerTopPadding),
            windowInsets = WindowInsets(0, 0, 0, 0),
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                actionIconContentColor = MaterialTheme.colorScheme.onBackground,
            ),
            title = {
                Text(
                    text = title,
                    modifier = Modifier.graphicsLayer { alpha = progress },
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            navigationIcon = {
                if (!useNativeNavigation) {
                    BackButton(
                        onClick = onBack,
                        containerColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.40f * (1f - progress)),
                        contentColor = lerp(Color.White, MaterialTheme.colorScheme.onBackground, progress),
                    )
                } else {
                    Box(modifier = Modifier.size(PersonNavigationSlotSize))
                }
            },
            actions = {
                Box(modifier = Modifier.size(PersonNavigationSlotSize))
            },
        )
    }
}

@Composable
private fun PersonDetailContent(
    person: PersonDetail,
    watchedKeys: Set<String>,
    fullyWatchedSeriesKeys: Set<String>,
    onBack: () -> Unit,
    onOpenMeta: (MetaPreview) -> Unit,
    initialProfilePhoto: String?,
) {
    val todayDate = remember { CurrentDateProvider.todayIsoDate() }
    val allCredits = remember(person.movieCredits, person.tvCredits) {
        (person.movieCredits + person.tvCredits).distinctBy { it.id }
    }
    val popularCredits = remember(allCredits) {
        allCredits.sortedByDescending { it.popularity ?: 0.0 }
    }
    val latestCredits = remember(allCredits, todayDate) {
        allCredits
            .filter { credit -> credit.rawReleaseDate?.let { it <= todayDate } == true }
            .sortedByDescending { it.rawReleaseDate.orEmpty() }
    }
    val upcomingCredits = remember(allCredits, todayDate) {
        allCredits
            .filter { credit -> credit.rawReleaseDate?.let { it > todayDate } == true }
            .sortedBy { it.rawReleaseDate.orEmpty() }
    }
    val rails = listOf(
        Triple("person-popular", stringResource(Res.string.person_popular), popularCredits),
        Triple("person-latest", stringResource(Res.string.person_latest), latestCredits),
        Triple("person-upcoming", stringResource(Res.string.person_upcoming), upcomingCredits),
    ).filter { it.third.isNotEmpty() }
    val infoRows = personInfoRows(person = person, credits = allCredits, todayDate = todayDate)
    val infoTitle = stringResource(Res.string.person_detail_personal_info)
    val biographyTitle = stringResource(Res.string.person_detail_biography)
    val biography = person.biography?.trim()?.takeIf(String::isNotBlank)
    val images = remember(person.profileImages, person.profilePhoto, initialProfilePhoto) {
        person.profileImages.ifEmpty {
            listOfNotNull(person.profilePhoto?.takeIf(String::isNotBlank) ?: initialProfilePhoto?.takeIf(String::isNotBlank))
        }
    }

    DetailPage(
        pageKey = "person-${person.tmdbId}",
        name = person.name,
        subtitle = person.knownFor?.trim()?.takeIf(String::isNotBlank),
        images = images,
        deceased = !person.deathday.isNullOrBlank(),
        onBack = onBack,
        infoItems = { metrics ->
            if (infoRows.isNotEmpty()) {
                item(key = "person-info") {
                    DetailSectionContainer(
                        horizontalPadding = metrics.horizontalPadding,
                        contentMaxWidth = metrics.contentMaxWidth,
                    ) {
                        DetailSection(title = infoTitle) {
                            DetailInfoRows(rows = infoRows)
                        }
                    }
                }
            }
            if (biography != null) {
                item(key = "person-biography") {
                    DetailSectionContainer(
                        horizontalPadding = metrics.horizontalPadding,
                        contentMaxWidth = metrics.contentMaxWidth,
                    ) {
                        DetailSection(title = biographyTitle) {
                            ExpandableDescription(
                                text = biography,
                                collapsedMaxLines = 6,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        railItems = { metrics ->
            rails.forEach { (key, title, items) ->
                val grid = metrics.railShelfGrid
                if (grid != null) {
                    detailPosterShelfGrid(
                        key = key,
                        title = { title },
                        items = items,
                        context = grid,
                        watchedKeys = watchedKeys,
                        fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                        onPosterClick = onOpenMeta,
                    )
                    return@forEach
                }
                item(key = key) {
                    DetailSectionContainer(
                        horizontalPadding = metrics.horizontalPadding,
                        contentMaxWidth = Dp.Unspecified,
                    ) {
                        DetailPosterRailSection(
                            title = title,
                            items = items,
                            watchedKeys = watchedKeys,
                            fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                            horizontalScrollPadding = metrics.horizontalPadding,
                            onPosterClick = onOpenMeta,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun personInfoRows(
    person: PersonDetail,
    credits: List<MetaPreview>,
    todayDate: String,
): List<Pair<String, String>> {
    val birthday = person.birthday?.trim()?.takeIf(String::isNotBlank)
    val deathday = person.deathday?.trim()?.takeIf(String::isNotBlank)
    val age = if (birthday != null) calculateAge(birthday, deathday ?: todayDate) else null
    val birthdayDisplay = birthday?.let { formatDateForDisplay(it) ?: it }
    val deathdayDisplay = deathday?.let { formatDateForDisplay(it) ?: it }
    val bornValue = if (birthdayDisplay != null && deathdayDisplay == null && age != null) {
        stringResource(Res.string.person_detail_date_with_age, birthdayDisplay, age)
    } else {
        birthdayDisplay
    }
    val diedValue = if (deathdayDisplay != null && age != null) {
        stringResource(Res.string.person_detail_date_with_age, deathdayDisplay, age)
    } else {
        deathdayDisplay
    }
    val firstYear = credits.mapNotNull { it.rawReleaseDate?.take(4)?.toIntOrNull() }.minOrNull()

    val candidates = listOf(
        stringResource(Res.string.person_detail_born) to bornValue,
        stringResource(Res.string.person_detail_died) to diedValue,
        stringResource(Res.string.person_detail_place_of_birth) to person.placeOfBirth,
        stringResource(Res.string.person_detail_credits) to credits.size.takeIf { it > 0 }?.toString(),
        stringResource(Res.string.person_detail_active_since) to firstYear?.toString(),
    )
    return candidates.mapNotNull { (label, value) ->
        value?.trim()?.takeIf(String::isNotBlank)?.let { label to it }
    }
}


@Composable
private fun PersonDetailSkeleton(
    personId: Int,
    personName: String,
    profilePhoto: String?,
    onBack: () -> Unit,
    placeholderIcon: ImageVector? = null,
) {
    val posterCardStyle = rememberPosterCardStyleUiState()
    val isLandscapeShelfMode = posterCardStyle.catalogLandscapeModeEnabled
    val posterWidth = if (isLandscapeShelfMode) {
        landscapePosterWidth(posterCardStyle.widthDp)
    } else {
        posterCardStyle.widthDp.dp
    }
    val posterHeight = if (isLandscapeShelfMode) {
        landscapePosterHeightForWidth(posterWidth)
    } else {
        posterCardStyle.heightDp.dp
    }
    val showPosterLabels = !isLandscapeShelfMode && !posterCardStyle.hideLabelsEnabled

    DetailPage(
        pageKey = "person-$personId",
        name = personName,
        subtitle = null,
        images = listOfNotNull(profilePhoto?.takeIf(String::isNotBlank)),
        deceased = false,
        onBack = onBack,
        placeholderIcon = placeholderIcon,
        infoItems = { metrics ->
            item(key = "person-info") {
                DetailSectionContainer(
                    horizontalPadding = metrics.horizontalPadding,
                    contentMaxWidth = metrics.contentMaxWidth,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        SkeletonTitle()
                        Column(modifier = Modifier.fillMaxWidth()) {
                            listOf(0.42f, 0.30f, 0.12f, 0.16f).forEachIndexed { index, valueFraction ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 13.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    SkeletonLine(widthFraction = 0.24f, height = 14.dp)
                                    SkeletonLine(widthFraction = valueFraction / 0.76f, height = 14.dp)
                                }
                                if (index < 3) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }
                    }
                }
            }
            item(key = "person-biography") {
                DetailSectionContainer(
                    horizontalPadding = metrics.horizontalPadding,
                    contentMaxWidth = metrics.contentMaxWidth,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        SkeletonTitle()
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1f, 0.96f, 0.92f, 0.98f, 0.88f, 0.64f).forEach { widthFraction ->
                                SkeletonLine(widthFraction = widthFraction, height = 16.dp)
                            }
                        }
                    }
                }
            }
        },
        railItems = { metrics ->
            items(count = 2, key = { "person-rail-skeleton-$it" }) {
                DetailSectionContainer(
                    horizontalPadding = metrics.horizontalPadding,
                    contentMaxWidth = Dp.Unspecified,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        SkeletonTitle()
                        SkeletonPosterRow(
                            width = posterWidth,
                            height = posterHeight,
                            cornerRadius = posterCardStyle.cornerRadiusDp.dp,
                            showLabels = showPosterLabels,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun SkeletonTitle() {
    Box(
        modifier = Modifier
            .width(140.dp)
            .height(22.dp)
            .skeleton(),
    )
}

@Composable
private fun SkeletonLine(
    widthFraction: Float,
    height: Dp,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction.coerceIn(0f, 1f))
            .height(height)
            .skeleton(),
    )
}

/** The person page's skeleton, with what is already known, under a dialog saying why it didn't load. */
@Composable
private fun PersonDetailError(
    personId: Int,
    personName: String,
    profilePhoto: String?,
    failure: LoadFailure,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    val networkStatusUiState by NetworkStatusRepository.uiState.collectAsStateWithLifecycle()
    val failures = listOf(failure)

    PersonDetailSkeleton(
        personId = personId,
        personName = personName,
        profilePhoto = profilePhoto,
        onBack = onBack,
        placeholderIcon = loadErrorIcon(failures, networkStatusUiState.condition),
    )
    LoadErrorDialog(
        failures = failures,
        networkCondition = networkStatusUiState.condition,
        onRetry = {
            NetworkStatusRepository.requestRefresh(force = true)
            onRetry()
        },
        onDismiss = onBack,
    )
}

private val PersonNavigationSlotSize = 48.dp

private fun calculateAge(birthday: String, endDate: String): Int? {
    val birthParts = birthday.split("-").mapNotNull { it.toIntOrNull() }
    val endParts = endDate.split("-").mapNotNull { it.toIntOrNull() }
    if (birthParts.size < 3 || endParts.size < 3) return null
    val (birthYear, birthMonth, birthDay) = birthParts
    val (endYear, endMonth, endDay) = endParts
    var age = endYear - birthYear
    if (endMonth < birthMonth || (endMonth == birthMonth && endDay < birthDay)) {
        age--
    }
    return age.takeIf { it >= 0 }
}

private fun formatDateForDisplay(date: String): String? {
    val parts = date.split("-").mapNotNull { it.toIntOrNull() }
    if (parts.size < 3) return null
    val (year, month, day) = parts
    return if (month in 1..12) {
        "${localizedShortMonthName(month)} $day, $year"
    } else {
        null
    }
}
