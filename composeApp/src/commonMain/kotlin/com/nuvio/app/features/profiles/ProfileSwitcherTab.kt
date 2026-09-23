package com.nuvio.app.features.profiles

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import com.nuvio.app.core.profiles.AvatarCatalogItem
import com.nuvio.app.core.profiles.AvatarRepository
import com.nuvio.app.core.profiles.NuvioProfile
import com.nuvio.app.core.profiles.avatarImageUrl
import com.nuvio.app.core.profiles.routeProfileSelection

/**
 * The profile tab, and the switcher it opens.
 *
 * Tapping goes where the tab goes. Holding is the shortcut: it opens the list of profiles as a
 * sheet, which is the same picker the rest of the app uses, rather than a panel floating over the
 * bar with its own hit-testing and its own idea of what a selected item looks like.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfileSwitcherTab(
    selected: Boolean,
    onClick: () -> Unit,
    onProfileSelected: (NuvioProfile) -> Unit,
    onAddProfileRequested: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val profileState by ProfileRepository.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val activeProfile = profileState.activeProfile
    val profiles = profileState.profiles
    val avatars by AvatarRepository.avatars.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        AvatarRepository.refreshAvatars()
    }

    var showSheet by remember { mutableStateOf(false) }
    var pinProfile by remember { mutableStateOf<NuvioProfile?>(null) }

    fun chooseProfile(profile: NuvioProfile) {
        routeProfileSelection(
            profile = profile,
            isEditMode = false,
            activeProfileIndex = ProfileRepository.state.value.activeProfile?.profileIndex,
            onEditProfile = {},
            onActiveProfileSelected = {
                scope.launch { showAlreadyActiveProfileToast(it) }
            },
            // The sheet closes itself on a pick, so the PIN is asked for after it has gone.
            onPinRequired = { pinProfile = it },
            onProfileSelected = onProfileSelected,
        )
    }

    Box(
        modifier = modifier.combinedClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
            onLongClick = {
                if (profiles.isNotEmpty()) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showSheet = true
                }
            },
        ),
        contentAlignment = Alignment.Center,
    ) {
        ActiveProfileMiniAvatar(
            profile = activeProfile,
            avatars = avatars,
            size = TriggerAvatarSize,
        )
    }

    if (showSheet) {
        ProfileSwitcherSheet(
            profiles = profiles,
            avatars = avatars,
            activeProfileIndex = activeProfile?.profileIndex,
            onProfileChosen = ::chooseProfile,
            onAddProfileRequested = onAddProfileRequested,
            onDismiss = { showSheet = false },
        )
    }

    pinProfile?.let { profile ->
        PinEntrySheet(
            profileName = profile.name,
            onVerify = { pin -> ProfileRepository.verifyPin(profile.profileIndex, pin) },
            onVerified = {
                pinProfile = null
                if (profile.profileIndex != ProfileRepository.state.value.activeProfile?.profileIndex) {
                    onProfileSelected(profile)
                }
            },
            onDismiss = { pinProfile = null },
        )
    }
}

/**
 * The profile as it appears outside the profile screens: the avatar alone, at whatever size the
 * place it sits in allows, or a person icon when there is no profile yet.
 */
@Composable
fun ActiveProfileMiniAvatar(
    profile: NuvioProfile?,
    avatars: List<AvatarCatalogItem>,
    size: Int = 24,
) {
    if (profile == null) {
        Icon(
            imageVector = Icons.Rounded.Person,
            contentDescription = stringResource(Res.string.compose_nav_profile),
            modifier = Modifier.size(size.dp),
        )
        return
    }

    val avatarItem = remember(profile.avatarId, avatars) {
        profile.avatarId?.let { id -> avatars.find { it.id == id } }
    }
    val avatarImageUrl = remember(profile.avatarUrl, avatarItem) {
        profileAvatarImageUrl(profile, avatarItem)
    }

    ProfileAvatar(
        size = size.dp,
        imageUrl = avatarImageUrl,
        monogram = profile.name,
        identifier = profile.profileIndex.toString(),
        contentDescription = profile.name,
        containerColor = avatarItem?.bgColor?.let { parseHexColor(it) },
    )
}

private const val TriggerAvatarSize = 28
