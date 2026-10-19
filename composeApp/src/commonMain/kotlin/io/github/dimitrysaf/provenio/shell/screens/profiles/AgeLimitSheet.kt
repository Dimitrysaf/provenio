package io.github.dimitrysaf.provenio.shell.screens.profiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explicit
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.core.profiles.AgeRatingSystems
import io.github.dimitrysaf.provenio.core.profiles.AgeRatingSystem
import io.github.dimitrysaf.provenio.shell.components.ExpandableListHeader
import io.github.dimitrysaf.provenio.shell.components.SelectedCheck
import io.github.dimitrysaf.provenio.shell.components.selectableListRowColors
import io.github.dimitrysaf.provenio.shell.components.selectableListRowShapes
import io.github.dimitrysaf.provenio.core.profiles.ProfileAgeLimit
import io.github.dimitrysaf.provenio.core.profiles.ageRatingKey
import io.github.dimitrysaf.provenio.core.metadata.tmdb.TmdbApiKey
import io.github.dimitrysaf.provenio.shell.components.BottomSheetBodyMargin
import io.github.dimitrysaf.provenio.shell.components.EmptyState
import io.github.dimitrysaf.provenio.shell.components.ModalSheet
import io.github.dimitrysaf.provenio.shell.components.SheetHeader
import io.github.dimitrysaf.provenio.shell.components.SheetNavigation
import io.github.dimitrysaf.provenio.shell.components.dismissBottomSheet
import io.github.dimitrysaf.provenio.shell.components.safeBottomPadding
import io.github.dimitrysaf.provenio.shell.screens.settings.ListItemBetweenSpace
import io.github.dimitrysaf.provenio.shell.screens.settings.SettingsList
import io.github.dimitrysaf.provenio.shell.screens.settings.segmentShape
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.*

/**
 * Picks a profile's age limit: the ratings it may watch, as a list under each rating board, from
 * as many boards as wanted, and whether titles without a rating are allowed. Done with nothing
 * checked leaves the profile without a limit.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun AgeLimitSheet(
    limit: ProfileAgeLimit?,
    onDone: (ProfileAgeLimit?) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var allowed by remember { mutableStateOf(limit?.allowedRatings.orEmpty()) }
    var allowUnrated by remember { mutableStateOf(limit?.allowUnrated ?: false) }
    var expanded by remember {
        mutableStateOf(
            AgeRatingSystems
                .filter { system -> system.ratings.any { ageRatingKey(system.country, it) in allowed } }
                .map { it.country }
                .toSet(),
        )
    }
    var showPicker by remember { mutableStateOf(limit != null || TmdbApiKey.current().isNotBlank()) }
    val close: () -> Unit = { scope.launch { dismissBottomSheet(sheetState, onDismiss) } }
    val finish: (ProfileAgeLimit?) -> Unit = { result ->
        scope.launch {
            dismissBottomSheet(sheetState) {
                onDone(result)
                onDismiss()
            }
        }
    }

    ModalSheet(onDismissRequest = close, sheetState = sheetState, fullHeight = true) {
        SheetHeader(
            title = stringResource(Res.string.profile_age_limit_title),
            navigation = SheetNavigation.Close,
            onNavigate = close,
        )
        if (!showPicker) {
            EmptyState(
                icon = Icons.Rounded.Explicit,
                title = stringResource(Res.string.profile_age_limit_no_ratings_title),
                message = stringResource(Res.string.profile_age_limit_no_ratings_message),
                actionLabel = stringResource(Res.string.profile_age_limit_continue),
                onActionClick = { showPicker = true },
                modifier = Modifier.padding(
                    start = BottomSheetBodyMargin,
                    end = BottomSheetBodyMargin,
                    top = 16.dp,
                    bottom = safeBottomPadding(24.dp),
                ),
            )
            return@ModalSheet
        }
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = BottomSheetBodyMargin),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = stringResource(Res.string.profile_age_limit_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SettingsList {
                switchRow(
                    title = stringResource(Res.string.profile_age_limit_unrated),
                    description = stringResource(Res.string.profile_age_limit_unrated_description),
                    checked = { allowUnrated },
                    onCheckedChange = { allowUnrated = it },
                )
            }
            // One segmented list: each board opens onto its ratings, as a language does onto its
            // subtitles in the player.
            val rows = buildList {
                AgeRatingSystems.forEach { system ->
                    add(AgeLimitRow.Board(system))
                    if (system.country in expanded) {
                        system.ratings.forEach { add(AgeLimitRow.Rating(system, it)) }
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(ListItemBetweenSpace)) {
                rows.forEachIndexed { index, row ->
                    val shapes = selectableListRowShapes(segmentShape(index = index, count = rows.size))
                    when (row) {
                        is AgeLimitRow.Board -> {
                            val open = row.system.country in expanded
                            ExpandableListHeader(
                                title = stringResource(row.system.labelRes),
                                expanded = open,
                                shapes = shapes,
                                onClick = {
                                    expanded = if (open) expanded - row.system.country else expanded + row.system.country
                                },
                                modifier = Modifier.fillMaxWidth(),
                                count = row.system.ratings
                                    .count { ageRatingKey(row.system.country, it) in allowed }
                                    .takeIf { it > 0 },
                            )
                        }
                        is AgeLimitRow.Rating -> {
                            val key = ageRatingKey(row.system.country, row.rating)
                            val selected = key in allowed
                            SegmentedListItem(
                                selected = selected,
                                onClick = { allowed = if (selected) allowed - key else allowed + key },
                                shapes = shapes,
                                colors = selectableListRowColors(MaterialTheme.colorScheme.surfaceContainer),
                                modifier = Modifier.fillMaxWidth(),
                                trailingContent = if (selected) {
                                    { SelectedCheck() }
                                } else {
                                    null
                                },
                            ) {
                                Text(row.rating)
                            }
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = BottomSheetBodyMargin)
                .padding(top = 16.dp, bottom = safeBottomPadding(16.dp)),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (limit != null) {
                TextButton(onClick = { finish(null) }) {
                    Text(stringResource(Res.string.profile_age_limit_remove))
                }
            }
            Button(
                onClick = {
                    finish(
                        allowed.takeIf { it.isNotEmpty() }?.let {
                            ProfileAgeLimit(allowedRatings = it, allowUnrated = allowUnrated)
                        },
                    )
                },
            ) {
                Text(stringResource(Res.string.action_done))
            }
        }
    }
}

private sealed interface AgeLimitRow {
    class Board(val system: AgeRatingSystem) : AgeLimitRow
    class Rating(val system: AgeRatingSystem, val rating: String) : AgeLimitRow
}
