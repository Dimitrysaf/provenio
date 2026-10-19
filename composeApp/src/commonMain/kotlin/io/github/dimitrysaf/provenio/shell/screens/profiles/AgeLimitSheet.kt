package io.github.dimitrysaf.provenio.shell.screens.profiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Explicit
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import io.github.dimitrysaf.provenio.core.profiles.ProfileAgeLimit
import io.github.dimitrysaf.provenio.core.profiles.ageRatingKey
import io.github.dimitrysaf.provenio.core.metadata.tmdb.TmdbApiKey
import io.github.dimitrysaf.provenio.shell.components.BottomSheetBodyMargin
import io.github.dimitrysaf.provenio.shell.components.EmptyState
import io.github.dimitrysaf.provenio.shell.components.ListSubheader
import io.github.dimitrysaf.provenio.shell.components.ModalSheet
import io.github.dimitrysaf.provenio.shell.components.SheetHeader
import io.github.dimitrysaf.provenio.shell.components.SheetNavigation
import io.github.dimitrysaf.provenio.shell.components.dismissBottomSheet
import io.github.dimitrysaf.provenio.shell.components.safeBottomPadding
import io.github.dimitrysaf.provenio.shell.screens.settings.SettingsList
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.*

/**
 * Picks a profile's age limit: the ratings it may watch, as chips under each rating board, from
 * as many boards as wanted, and whether titles without a rating are allowed. Done with nothing
 * checked leaves the profile without a limit.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
            AgeRatingSystems.forEach { system ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ListSubheader(text = stringResource(system.labelRes))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        system.ratings.forEach { rating ->
                            val key = ageRatingKey(system.country, rating)
                            val selected = key in allowed
                            FilterChip(
                                selected = selected,
                                onClick = { allowed = if (selected) allowed - key else allowed + key },
                                label = { Text(rating) },
                                leadingIcon = if (selected) {
                                    {
                                        Icon(imageVector = Icons.Rounded.Check, contentDescription = null)
                                    }
                                } else {
                                    null
                                },
                            )
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
