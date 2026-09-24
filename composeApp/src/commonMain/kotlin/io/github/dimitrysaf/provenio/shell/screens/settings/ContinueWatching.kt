package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.shell.components.SingleChoiceBottomSheet
import io.github.dimitrysaf.provenio.shell.components.SingleChoiceOption
import io.github.dimitrysaf.provenio.shell.components.SelectableListRow
import io.github.dimitrysaf.provenio.shell.screens.home.components.ContinueWatchingStylePreview
import io.github.dimitrysaf.provenio.core.watch.progress.ContinueWatchingPreferencesRepository
import io.github.dimitrysaf.provenio.core.watch.progress.ContinueWatchingSectionStyle
import io.github.dimitrysaf.provenio.core.watch.progress.ContinueWatchingSortMode
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.settings_continue_watching_blur_next_up_description
import provenio.composeapp.generated.resources.settings_continue_watching_blur_next_up_title
import provenio.composeapp.generated.resources.settings_continue_watching_show_unaired_next_up_description
import provenio.composeapp.generated.resources.settings_continue_watching_show_unaired_next_up_title
import provenio.composeapp.generated.resources.settings_continue_watching_section_card_style
import provenio.composeapp.generated.resources.settings_continue_watching_section_sort_order
import provenio.composeapp.generated.resources.settings_continue_watching_section_up_next_behavior
import provenio.composeapp.generated.resources.settings_continue_watching_section_visibility
import provenio.composeapp.generated.resources.settings_continue_watching_show_description
import provenio.composeapp.generated.resources.settings_continue_watching_show_title
import provenio.composeapp.generated.resources.settings_continue_watching_sort_mode_default
import provenio.composeapp.generated.resources.settings_continue_watching_sort_mode_default_desc
import provenio.composeapp.generated.resources.settings_continue_watching_sort_mode_streaming
import provenio.composeapp.generated.resources.settings_continue_watching_sort_mode_streaming_desc
import provenio.composeapp.generated.resources.settings_continue_watching_sort_mode_split_upcoming
import provenio.composeapp.generated.resources.settings_continue_watching_sort_mode_split_upcoming_desc
import provenio.composeapp.generated.resources.settings_continue_watching_sort_mode_title
import provenio.composeapp.generated.resources.settings_continue_watching_style_card
import provenio.composeapp.generated.resources.settings_continue_watching_style_card_description
import provenio.composeapp.generated.resources.settings_continue_watching_style_poster
import provenio.composeapp.generated.resources.settings_continue_watching_style_poster_description
import provenio.composeapp.generated.resources.settings_continue_watching_style_wide
import provenio.composeapp.generated.resources.settings_continue_watching_style_wide_description
import provenio.composeapp.generated.resources.settings_continue_watching_up_next_description
import provenio.composeapp.generated.resources.settings_continue_watching_up_next_title
import provenio.composeapp.generated.resources.settings_continue_watching_use_episode_thumbnails_description
import provenio.composeapp.generated.resources.settings_continue_watching_use_episode_thumbnails_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.continueWatchingSettingsContent(
    isTablet: Boolean,
    isVisible: Boolean,
    style: ContinueWatchingSectionStyle,
    upNextFromFurthestEpisode: Boolean,
    useEpisodeThumbnails: Boolean,
    showUnairedNextUp: Boolean,
    blurNextUp: Boolean,
    sortMode: ContinueWatchingSortMode,
) {
    item {
        SettingsSection(
            title = stringResource(Res.string.settings_continue_watching_section_visibility),
            isTablet = isTablet,
        ) {
            SettingsList {
                switchRow(
                    title = stringResource(Res.string.settings_continue_watching_show_title),
                    description = stringResource(Res.string.settings_continue_watching_show_description),
                    checked = { isVisible },
                    onCheckedChange = ContinueWatchingPreferencesRepository::setVisible,
                )
            }
        }
    }
    item {
        SettingsSection(
            title = stringResource(Res.string.settings_continue_watching_section_card_style),
            isTablet = isTablet,
        ) {
            ContinueWatchingStyleSelector(
                selectedStyle = style,
                onStyleSelected = ContinueWatchingPreferencesRepository::setStyle,
            )
        }
    }
    item {
        SettingsSection(
            title = stringResource(Res.string.settings_continue_watching_section_up_next_behavior),
            isTablet = isTablet,
        ) {
            SettingsList {
                if (style != ContinueWatchingSectionStyle.Poster) {
                    switchRow(
                        title = stringResource(Res.string.settings_continue_watching_use_episode_thumbnails_title),
                        description = stringResource(Res.string.settings_continue_watching_use_episode_thumbnails_description),
                        checked = { useEpisodeThumbnails },
                        onCheckedChange = ContinueWatchingPreferencesRepository::setUseEpisodeThumbnails,
                    )
                }
                switchRow(
                    title = stringResource(Res.string.settings_continue_watching_up_next_title),
                    description = stringResource(Res.string.settings_continue_watching_up_next_description),
                    checked = { upNextFromFurthestEpisode },
                    onCheckedChange = ContinueWatchingPreferencesRepository::setUpNextFromFurthestEpisode,
                )
                switchRow(
                    title = stringResource(Res.string.settings_continue_watching_show_unaired_next_up_title),
                    description = stringResource(Res.string.settings_continue_watching_show_unaired_next_up_description),
                    checked = { showUnairedNextUp },
                    onCheckedChange = ContinueWatchingPreferencesRepository::setShowUnairedNextUp,
                )
                if (style != ContinueWatchingSectionStyle.Poster && useEpisodeThumbnails) {
                    switchRow(
                        title = stringResource(Res.string.settings_continue_watching_blur_next_up_title),
                        description = stringResource(Res.string.settings_continue_watching_blur_next_up_description),
                        checked = { blurNextUp },
                        onCheckedChange = ContinueWatchingPreferencesRepository::setBlurNextUp,
                    )
                }
            }
        }
    }
    item {
        var showSortModeSheet by remember { mutableStateOf(false) }
        SettingsSection(
            title = stringResource(Res.string.settings_continue_watching_section_sort_order),
            isTablet = isTablet,
        ) {
            SettingsList {
                val currentModeLabel = stringResource(
                    when (sortMode) {
                        ContinueWatchingSortMode.DEFAULT -> Res.string.settings_continue_watching_sort_mode_default
                        ContinueWatchingSortMode.STREAMING_STYLE -> Res.string.settings_continue_watching_sort_mode_streaming
                        ContinueWatchingSortMode.SPLIT_UPCOMING -> Res.string.settings_continue_watching_sort_mode_split_upcoming
                    }
                )
                navigationRow(
                    title = stringResource(Res.string.settings_continue_watching_sort_mode_title),
                    description = currentModeLabel,
                    onClick = { showSortModeSheet = true },
                )
            }
        }

        if (showSortModeSheet) {
            ContinueWatchingSortModeDialog(
                currentMode = sortMode,
                onModeSelected = ContinueWatchingPreferencesRepository::setSortMode,
                onDismiss = { showSortModeSheet = false },
            )
        }
    }
}

/**
 * Choosing how a continue-watching card looks.
 *
 * Rows of one segmented group, like every other setting on this page, rather than three tiles in
 * a grid. Each row keeps its miniature, so the choice is still made by looking, but the name and
 * the description get a row's full width instead of two cramped lines under a thumbnail, and
 * selection is the spec's own: the chosen row takes the primary container and its corners morph
 * to the full radius.
 *
 * The three miniatures are different sizes, so each sits in the same box and centres in it,
 * keeping the text aligned down the column.
 */
@Composable
private fun ContinueWatchingStyleSelector(
    selectedStyle: ContinueWatchingSectionStyle,
    onStyleSelected: (ContinueWatchingSectionStyle) -> Unit,
) {
    val styles = ContinueWatchingSectionStyle.entries

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
                    ContinueWatchingStylePreview(
                        style = style,
                        modifier = Modifier.size(width = 100.dp, height = 92.dp),
                    )
                },
            )
        }
    }
}

private val ContinueWatchingSectionStyle.labelRes: StringResource
    get() = when (this) {
        ContinueWatchingSectionStyle.Card -> Res.string.settings_continue_watching_style_card
        ContinueWatchingSectionStyle.Wide -> Res.string.settings_continue_watching_style_wide
        ContinueWatchingSectionStyle.Poster -> Res.string.settings_continue_watching_style_poster
    }

private val ContinueWatchingSectionStyle.descriptionRes: StringResource
    get() = when (this) {
        ContinueWatchingSectionStyle.Card -> Res.string.settings_continue_watching_style_card_description
        ContinueWatchingSectionStyle.Wide -> Res.string.settings_continue_watching_style_wide_description
        ContinueWatchingSectionStyle.Poster -> Res.string.settings_continue_watching_style_poster_description
    }

@Composable
private fun ContinueWatchingSortModeDialog(
    currentMode: ContinueWatchingSortMode,
    onModeSelected: (ContinueWatchingSortMode) -> Unit,
    onDismiss: () -> Unit,
) {
    SingleChoiceBottomSheet(
        title = stringResource(Res.string.settings_continue_watching_sort_mode_title),
        options = listOf(
            SingleChoiceOption(
                value = ContinueWatchingSortMode.DEFAULT,
                label = stringResource(Res.string.settings_continue_watching_sort_mode_default),
                supportingText = stringResource(Res.string.settings_continue_watching_sort_mode_default_desc),
            ),
            SingleChoiceOption(
                value = ContinueWatchingSortMode.STREAMING_STYLE,
                label = stringResource(Res.string.settings_continue_watching_sort_mode_streaming),
                supportingText = stringResource(Res.string.settings_continue_watching_sort_mode_streaming_desc),
            ),
            SingleChoiceOption(
                value = ContinueWatchingSortMode.SPLIT_UPCOMING,
                label = stringResource(Res.string.settings_continue_watching_sort_mode_split_upcoming),
                supportingText = stringResource(Res.string.settings_continue_watching_sort_mode_split_upcoming_desc),
            ),
        ),
        isSelected = { it == currentMode },
        onSelected = onModeSelected,
        onDismiss = onDismiss,
    )
}
