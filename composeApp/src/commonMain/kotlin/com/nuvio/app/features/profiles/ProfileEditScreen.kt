package com.nuvio.app.features.profiles

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.nuvio.app.shell.components.NuvioScreen
import com.nuvio.app.shell.components.TextPromptDialog
import com.nuvio.app.shell.components.nuvioSafeBottomPadding
import com.nuvio.app.features.membership.CosmeticEntitlement
import com.nuvio.app.features.membership.MemberAccessRepository
import com.nuvio.app.features.membership.ProfileBackgroundRepository
import com.nuvio.app.features.settings.SettingsList
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/**
 * The profile editor. [embedded] hosts it inside a layout pane instead of as a full window: the
 * screen chrome is dropped for a pane header, since a pane is already framed by the layout around
 * it. Compact and medium windows have no room for a second pane, so they keep the full-window
 * form.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProfileEditScreen(
    profile: NuvioProfile? = null,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    embedded: Boolean = false,
) {
    val isNew = profile == null
    val scope = rememberCoroutineScope()
    val profileState by ProfileRepository.state.collectAsStateWithLifecycle()
    val currentProfile = remember(profile?.profileIndex, profileState.profiles, profile) {
        profile?.let { snapshot ->
            profileState.profiles.find { it.profileIndex == snapshot.profileIndex } ?: snapshot
        }
    }
    val fallbackColorHex = currentProfile?.avatarColorHex ?: PROFILE_COLORS.first()

    // A new profile is named rather than blank, so it can be saved as it stands and named later
    // by whoever cares to.
    val defaultProfileName = stringResource(Res.string.profile_default_name)
    var name by rememberSaveable { mutableStateOf(currentProfile?.name ?: defaultProfileName) }
    var selectedAvatarId by rememberSaveable { mutableStateOf(currentProfile?.avatarId) }
    var avatarUrl by rememberSaveable { mutableStateOf(currentProfile?.avatarUrl.orEmpty()) }
    var selectedBackgroundId by rememberSaveable { mutableStateOf(currentProfile?.profileBackgroundId) }
    var selectedBackgroundUrl by rememberSaveable { mutableStateOf(currentProfile?.profileBackgroundUrl) }
    var usesPrimaryAddons by rememberSaveable { mutableStateOf(currentProfile?.usesPrimaryAddons ?: false) }
    var isSaving by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showPinSetup by remember { mutableStateOf(false) }
    var showPinClear by remember { mutableStateOf(false) }
    var showNameDialog by rememberSaveable { mutableStateOf(false) }
    var showAvatarDialog by rememberSaveable { mutableStateOf(false) }
    val memberAccess by remember {
        MemberAccessRepository.ensureStarted()
        MemberAccessRepository.access
    }.collectAsStateWithLifecycle()
    val backgroundCatalog by ProfileBackgroundRepository.catalog.collectAsStateWithLifecycle()
    val canChooseBackground = !isNew && memberAccess.entitlements.includes(CosmeticEntitlement.PROFILE_BACKGROUNDS)

    val avatars by AvatarRepository.avatars.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        AvatarRepository.refreshAvatars()
    }
    LaunchedEffect(canChooseBackground) {
        if (canChooseBackground) ProfileBackgroundRepository.preloadLandscapeImages()
    }
    LaunchedEffect(isNew, avatars, selectedAvatarId, avatarUrl) {
        if (isNew && avatarUrl.isBlank() && selectedAvatarId == null && avatars.isNotEmpty()) {
            selectedAvatarId = avatars.first().id
        }
    }

    val customAvatarUrl = remember(avatarUrl) { normalizedAvatarUrl(avatarUrl) }
    val avatarUrlIsInvalid = avatarUrl.isNotBlank() && customAvatarUrl == null
    val selectedAvatarItem = remember(selectedAvatarId, avatars) {
        selectedAvatarId?.let { id -> avatars.find { it.id == id } }
    }
    val visibleAvatarItem = if (customAvatarUrl == null) selectedAvatarItem else null
    val previewAccent = remember(visibleAvatarItem, fallbackColorHex) {
        parseHexColor(visibleAvatarItem?.bgColor ?: fallbackColorHex)
    }

    val title = if (isNew) {
        stringResource(Res.string.profile_edit_add_title)
    } else {
        stringResource(Res.string.profile_edit_edit_title)
    }

    val body: LazyListScope.() -> Unit = {
        item {
            ProfileIdentityPreview(
                name = name,
                isNew = isNew,
                selectedAvatar = visibleAvatarItem,
                customAvatarUrl = customAvatarUrl,
                accentColor = previewAccent,
                identifier = (currentProfile?.profileIndex ?: 0).toString(),
                onEditName = { showNameDialog = true },
                onEditAvatar = { showAvatarDialog = true },
            )
        }

        item {
            // The profile's options are settings, so they are the app's settings list: one
            // segmented group, each option a row, the PIN among them rather than a button under
            // a heading that said the same thing the row now says.
            SettingsList {
                switchRow(
                    title = stringResource(Res.string.profile_use_primary_addons),
                    description = stringResource(Res.string.profile_use_primary_addons_description),
                    checked = { usesPrimaryAddons },
                    onCheckedChange = { usesPrimaryAddons = it },
                )

                if (!isNew) {
                    val pinEnabled = currentProfile?.pinEnabled == true
                    navigationRow(
                        title = if (pinEnabled) {
                            stringResource(Res.string.profile_remove_pin_lock)
                        } else {
                            stringResource(Res.string.profile_set_pin_lock)
                        },
                        description = if (pinEnabled) {
                            stringResource(Res.string.profile_security_pin_enabled)
                        } else {
                            stringResource(Res.string.profile_security_pin_disabled)
                        },
                        icon = if (pinEnabled) Icons.Outlined.LockOpen else Icons.Outlined.Lock,
                        onClick = {
                            if (pinEnabled) showPinClear = true else showPinSetup = true
                        },
                    )
                }
            }
        }

        if (canChooseBackground) {
                item {
                ProfileEditSection(
                    title = stringResource(Res.string.profile_choose_background),
                    description = stringResource(Res.string.profile_background_member_note),
                ) {
                    ProfileBackgroundPicker(
                        backgrounds = backgroundCatalog,
                        selectedBackgroundId = selectedBackgroundId,
                        selectedBackgroundUrl = selectedBackgroundUrl,
                        customBackgroundUrl = currentProfile?.profileBackgroundUrl,
                        standardBackgroundColor = previewAccent,
                        onSelectionChange = { id, url ->
                            selectedBackgroundId = id
                            selectedBackgroundUrl = url
                        },
                    )
                }
            }
        }

    }

    val canSave = name.isNotBlank() && !avatarUrlIsInvalid && !isSaving
    val canDelete = !isNew && (currentProfile?.profileIndex ?: 0) > 1
    val onSave: () -> Unit = {
        isSaving = true
        scope.launch {
            val avatarColorHex = visibleAvatarItem?.bgColor ?: fallbackColorHex
            if (isNew) {
                ProfileRepository.createProfile(
                    name = name,
                    avatarColorHex = avatarColorHex,
                    avatarId = if (customAvatarUrl == null) selectedAvatarId else null,
                    avatarUrl = customAvatarUrl,
                    usesPrimaryAddons = usesPrimaryAddons,
                )
            } else {
                ProfileRepository.updateProfile(
                    profileIndex = currentProfile!!.profileIndex,
                    name = name,
                    avatarColorHex = avatarColorHex,
                    avatarId = if (customAvatarUrl == null) selectedAvatarId else null,
                    avatarUrl = customAvatarUrl,
                    profileBackgroundId = selectedBackgroundId,
                    profileBackgroundUrl = selectedBackgroundUrl,
                    usesPrimaryAddons = usesPrimaryAddons,
                )
            }
            isSaving = false
            onSaved()
        }
    }

    if (embedded) {
        // Laid out as a standard side sheet: 24dp start/end padding, 12dp between the top
        // elements, a single divider under the headline, and the actions docked in a 72dp
        // region so they stay put while the content scrolls.
        // The 24dp start/end padding belongs to each part, not the sheet, so the divider above
        // the docked actions runs the full width of the container instead of being inset with
        // the content.
        Column(modifier = modifier.fillMaxSize()) {
            ProfileEditPaneHeader(
                title = title,
                onClose = onBack,
                modifier = Modifier.padding(horizontal = 24.dp),
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                content = body,
            )

            HorizontalDivider()

            ProfileEditActions(
                isNew = isNew,
                isSaving = isSaving,
                canSave = canSave,
                canDelete = canDelete,
                onSave = onSave,
                onDelete = { showDeleteConfirm = true },
            )
        }
    } else {
        NuvioScreen(
            title = title,
            modifier = modifier,
            onBack = onBack,
            bottomBar = {
                ProfileEditDockedActions(
                    isNew = isNew,
                    isSaving = isSaving,
                    canSave = canSave,
                    canDelete = canDelete,
                    onSave = onSave,
                    onDelete = { showDeleteConfirm = true },
                )
            },
            content = body,
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(Res.string.profile_delete_title)) },
            text = {
                Text(
                    stringResource(
                        Res.string.profile_delete_confirm_message,
                        currentProfile?.name.orEmpty(),
                    ),
                )
            },
            // Deleting a profile takes everything in it, so the action that does it is the one
            // carrying the error colour, as every other destructive confirm in the app does.
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        scope.launch {
                            currentProfile?.let { ProfileRepository.deleteProfile(it.profileIndex) }
                            onBack()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(Res.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(Res.string.action_cancel))
                }
            },
        )
    }

    if (showPinSetup && currentProfile != null) {
        PinSetupSheet(
            profileIndex = currentProfile.profileIndex,
            hasExistingPin = currentProfile.pinEnabled,
            onDone = {
                showPinSetup = false
            },
            onDismiss = { showPinSetup = false },
        )
    }

    if (showNameDialog) {
        TextPromptDialog(
            title = stringResource(Res.string.profile_name_placeholder),
            label = stringResource(Res.string.profile_name_placeholder),
            initialValue = name,
            onConfirm = { value ->
                name = value
                showNameDialog = false
            },
            onDismiss = { showNameDialog = false },
        )
    }

    if (showAvatarDialog) {
        ProfileAvatarDialog(
            avatars = avatars,
            selectedAvatarId = selectedAvatarId.takeIf { customAvatarUrl == null },
            avatarUrl = avatarUrl,
            urlIsInvalid = avatarUrlIsInvalid,
            onAvatarSelected = { avatar ->
                avatarUrl = ""
                selectedAvatarId = avatar.id
            },
            onAvatarUrlChange = { value ->
                avatarUrl = value
                if (value.isNotBlank()) {
                    selectedAvatarId = null
                }
            },
            onDismiss = { showAvatarDialog = false },
        )
    }

    if (showPinClear && currentProfile != null) {
        PinEntrySheet(
            profileName = stringResource(Res.string.profile_remove_pin_for, currentProfile.name),
            onVerify = { pin -> ProfileRepository.clearPin(currentProfile.profileIndex, pin) },
            onVerified = {
                showPinClear = false
            },
            onDismiss = {
                showPinClear = false
            },
        )
    }
}

/**
 * The side sheet's docked actions: a 72dp region with 16dp above and 24dp below, aligned left,
 * outside the scrolling content so the primary action is always reachable.
 *
 * A sheet is wide enough to seat both actions on one line, so they sit side by side. A narrow
 * sheet, or a long label in another language, would leave the second one no room, so the row flows
 * and the overflowing action wraps to the line below rather than being squeezed or clipped.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
private fun ProfileEditActions(
    isNew: Boolean,
    isSaving: Boolean,
    canSave: Boolean,
    canDelete: Boolean,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(onClick = onSave, enabled = canSave) {
            if (isSaving) {
                LoadingIndicator(
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(
                    text = if (isNew) {
                        stringResource(Res.string.profile_create_profile)
                    } else {
                        stringResource(Res.string.collections_editor_save_changes)
                    },
                )
            }
        }

        if (canDelete) {
            // The error container pair is the M3 role for a destructive action's container.
            Button(
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            ) {
                Text(stringResource(Res.string.profile_delete_title))
            }
        }
    }
}

/**
 * The full-window form's docked actions. A side sheet's actions sit in a fixed region because the
 * sheet is short; a full window's form can run past the fold, so the same region is pinned to the
 * bottom of the window and the primary action spans its width: the widest target, and reachable at
 * any scroll position.
 *
 * The bar carries the screen's container colour so content scrolls out of sight beneath it, and
 * owns the navigation bar inset, since it is the bottom-most element.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ProfileEditDockedActions(
    isNew: Boolean,
    isSaving: Boolean,
    canSave: Boolean,
    canDelete: Boolean,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        HorizontalDivider()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = ScreenHorizontalPadding,
                    end = ScreenHorizontalPadding,
                    top = 16.dp,
                    bottom = nuvioSafeBottomPadding(16.dp),
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onSave,
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isSaving) {
                    LoadingIndicator(
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(
                        text = if (isNew) {
                            stringResource(Res.string.profile_create_profile)
                        } else {
                            stringResource(Res.string.collections_editor_save_changes)
                        },
                    )
                }
            }

            if (canDelete) {
                // The error container pair is the M3 role for a destructive action's container.
                Button(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                ) {
                    Text(stringResource(Res.string.profile_delete_title))
                }
            }
        }
    }
}

/**
 * A pane's own header. The pane is already bounded by the layout, so this is a title and a close
 * affordance rather than a back-navigating app bar.
 */
@Composable
private fun ProfileEditPaneHeader(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        IconButton(
            onClick = onClose,
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(Res.string.action_cancel),
            )
        }
    }
}

/** Where an avatar comes from: the catalog, or a link the person supplies. */
private enum class ProfileAvatarSource {
    CATALOG,
    URL,
}

/**
 * The avatar picker.
 *
 * Choosing an avatar is a short, self-contained task with one way out, so it is a dialog rather
 * than a block of the form: the editor stays about the profile, and the two places an avatar can
 * come from are one choice between them rather than two stacked sections.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ProfileAvatarDialog(
    avatars: List<AvatarCatalogItem>,
    selectedAvatarId: String?,
    avatarUrl: String,
    urlIsInvalid: Boolean,
    onAvatarSelected: (AvatarCatalogItem) -> Unit,
    onAvatarUrlChange: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sources = ProfileAvatarSource.entries
    // A link already in hand is the state the dialog opens in; otherwise the catalog, which is
    // what most people want and what the editor defaults to.
    var source by rememberSaveable {
        mutableStateOf(
            if (avatarUrl.isNotBlank()) ProfileAvatarSource.URL else ProfileAvatarSource.CATALOG,
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.profile_avatar_options)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    sources.forEachIndexed { index, entry ->
                        SegmentedButton(
                            selected = entry == source,
                            onClick = { source = entry },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = sources.size,
                            ),
                        ) {
                            Text(profileAvatarSourceLabel(entry))
                        }
                    }
                }

                when (source) {
                    ProfileAvatarSource.URL -> OutlinedTextField(
                        value = avatarUrl,
                        onValueChange = onAvatarUrlChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(Res.string.profile_custom_avatar_url_placeholder)) },
                        leadingIcon = { Icon(Icons.Outlined.Link, contentDescription = null) },
                        singleLine = true,
                        isError = urlIsInvalid,
                        supportingText = if (urlIsInvalid) {
                            { Text(stringResource(Res.string.profile_avatar_url_invalid)) }
                        } else {
                            null
                        },
                    )

                    ProfileAvatarSource.CATALOG -> if (avatars.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            LoadingIndicator()
                        }
                    } else {
                        AvatarPicker(
                            avatars = avatars,
                            selectedAvatarId = selectedAvatarId,
                            onAvatarSelected = onAvatarSelected,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_done))
            }
        },
    )
}

@Composable
private fun profileAvatarSourceLabel(source: ProfileAvatarSource): String = when (source) {
    ProfileAvatarSource.CATALOG -> stringResource(Res.string.profile_avatar_source_catalog)
    ProfileAvatarSource.URL -> stringResource(Res.string.profile_avatar_source_url)
}

/**
 * A titled block of the form. The editor sits inside a pane that already provides containment, so
 * sections are separated by spacing and a title rather than by nested card surfaces.
 */
@Composable
private fun ProfileEditSection(
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))
        content()
    }
}

/**
 * Who the profile is: the avatar and the name, each with the pencil that changes it.
 *
 * Both are edited in a dialog rather than in the form below, so the two things a person actually
 * recognises a profile by are also the two controls, and there is nothing under them restating
 * what the preview already shows.
 */
@Composable
private fun ProfileIdentityPreview(
    name: String,
    isNew: Boolean,
    selectedAvatar: AvatarCatalogItem?,
    customAvatarUrl: String?,
    accentColor: Color,
    identifier: String,
    onEditName: () -> Unit,
    onEditAvatar: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            ProfileAvatar(
                size = AvatarPreviewSize,
                imageUrl = customAvatarUrl ?: selectedAvatar?.let { avatarImageUrl(it) },
                monogram = name,
                identifier = identifier,
                contentDescription = selectedAvatar?.displayName ?: name,
                containerColor = selectedAvatar?.let { accentColor },
            )
            // The pencil sits on the avatar it edits, so what it changes needs no label, and no
            // container either: a filled button over the face would hide what it is pointing at.
            IconButton(
                onClick = onEditAvatar,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(AvatarEditBadgeSize),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(Res.string.profile_edit_avatar),
                    tint = LetterTile.FontColor,
                    modifier = Modifier.size(AvatarEditBadgeIconSize),
                )
            }
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .clip(ShapeDefaults.Small)
                .clickable(onClick = onEditName)
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = name.ifBlank {
                    if (isNew) stringResource(Res.string.profile_new)
                    else stringResource(Res.string.profile_unnamed)
                },
                modifier = Modifier.weight(1f, fill = false),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = stringResource(Res.string.profile_edit_name),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(NameEditIconSize),
            )
        }
    }
}

/** The screen's own side margin, the same one `NuvioScreen` gives its content. */
private val ScreenHorizontalPadding = 16.dp

private val AvatarPreviewSize = 88.dp
private val AvatarEditBadgeSize = 40.dp
private val AvatarEditBadgeIconSize = 20.dp
private val NameEditIconSize = 20.dp

@Composable
fun PinSetupSheet(
    profileIndex: Int,
    hasExistingPin: Boolean,
    onDone: () -> Unit,
    onDismiss: () -> Unit,
) {
    var step by remember { mutableStateOf(if (hasExistingPin) "current" else "new") }
    var currentPin by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    when (step) {
        "current" -> PinEntrySheet(
            profileName = stringResource(Res.string.profile_enter_current_pin),
            onVerify = { pin -> ProfileRepository.verifyPin(profileIndex, pin) },
            onVerified = { pin ->
                currentPin = pin
                step = "new"
            },
            onDismiss = onDismiss,
        )

        "new" -> PinEntrySheet(
            profileName = stringResource(Res.string.profile_enter_new_pin),
            onVerify = { pin ->
                ProfileRepository.setPin(
                    profileIndex = profileIndex,
                    pin = pin,
                    currentPin = currentPin.ifEmpty { null },
                )
            },
            onVerified = {
                onDone()
            },
            onDismiss = onDismiss,
        )
    }
}
