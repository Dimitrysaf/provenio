package io.github.dimitrysaf.provenio.shell.screens.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.shell.components.SingleChoiceBottomSheet
import io.github.dimitrysaf.provenio.shell.components.SingleChoiceOption
import provenio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import io.github.dimitrysaf.provenio.core.profiles.AvatarCatalogItem
import io.github.dimitrysaf.provenio.core.profiles.MAX_PROFILES
import io.github.dimitrysaf.provenio.core.profiles.Profile

/**
 * Switching profile: the profiles as a list, in a sheet.
 *
 * A list of who could be watching is the app's single-select picker like any other, so it is the
 * same sheet, and the avatars ride the rows' leading slot. Making a new profile is the last row
 * rather than a control somewhere else, because from here it is one more thing to pick.
 *
 * A locked profile is not switched to from the list: the sheet closes and its PIN is asked for in
 * the same dialog the profile picker and the editor use.
 */
@Composable
internal fun ProfileSwitcherSheet(
    profiles: List<Profile>,
    avatars: List<AvatarCatalogItem>,
    activeProfileIndex: Int?,
    onProfileChosen: (Profile) -> Unit,
    onAddProfileRequested: () -> Unit,
    onDismiss: () -> Unit,
) {
    val addProfileLabel = stringResource(Res.string.compose_profile_add_profile)
    val options = buildList {
        profiles.forEach { profile ->
            add(
                SingleChoiceOption(
                    value = profile.profileIndex,
                    label = profile.name.ifBlank {
                        stringResource(Res.string.profile_label_number, profile.profileIndex)
                    },
                    leadingContent = {
                        ProfileRowAvatar(profile = profile, avatars = avatars)
                    },
                ),
            )
        }
        if (profiles.size < MAX_PROFILES) {
            add(
                SingleChoiceOption(
                    value = AddProfileValue,
                    label = addProfileLabel,
                    leadingContent = { AddProfileRowAvatar() },
                ),
            )
        }
    }

    SingleChoiceBottomSheet(
        title = stringResource(Res.string.profile_who_is_watching),
        options = options,
        isSelected = { value -> value != AddProfileValue && value == activeProfileIndex },
        onSelected = { value ->
            if (value == AddProfileValue) {
                onAddProfileRequested()
            } else {
                profiles.firstOrNull { it.profileIndex == value }?.let(onProfileChosen)
            }
        },
        onDismiss = onDismiss,
    )
}

/** The avatar in a row's leading slot, with the padlock a locked profile carries. */
@Composable
private fun ProfileRowAvatar(
    profile: Profile,
    avatars: List<AvatarCatalogItem>,
) {
    Box(modifier = Modifier.size(RowAvatarSize)) {
        ActiveProfileMiniAvatar(
            profile = profile,
            avatars = avatars,
            size = RowAvatarSize.value.toInt(),
        )
        if (profile.pinEnabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(LockBadgeSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(10.dp),
                )
            }
        }
    }
}

@Composable
private fun AddProfileRowAvatar() {
    Box(
        modifier = Modifier
            .size(RowAvatarSize)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
    }
}

/**
 * The last row's value. Profile indexes start at one, so nothing can collide with it, and the
 * picker stays a picker of one plain type.
 */
private const val AddProfileValue = -1

private val RowAvatarSize = 40.dp
private val LockBadgeSize = 16.dp
