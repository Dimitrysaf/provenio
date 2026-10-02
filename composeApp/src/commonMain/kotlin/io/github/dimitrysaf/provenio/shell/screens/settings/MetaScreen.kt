package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import provenio.composeapp.generated.resources.settings_meta_background_mode_cinematic_description
import provenio.composeapp.generated.resources.settings_meta_background_mode_cinematic
import provenio.composeapp.generated.resources.settings_meta_background_mode_normal_description
import provenio.composeapp.generated.resources.settings_meta_background_mode_normal
import provenio.composeapp.generated.resources.settings_meta_background_mode_description
import provenio.composeapp.generated.resources.settings_meta_background_mode
import io.github.dimitrysaf.provenio.core.metadata.MetaScreenBackgroundMode
import io.github.dimitrysaf.provenio.shell.components.SingleChoiceOption
import io.github.dimitrysaf.provenio.shell.components.SingleChoiceBottomSheet
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.core.build.AppFeaturePolicy
import io.github.dimitrysaf.provenio.core.build.TrailerPlaybackMode
import io.github.dimitrysaf.provenio.core.metadata.MetaScreenSectionItem
import io.github.dimitrysaf.provenio.core.metadata.MetaScreenSectionKey
import io.github.dimitrysaf.provenio.core.metadata.MetaScreenSettingsRepository
import io.github.dimitrysaf.provenio.core.metadata.MetaScreenSettingsUiState
import io.github.dimitrysaf.provenio.core.build.supportsPosterNavigationMotion
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.action_reset
import provenio.composeapp.generated.resources.settings_homescreen_hidden
import provenio.composeapp.generated.resources.settings_homescreen_visible
import provenio.composeapp.generated.resources.settings_meta_actions
import provenio.composeapp.generated.resources.settings_meta_actions_description
import provenio.composeapp.generated.resources.settings_meta_blur_unwatched_episodes
import provenio.composeapp.generated.resources.settings_meta_blur_unwatched_episodes_description
import provenio.composeapp.generated.resources.settings_meta_cast
import provenio.composeapp.generated.resources.settings_meta_cast_description
import provenio.composeapp.generated.resources.settings_meta_collection
import provenio.composeapp.generated.resources.settings_meta_collection_description
import provenio.composeapp.generated.resources.settings_meta_comments
import provenio.composeapp.generated.resources.settings_meta_comments_description
import provenio.composeapp.generated.resources.settings_meta_details
import provenio.composeapp.generated.resources.settings_meta_details_description
import provenio.composeapp.generated.resources.settings_meta_episodes
import provenio.composeapp.generated.resources.settings_meta_episodes_description
import provenio.composeapp.generated.resources.settings_meta_hero_trailer_playback
import provenio.composeapp.generated.resources.settings_meta_hero_trailer_playback_description
import provenio.composeapp.generated.resources.settings_meta_more_like_this
import provenio.composeapp.generated.resources.settings_meta_more_like_this_description
import provenio.composeapp.generated.resources.settings_meta_overview
import provenio.composeapp.generated.resources.settings_meta_overview_description
import provenio.composeapp.generated.resources.settings_meta_parents_guide
import provenio.composeapp.generated.resources.settings_meta_parents_guide_description
import provenio.composeapp.generated.resources.settings_meta_poster_transition
import provenio.composeapp.generated.resources.settings_meta_poster_transition_description
import provenio.composeapp.generated.resources.settings_meta_production
import provenio.composeapp.generated.resources.settings_meta_production_description
import provenio.composeapp.generated.resources.settings_meta_section_appearance
import provenio.composeapp.generated.resources.settings_meta_section_sections
import provenio.composeapp.generated.resources.settings_meta_trailers
import provenio.composeapp.generated.resources.settings_meta_trailers_description
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
            title = stringResource(Res.string.settings_meta_section_sections),
            isTablet = isTablet,
        ) {
            MetaSectionReorderableList(
                items = uiState.items,
                isTablet = isTablet,
            )
        }
    }
}

/**
 * The metadata page's sections, as one segmented list.
 *
 * Tapping a row shows or hides its section, which the eye at its end reports; holding a row
 * picks it up to reorder. Reset scrolls with the rows because it belongs to them: it
 * restores this list alone and leaves the rest of the page as it is.
 */
@Composable
private fun MetaSectionReorderableList(
    items: List<MetaScreenSectionItem>,
    isTablet: Boolean,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
    ) { from, to ->
        MetaScreenSettingsRepository.moveByIndex(from.index, to.index)
        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                        shape = shape,
                        onEnabledChange = { MetaScreenSettingsRepository.setEnabled(item.key, it) },
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
    shape: RoundedCornerShape,
    onEnabledChange: (Boolean) -> Unit,
    dragHandleScope: ReorderableCollectionItemScope,
) {
    val hapticFeedback = LocalHapticFeedback.current

    SegmentedListItem(
        // The whole row is the drag handle: holding it picks the section up, a tap shows or hides it.
        modifier = with(dragHandleScope) {
            Modifier.longPressDraggableHandle(
                onDragStarted = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                onDragStopped = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
            )
        },
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
        verticalAlignment = Alignment.CenterVertically,
        supportingContent = { Text(stringResource(item.key.descriptionRes)) },
        trailingContent = {
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
    ) {
        Text(stringResource(item.key.titleRes))
    }
}

private val MetaScreenBackgroundMode.labelRes: StringResource
    get() = when (this) {
        MetaScreenBackgroundMode.Normal -> Res.string.settings_meta_background_mode_normal
        MetaScreenBackgroundMode.Cinematic -> Res.string.settings_meta_background_mode_cinematic
    }

private val MetaScreenBackgroundMode.descriptionRes: StringResource
    get() = when (this) {
        MetaScreenBackgroundMode.Normal -> Res.string.settings_meta_background_mode_normal_description
        MetaScreenBackgroundMode.Cinematic -> Res.string.settings_meta_background_mode_cinematic_description
    }

private val MetaScreenSectionKey.titleRes: StringResource
    get() = when (this) {
        MetaScreenSectionKey.ACTIONS -> Res.string.settings_meta_actions
        MetaScreenSectionKey.OVERVIEW -> Res.string.settings_meta_overview
        MetaScreenSectionKey.PARENTS_GUIDE -> Res.string.settings_meta_parents_guide
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
        MetaScreenSectionKey.PARENTS_GUIDE -> Res.string.settings_meta_parents_guide_description
        MetaScreenSectionKey.PRODUCTION -> Res.string.settings_meta_production_description
        MetaScreenSectionKey.CAST -> Res.string.settings_meta_cast_description
        MetaScreenSectionKey.COMMENTS -> Res.string.settings_meta_comments_description
        MetaScreenSectionKey.TRAILERS -> Res.string.settings_meta_trailers_description
        MetaScreenSectionKey.EPISODES -> Res.string.settings_meta_episodes_description
        MetaScreenSectionKey.DETAILS -> Res.string.settings_meta_details_description
        MetaScreenSectionKey.COLLECTION -> Res.string.settings_meta_collection_description
        MetaScreenSectionKey.MORE_LIKE_THIS -> Res.string.settings_meta_more_like_this_description
    }
