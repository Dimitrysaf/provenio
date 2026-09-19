package com.nuvio.app.features.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.build.AppFeaturePolicy
import com.nuvio.app.core.build.TrailerPlaybackMode
import com.nuvio.app.core.ui.SelectableListRow
import com.nuvio.app.core.ui.SingleChoiceBottomSheet
import com.nuvio.app.core.ui.SingleChoiceOption
import com.nuvio.app.features.details.MetaEpisodeCardStyle
import com.nuvio.app.features.details.MetaScreenBackgroundMode
import com.nuvio.app.features.details.MetaScreenSectionItem
import com.nuvio.app.features.details.MetaScreenSectionKey
import com.nuvio.app.features.details.MetaScreenSettingsRepository
import com.nuvio.app.features.details.MetaScreenSettingsUiState
import com.nuvio.app.supportsPosterNavigationMotion
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_reorder
import nuvio.composeapp.generated.resources.action_reset
import nuvio.composeapp.generated.resources.settings_homescreen_hidden
import nuvio.composeapp.generated.resources.settings_homescreen_visible
import nuvio.composeapp.generated.resources.settings_meta_actions
import nuvio.composeapp.generated.resources.settings_meta_actions_description
import nuvio.composeapp.generated.resources.settings_meta_background_mode
import nuvio.composeapp.generated.resources.settings_meta_background_mode_cinematic
import nuvio.composeapp.generated.resources.settings_meta_background_mode_cinematic_description
import nuvio.composeapp.generated.resources.settings_meta_background_mode_description
import nuvio.composeapp.generated.resources.settings_meta_background_mode_dominant
import nuvio.composeapp.generated.resources.settings_meta_background_mode_dominant_description
import nuvio.composeapp.generated.resources.settings_meta_background_mode_normal
import nuvio.composeapp.generated.resources.settings_meta_background_mode_normal_description
import nuvio.composeapp.generated.resources.settings_meta_blur_unwatched_episodes
import nuvio.composeapp.generated.resources.settings_meta_blur_unwatched_episodes_description
import nuvio.composeapp.generated.resources.settings_meta_cast
import nuvio.composeapp.generated.resources.settings_meta_cast_description
import nuvio.composeapp.generated.resources.settings_meta_collection
import nuvio.composeapp.generated.resources.settings_meta_collection_description
import nuvio.composeapp.generated.resources.settings_meta_comments
import nuvio.composeapp.generated.resources.settings_meta_comments_description
import nuvio.composeapp.generated.resources.settings_meta_details
import nuvio.composeapp.generated.resources.settings_meta_details_description
import nuvio.composeapp.generated.resources.settings_meta_episode_cards
import nuvio.composeapp.generated.resources.settings_meta_episode_style_horizontal
import nuvio.composeapp.generated.resources.settings_meta_episode_style_horizontal_description
import nuvio.composeapp.generated.resources.settings_meta_episode_style_list
import nuvio.composeapp.generated.resources.settings_meta_episode_style_list_description
import nuvio.composeapp.generated.resources.settings_meta_episodes
import nuvio.composeapp.generated.resources.settings_meta_episodes_description
import nuvio.composeapp.generated.resources.settings_meta_group_label
import nuvio.composeapp.generated.resources.settings_meta_hero_trailer_playback
import nuvio.composeapp.generated.resources.settings_meta_hero_trailer_playback_description
import nuvio.composeapp.generated.resources.settings_meta_more_like_this
import nuvio.composeapp.generated.resources.settings_meta_more_like_this_description
import nuvio.composeapp.generated.resources.settings_meta_none
import nuvio.composeapp.generated.resources.settings_meta_overview
import nuvio.composeapp.generated.resources.settings_meta_overview_description
import nuvio.composeapp.generated.resources.settings_meta_poster_transition
import nuvio.composeapp.generated.resources.settings_meta_poster_transition_description
import nuvio.composeapp.generated.resources.settings_meta_production
import nuvio.composeapp.generated.resources.settings_meta_production_description
import nuvio.composeapp.generated.resources.settings_meta_section_appearance
import nuvio.composeapp.generated.resources.settings_meta_section_sections
import nuvio.composeapp.generated.resources.settings_meta_tab_group_format
import nuvio.composeapp.generated.resources.settings_meta_tab_layout
import nuvio.composeapp.generated.resources.settings_meta_tab_layout_description
import nuvio.composeapp.generated.resources.settings_meta_trailers
import nuvio.composeapp.generated.resources.settings_meta_trailers_description
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

internal fun LazyListScope.metaScreenSettingsContent(
    isTablet: Boolean,
    uiState: MetaScreenSettingsUiState,
) {
    val showHeroTrailerPlaybackSetting = AppFeaturePolicy.heroTrailerPlaybackSupported &&
        AppFeaturePolicy.trailerPlaybackMode == TrailerPlaybackMode.IN_APP
    item {
        var showBackgroundSheet by rememberSaveable { mutableStateOf(false) }
        SettingsSection(
            title = stringResource(Res.string.settings_meta_section_appearance),
            isTablet = isTablet,
        ) {
            SettingsList {
                navigationRow(
                    title = stringResource(Res.string.settings_meta_background_mode),
                    description = stringResource(uiState.backgroundMode.labelRes),
                    onClick = { showBackgroundSheet = true },
                )
                if (supportsPosterNavigationMotion) {
                    switchRow(
                        title = stringResource(Res.string.settings_meta_poster_transition),
                        description = stringResource(Res.string.settings_meta_poster_transition_description),
                        checked = { uiState.posterTransitionEnabled },
                        onCheckedChange = MetaScreenSettingsRepository::setPosterTransitionEnabled,
                    )
                }
                if (showHeroTrailerPlaybackSetting) {
                    switchRow(
                        title = stringResource(Res.string.settings_meta_hero_trailer_playback),
                        description = stringResource(Res.string.settings_meta_hero_trailer_playback_description),
                        checked = { uiState.heroTrailerPlayback },
                        onCheckedChange = { MetaScreenSettingsRepository.setHeroTrailerPlayback(it) },
                    )
                }
                switchRow(
                    title = stringResource(Res.string.settings_meta_tab_layout),
                    description = stringResource(Res.string.settings_meta_tab_layout_description),
                    checked = { uiState.tabLayout },
                    onCheckedChange = { MetaScreenSettingsRepository.setTabLayout(it) },
                )
                switchRow(
                    title = stringResource(Res.string.settings_meta_blur_unwatched_episodes),
                    description = stringResource(Res.string.settings_meta_blur_unwatched_episodes_description),
                    checked = { uiState.blurUnwatchedEpisodes },
                    onCheckedChange = { MetaScreenSettingsRepository.setBlurUnwatchedEpisodes(it) },
                )
            }
        }
        if (showBackgroundSheet) {
            SingleChoiceBottomSheet(
                title = stringResource(Res.string.settings_meta_background_mode),
                description = stringResource(Res.string.settings_meta_background_mode_description),
                options = MetaScreenBackgroundMode.entries.map { mode ->
                    SingleChoiceOption(
                        value = mode,
                        label = stringResource(mode.labelRes),
                        supportingText = stringResource(mode.descriptionRes),
                    )
                },
                isSelected = { it == uiState.backgroundMode },
                onSelected = MetaScreenSettingsRepository::setBackgroundMode,
                onDismiss = { showBackgroundSheet = false },
            )
        }
    }
    item {
        SettingsSection(
            title = stringResource(Res.string.settings_meta_episode_cards),
            isTablet = isTablet,
        ) {
            MetaEpisodeCardStyleSelector(
                selectedStyle = uiState.episodeCardStyle,
                onStyleSelected = MetaScreenSettingsRepository::setEpisodeCardStyle,
            )
        }
    }
    item {
        SettingsSection(
            title = stringResource(Res.string.settings_meta_section_sections),
            isTablet = isTablet,
        ) {
            MetaSectionReorderableList(
                items = uiState.items,
                isTablet = isTablet,
                tabLayout = uiState.tabLayout,
            )
        }
    }
}

/**
 * The metadata page's sections, as one segmented list.
 *
 * A row is selected when its section is shown, so tapping it is what shows or hides the section
 * and the expressive list's selected container is the state, in place of a switch. Reordering is
 * the handle at the end of each row. Reset scrolls with the rows because it belongs to them: it
 * restores this list alone and leaves the rest of the page as it is.
 */
@Composable
private fun MetaSectionReorderableList(
    items: List<MetaScreenSectionItem>,
    isTablet: Boolean,
    tabLayout: Boolean,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
    ) { from, to ->
        MetaScreenSettingsRepository.moveByIndex(from.index, to.index)
        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    // A tab group holds at most three sections, so a full group stops offering itself.
    val groupCounts: Map<Int, Int> = if (tabLayout) {
        items.filter { it.tabGroup != null }.groupBy { it.tabGroup!! }.mapValues { it.value.size }
    } else {
        emptyMap()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = if (isTablet) 820.dp else 640.dp),
        state = lazyListState,
        verticalArrangement = Arrangement.spacedBy(ListItemBetweenSpace),
    ) {
        itemsIndexed(items, key = { _, item -> item.key.name }) { index, item ->
            ReorderableItem(reorderableLazyListState, key = item.key.name) { isDragging ->
                val elevation by animateDpAsState(if (isDragging) 4.dp else 0.dp)
                val shape = segmentShape(index = index, count = items.size)
                Surface(
                    shadowElevation = elevation,
                    // The row morphs while it is dragged, so the shadow under it morphs too.
                    shape = if (isDragging) RoundedCornerShape(OuterCorner) else shape,
                    color = Color.Transparent,
                ) {
                    MetaSectionRow(
                        item = item,
                        selected = isDragging,
                        tabLayout = tabLayout,
                        groupCounts = groupCounts,
                        shape = shape,
                        onEnabledChange = { MetaScreenSettingsRepository.setEnabled(item.key, it) },
                        onTabGroupChange = { MetaScreenSettingsRepository.setTabGroup(item.key, it) },
                        dragHandleScope = this@ReorderableItem,
                    )
                }
            }
        }
        item {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = MetaScreenSettingsRepository::resetSectionsToDefaults,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.action_reset))
                }
            }
        }
    }
}

/**
 * One section of the metadata page.
 *
 * Being dragged is the row's one selected state. That state means a row has been picked out of
 * the others, which is what a drag does; whether a section is shown is a property of the section,
 * not a selection, so the eye reports it and the row otherwise keeps the container its neighbours
 * have.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MetaSectionRow(
    item: MetaScreenSectionItem,
    selected: Boolean,
    tabLayout: Boolean,
    groupCounts: Map<Int, Int>,
    shape: RoundedCornerShape,
    onEnabledChange: (Boolean) -> Unit,
    onTabGroupChange: (Int?) -> Unit,
    dragHandleScope: ReorderableCollectionItemScope,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val canPickGroup = tabLayout && item.enabled && item.key.canBeTabbed
    var showGroupSheet by rememberSaveable { mutableStateOf(false) }
    val groupLabel = item.tabGroup?.let { stringResource(Res.string.settings_meta_tab_group_format, it) }

    SegmentedListItem(
        selected = selected,
        onClick = { onEnabledChange(!item.enabled) },
        // Only the selected shape differs, so pressing or focusing a row does not re-round it
        // while a dragged one takes the group's outer corner on every edge.
        shapes = ListItemDefaults.shapes(
            shape = shape,
            selectedShape = RoundedCornerShape(OuterCorner),
            pressedShape = shape,
            focusedShape = shape,
            hoveredShape = shape,
        ),
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        leadingContent = {
            Icon(
                imageVector = if (item.enabled) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                contentDescription = if (item.enabled) {
                    stringResource(Res.string.settings_homescreen_visible)
                } else {
                    stringResource(Res.string.settings_homescreen_hidden)
                },
                tint = if (item.enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        },
        supportingContent = { Text(stringResource(item.key.descriptionRes)) },
        trailingContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (canPickGroup) {
                    TextButton(onClick = { showGroupSheet = true }) {
                        Text(
                            text = groupLabel ?: stringResource(Res.string.settings_meta_none),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                IconButton(
                    modifier = with(dragHandleScope) {
                        Modifier.draggableHandle(
                            onDragStarted = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDragStopped = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                        )
                    },
                    onClick = {},
                ) {
                    Icon(
                        Icons.Rounded.Menu,
                        contentDescription = stringResource(Res.string.action_reorder),
                    )
                }
            }
        },
    ) {
        Text(stringResource(item.key.titleRes))
    }

    if (showGroupSheet) {
        // Four choices with a rule about which are still free, which is a list, not a row of chips.
        val options = buildList {
            add(SingleChoiceOption(value = 0, label = stringResource(Res.string.settings_meta_none)))
            for (groupId in 1..3) {
                val isSelected = item.tabGroup == groupId
                add(
                    SingleChoiceOption(
                        value = groupId,
                        label = stringResource(Res.string.settings_meta_group_label, groupId),
                        enabled = isSelected || (groupCounts[groupId] ?: 0) < 3,
                    ),
                )
            }
        }
        SingleChoiceBottomSheet(
            title = stringResource(Res.string.settings_meta_tab_layout),
            options = options,
            isSelected = { it == (item.tabGroup ?: 0) },
            onSelected = { picked -> onTabGroupChange(picked.takeIf { it != 0 }) },
            onDismiss = { showGroupSheet = false },
        )
    }
}

private val MetaScreenBackgroundMode.labelRes: StringResource
    get() = when (this) {
        MetaScreenBackgroundMode.Normal -> Res.string.settings_meta_background_mode_normal
        MetaScreenBackgroundMode.Cinematic -> Res.string.settings_meta_background_mode_cinematic
        MetaScreenBackgroundMode.DominantColor -> Res.string.settings_meta_background_mode_dominant
    }

private val MetaScreenBackgroundMode.descriptionRes: StringResource
    get() = when (this) {
        MetaScreenBackgroundMode.Normal -> Res.string.settings_meta_background_mode_normal_description
        MetaScreenBackgroundMode.Cinematic -> Res.string.settings_meta_background_mode_cinematic_description
        MetaScreenBackgroundMode.DominantColor -> Res.string.settings_meta_background_mode_dominant_description
    }

/**
 * The episode card styles, as rows of one list, the way the continue watching styles are chosen.
 * Each row carries the miniature that used to be a tile, so the choice still shows what it does.
 */
@Composable
private fun MetaEpisodeCardStyleSelector(
    selectedStyle: MetaEpisodeCardStyle,
    onStyleSelected: (MetaEpisodeCardStyle) -> Unit,
) {
    val styles = MetaEpisodeCardStyle.entries

    Column(verticalArrangement = Arrangement.spacedBy(ListItemBetweenSpace)) {
        styles.forEachIndexed { index, style ->
            SelectableListRow(
                selected = style == selectedStyle,
                onClick = { onStyleSelected(style) },
                headline = stringResource(style.labelRes),
                supporting = stringResource(style.descriptionRes),
                unselectedShape = segmentShape(index = index, count = styles.size),
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                leadingContent = {
                    MetaEpisodeCardStylePreview(
                        style = style,
                        modifier = Modifier.size(width = 100.dp, height = 92.dp),
                    )
                },
            )
        }
    }
}

private val MetaEpisodeCardStyle.labelRes: StringResource
    get() = when (this) {
        MetaEpisodeCardStyle.Horizontal -> Res.string.settings_meta_episode_style_horizontal
        MetaEpisodeCardStyle.List -> Res.string.settings_meta_episode_style_list
    }

private val MetaEpisodeCardStyle.descriptionRes: StringResource
    get() = when (this) {
        MetaEpisodeCardStyle.Horizontal -> Res.string.settings_meta_episode_style_horizontal_description
        MetaEpisodeCardStyle.List -> Res.string.settings_meta_episode_style_list_description
    }

private val MetaScreenSectionKey.titleRes: StringResource
    get() = when (this) {
        MetaScreenSectionKey.ACTIONS -> Res.string.settings_meta_actions
        MetaScreenSectionKey.OVERVIEW -> Res.string.settings_meta_overview
        MetaScreenSectionKey.PRODUCTION -> Res.string.settings_meta_production
        MetaScreenSectionKey.CAST -> Res.string.settings_meta_cast
        MetaScreenSectionKey.COMMENTS -> Res.string.settings_meta_comments
        MetaScreenSectionKey.TRAILERS -> Res.string.settings_meta_trailers
        MetaScreenSectionKey.EPISODES -> Res.string.settings_meta_episodes
        MetaScreenSectionKey.DETAILS -> Res.string.settings_meta_details
        MetaScreenSectionKey.COLLECTION -> Res.string.settings_meta_collection
        MetaScreenSectionKey.MORE_LIKE_THIS -> Res.string.settings_meta_more_like_this
    }

private val MetaScreenSectionKey.descriptionRes: StringResource
    get() = when (this) {
        MetaScreenSectionKey.ACTIONS -> Res.string.settings_meta_actions_description
        MetaScreenSectionKey.OVERVIEW -> Res.string.settings_meta_overview_description
        MetaScreenSectionKey.PRODUCTION -> Res.string.settings_meta_production_description
        MetaScreenSectionKey.CAST -> Res.string.settings_meta_cast_description
        MetaScreenSectionKey.COMMENTS -> Res.string.settings_meta_comments_description
        MetaScreenSectionKey.TRAILERS -> Res.string.settings_meta_trailers_description
        MetaScreenSectionKey.EPISODES -> Res.string.settings_meta_episodes_description
        MetaScreenSectionKey.DETAILS -> Res.string.settings_meta_details_description
        MetaScreenSectionKey.COLLECTION -> Res.string.settings_meta_collection_description
        MetaScreenSectionKey.MORE_LIKE_THIS -> Res.string.settings_meta_more_like_this_description
    }

@Composable
private fun MetaEpisodeCardStylePreview(
    style: MetaEpisodeCardStyle,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (style) {
            MetaEpisodeCardStyle.Horizontal -> {
                Box(
                    modifier = Modifier
                        .width(96.dp)
                        .height(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                            .align(Alignment.BottomCenter)
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.36f)),
                    )
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .height(7.dp)
                            .align(Alignment.TopStart)
                            .padding(start = 6.dp, top = 6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.26f)),
                    )
                    Box(
                        modifier = Modifier
                            .width(72.dp)
                            .height(6.dp)
                            .align(Alignment.BottomStart)
                            .padding(start = 8.dp, bottom = 8.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)),
                    )
                }
            }

            MetaEpisodeCardStyle.List -> {
                Row(
                    modifier = Modifier
                        .width(96.dp)
                        .height(58.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f)),
                ) {
                    Box(
                        modifier = Modifier
                            .width(34.dp)
                            .height(58.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.82f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.20f)),
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.52f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)),
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                        )
                    }
                }
            }
        }
    }
}
