package io.github.dimitrysaf.provenio.shell.screens.profiles

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.foundation.layout.fillMaxHeight
import io.github.dimitrysaf.provenio.shell.components.BackButton
import io.github.dimitrysaf.provenio.shell.components.WindowBreakpoint
import io.github.dimitrysaf.provenio.core.membership.CosmeticEntitlement
import io.github.dimitrysaf.provenio.shell.screens.settings.MemberBrandWordmark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import provenio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import io.github.dimitrysaf.provenio.core.profiles.AvatarRepository
import io.github.dimitrysaf.provenio.core.profiles.MAX_PROFILES
import io.github.dimitrysaf.provenio.core.profiles.Profile
import io.github.dimitrysaf.provenio.core.profiles.avatarImageUrl
import io.github.dimitrysaf.provenio.core.profiles.routeProfileSelection
import io.github.dimitrysaf.provenio.core.profiles.ProfileRepository

/** m3.material.io/components/side-sheets/specs — max-width. */
private val SideSheetMaxWidth = 400.dp

@Composable
fun ProfileSelectionScreen(
    onProfileSelected: (Profile) -> Unit,
    onEditProfile: (Profile) -> Unit,
    onAddProfile: () -> Unit,
    onBack: (() -> Unit)? = null,
    interactionEnabled: Boolean = true,
    activeProfileIndex: Int? = null,
    contentVisible: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val profileState by ProfileRepository.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var pinDialogProfile by remember { mutableStateOf<Profile?>(null) }
    var isEditMode by remember { mutableStateOf(false) }
    // Non-null while the editor pane is open. Only ever set on windows wide enough for a second
    // pane; narrower windows hand the editor to the host as a full-window destination instead.
    var editorTarget by remember { mutableStateOf<ProfileEditorTarget?>(null) }

    val titleAlpha = remember { Animatable(0f) }
    val titleOffset = remember { Animatable(20f) }
    val manageAlpha = remember { Animatable(0f) }
    val onProfileClick: (Profile, Boolean) -> Unit = { profile, paneAvailable ->
        if (interactionEnabled) {
            routeProfileSelection(
                profile = profile,
                isEditMode = isEditMode,
                activeProfileIndex = activeProfileIndex,
                onEditProfile = { target ->
                    if (paneAvailable) {
                        editorTarget = ProfileEditorTarget.Edit(target)
                    } else {
                        onEditProfile(target)
                    }
                },
                onActiveProfileSelected = { scope.launch { showAlreadyActiveProfileToast(it) } },
                onPinRequired = { pinDialogProfile = it },
                onProfileSelected = onProfileSelected,
            )
        }
    }

    LaunchedEffect(Unit) {
        AvatarRepository.refreshAvatars()
    }

    LaunchedEffect(Unit) {
        launch { titleAlpha.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }
        launch { titleOffset.animateTo(0f, tween(600, easing = FastOutSlowInEasing)) }
        delay(300)
        manageAlpha.animateTo(1f, tween(500))
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navigationBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        val breakpoint = WindowBreakpoint.forWidth(maxWidth)
        val usePane = breakpoint.isTwoPane

        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(180)),
            modifier = Modifier.fillMaxSize(),
        ) {
            val profiles = profileState.profiles
            val canAdd = profiles.size < MAX_PROFILES
            val items = profiles.size + if (canAdd) 1 else 0

            val openCreate: () -> Unit = {
                if (usePane) editorTarget = ProfileEditorTarget.Create else onAddProfile()
            }

            // With no profiles there is nothing to choose, so the device is set up here: a new profile, or another device's.
            if (profileState.isLoaded && profiles.isEmpty() && editorTarget == null) {
                ProfileWelcome(
                    onCreateProfile = openCreate,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = statusBarTop, bottom = navigationBarBottom),
                )
                return@AnimatedVisibility
            }

            if (usePane) {
                // Creating or editing a profile is a temporary task, and the spec keeps those
                // floating rather than co-planar, so the editor is a modal side sheet over the
                // avatars instead of a pane that displaces them. The avatars keep their full
                // size the whole time.
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = statusBarTop)
                            .padding(horizontal = breakpoint.margin, vertical = 24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ProfileSelectionHeading(
                            titleAlpha = titleAlpha.value,
                            titleOffset = titleOffset.value,
                            wordmarkHeight = 42.dp,
                        )

                        Spacer(modifier = Modifier.height(40.dp))

                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                modifier = Modifier
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(32.dp),
                            ) {
                                for (currentIndex in 0 until items) {
                                    if (currentIndex < profiles.size) {
                                        val profile = profiles[currentIndex]
                                        ProfileAvatarCard(
                                            profile = profile,
                                            isEditMode = isEditMode,
                                            animDelay = currentIndex * 80,
                                            enabled = interactionEnabled,
                                            metrics = ProfileCardMetrics.Large,
                                            onClick = { onProfileClick(profile, usePane) },
                                        )
                                    } else {
                                        AddProfileCard(
                                            animDelay = currentIndex * 80,
                                            enabled = interactionEnabled,
                                            metrics = ProfileCardMetrics.Large,
                                            onClick = openCreate,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Managing profiles is not one of the things being chosen between, so it
                    // sits in the corner rather than in the line of avatars.
                    ManageProfilesToggle(
                        isEditMode = isEditMode,
                        alpha = manageAlpha.value,
                        enabled = interactionEnabled,
                        onToggle = { isEditMode = !isEditMode },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(24.dp)
                            .padding(bottom = navigationBarBottom),
                    )

                    AnimatedVisibility(
                        visible = editorTarget != null,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { editorTarget = null },
                        )
                    }

                    AnimatedVisibility(
                        visible = editorTarget != null,
                        enter = slideInHorizontally { it },
                        exit = slideOutHorizontally { it },
                        // A detached sheet sits inside the safety region, otherwise its docked
                        // actions run under the system bars.
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .windowInsetsPadding(WindowInsets.systemBars),
                    ) {
                        Surface(
                            // Detached, so it takes the spec's 16dp margins and its own
                            // surface container low rather than sitting on the background.
                            modifier = Modifier
                                .padding(16.dp)
                                .width(SideSheetMaxWidth)
                                .fillMaxHeight(),
                            shape = ShapeDefaults.ExtraLarge,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                        ) {
                            ProfileEditScreen(
                                profile = (editorTarget as? ProfileEditorTarget.Edit)?.profile,
                                onBack = { editorTarget = null },
                                onSaved = { editorTarget = null },
                                embedded = true,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = statusBarTop)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = breakpoint.margin),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(60.dp))

                    ProfileSelectionHeading(
                        titleAlpha = titleAlpha.value,
                        titleOffset = titleOffset.value,
                        wordmarkHeight = 34.dp,
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        var index = 0
                        while (index < items) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                for (col in 0..1) {
                                    if (index < items) {
                                        val currentIndex = index
                                        if (currentIndex < profiles.size) {
                                            val profile = profiles[currentIndex]
                                            ProfileAvatarCard(
                                                profile = profile,
                                                isEditMode = isEditMode,
                                                animDelay = currentIndex * 80,
                                                enabled = interactionEnabled,
                                                onClick = { onProfileClick(profile, usePane) },
                                            )
                                        } else {
                                            AddProfileCard(
                                                animDelay = currentIndex * 80,
                                                enabled = interactionEnabled,
                                                onClick = openCreate,
                                            )
                                        }
                                        index++
                                    } else {
                                        if (profiles.isNotEmpty()) {
                                            Spacer(modifier = Modifier.width(150.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    // A phone has no corner to spare, so the button takes the width instead.
                    ManageProfilesToggle(
                        isEditMode = isEditMode,
                        alpha = manageAlpha.value,
                        enabled = interactionEnabled,
                        onToggle = { isEditMode = !isEditMode },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        if (onBack != null && interactionEnabled && contentVisible) {
            BackButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = statusBarTop + 8.dp),
            )
        }
    }

    pinDialogProfile?.let { profile ->
        PinEntrySheet(
            profileName = profile.name,
            onVerify = { pin -> ProfileRepository.verifyPin(profile.profileIndex, pin) },
            onVerified = {
                pinDialogProfile = null
                if (interactionEnabled && profile.profileIndex != activeProfileIndex) {
                    onProfileSelected(profile)
                }
            },
            onDismiss = { pinDialogProfile = null },
        )
    }
}

/** What the editor pane is currently open for; null when the pane is closed. */
private sealed interface ProfileEditorTarget {
    data object Create : ProfileEditorTarget
    data class Edit(val profile: Profile) : ProfileEditorTarget
}

/** Sizing for a profile tile. The two-pane layout shows the avatars far larger. */
/**
 * How a profile arrives: it fades in where it belongs and settles up into place, staggered by
 * [delayMs] so the row reads left to right rather than all at once.
 *
 * The specs come from the theme's motion scheme, so the movement is the spring Material uses for
 * things that travel and the fade is the curve it uses for things that only change opacity,
 * instead of two hand-picked tweens.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Modifier.profileCardEntrance(delayMs: Int): Modifier {
    var arrived by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMs.toLong() + ProfileEntranceLeadIn)
        arrived = true
    }
    val scale by animateFloatAsState(
        targetValue = if (arrived) 1f else 0.85f,
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "profileCardScale",
    )
    val lift by animateFloatAsState(
        targetValue = if (arrived) 0f else ProfileEntranceLift,
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "profileCardLift",
    )
    val fade by animateFloatAsState(
        targetValue = if (arrived) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "profileCardFade",
    )
    return graphicsLayer {
        alpha = fade
        scaleX = scale
        scaleY = scale
        translationY = lift
    }
}

/** Long enough for the screen itself to have settled before the profiles start arriving. */
private const val ProfileEntranceLeadIn = 150L

/** How far below its place a profile starts, in pixels. */
private const val ProfileEntranceLift = 30f

private data class ProfileCardMetrics(
    val cardWidth: Dp,
    val avatarSize: Dp,
    val haloSize: Dp,
    val initialSize: TextUnit,
    val placeholderIconSize: Dp,
    val badgeSize: Dp,
    val badgeIconSize: Dp,
    val labelSize: TextUnit,
) {
    companion object {
        val Default = ProfileCardMetrics(
            cardWidth = 150.dp,
            avatarSize = 100.dp,
            haloSize = 110.dp,
            initialSize = 38.sp,
            placeholderIconSize = 46.dp,
            badgeSize = 34.dp,
            badgeIconSize = 16.dp,
            labelSize = 16.sp,
        )
        val Large = ProfileCardMetrics(
            cardWidth = 260.dp,
            avatarSize = 200.dp,
            haloSize = 216.dp,
            initialSize = 76.sp,
            placeholderIconSize = 92.dp,
            badgeSize = 56.dp,
            badgeIconSize = 28.dp,
            labelSize = 22.sp,
        )
    }
}

@Composable
private fun ProfileSelectionHeading(
    titleAlpha: Float,
    titleOffset: Float,
    wordmarkHeight: Dp,
) {
    MemberBrandWordmark(
        height = wordmarkHeight,
        modifier = Modifier.graphicsLayer {
            alpha = titleAlpha
            translationY = titleOffset
        },
    )

    Spacer(modifier = Modifier.height(22.dp))

    Text(
        text = stringResource(Res.string.profile_who_is_watching),
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.graphicsLayer {
            alpha = titleAlpha
            translationY = titleOffset
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ManageProfilesToggle(
    isEditMode: Boolean,
    alpha: Float,
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Manage mode is an on/off selection, which is what the toggle button variant is for: one
    // component carrying a checked state, morphing round to square and taking the selected
    // colour roles. Swapping a filled button in for an outlined one would be a component swap
    // where a state change is what's actually happening.
    ToggleButton(
        checked = isEditMode,
        onCheckedChange = { onToggle() },
        modifier = modifier.graphicsLayer { this.alpha = alpha },
        enabled = enabled,
        shapes = ToggleButtonDefaults.shapesFor(ToggleButtonDefaults.MinHeight),
        contentPadding = ButtonDefaults.contentPaddingFor(
            buttonHeight = ToggleButtonDefaults.MinHeight,
            hasStartIcon = true,
        ),
    ) {
        Icon(
            imageVector = if (isEditMode) Icons.Outlined.Done else Icons.Outlined.Edit,
            contentDescription = null,
            modifier = Modifier.size(ToggleButtonDefaults.IconSize),
        )
        Spacer(modifier = Modifier.width(ToggleButtonDefaults.IconSpacing))
        Text(
            text = if (isEditMode) {
                stringResource(Res.string.action_done)
            } else {
                stringResource(Res.string.profile_manage_profiles)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileAvatarCard(
    profile: Profile,
    isEditMode: Boolean,
    animDelay: Int,
    enabled: Boolean,
    metrics: ProfileCardMetrics = ProfileCardMetrics.Default,
    onClick: () -> Unit,
) {
    val avatars by AvatarRepository.avatars.collectAsStateWithLifecycle()
    val avatarItem = remember(profile.avatarId, avatars) {
        profile.avatarId?.let { id -> avatars.find { it.id == id } }
    }
    val avatarImageUrl = remember(profile.avatarUrl, avatarItem) {
        profileAvatarImageUrl(profile, avatarItem)
    }

    // The card is a surface that takes a tap, so pressing it draws Material's own state layer
    // and ripple. It used to shrink instead, which is a gesture no Material component makes.
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .width(metrics.cardWidth)
            .profileCardEntrance(animDelay),
        shape = ShapeDefaults.ExtraLarge,
        color = Color.Transparent,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp),
        ) {
            // Not a Badge. Badges are 6dp dots or 16dp-tall count chips in error colours, for
            // notifications on navigation items — a 28dp icon inside one stretches it into a pill.
            // These are status/affordance pips on the avatar, so they're built from the shape and
            // colour system at a size proportional to the tile.
            Box(contentAlignment = Alignment.BottomEnd) {
                ProfileAvatar(
                    size = metrics.avatarSize,
                    imageUrl = avatarImageUrl,
                    monogram = profile.name,
                    // The colour is hashed from the profile's stable index, not its name, so
                    // renaming a profile doesn't change the avatar out from under someone.
                    identifier = profile.profileIndex.toString(),
                    contentDescription = avatarItem?.displayName ?: profile.name,
                    containerColor = avatarItem?.bgColor?.let { parseHexColor(it) },
                    iconSize = metrics.placeholderIconSize,
                )

                val pip: Pair<androidx.compose.ui.graphics.vector.ImageVector, Boolean>? = when {
                    isEditMode -> Icons.Outlined.Edit to true
                    profile.pinEnabled -> Icons.Outlined.Lock to false
                    else -> null
                }

                pip?.let { (icon, isPrimary) ->
                    Box(
                        modifier = Modifier
                            .size(metrics.badgeSize)
                            .clip(CircleShape)
                            .background(
                                if (isPrimary) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.secondaryContainer
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isPrimary) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            },
                            modifier = Modifier.size(metrics.badgeIconSize),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = profile.name.ifBlank {
                    stringResource(Res.string.profile_label_number, profile.profileIndex)
                },
                style = MaterialTheme.typography.titleMedium.copy(fontSize = metrics.labelSize),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddProfileCard(
    animDelay: Int,
    enabled: Boolean,
    metrics: ProfileCardMetrics = ProfileCardMetrics.Default,
    onClick: () -> Unit,
) {
    // The card is a surface that takes a tap, so pressing it draws Material's own state layer
    // and ripple. It used to shrink instead, which is a gesture no Material component makes.
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .width(metrics.cardWidth)
            .profileCardEntrance(animDelay),
        shape = ShapeDefaults.ExtraLarge,
        color = Color.Transparent,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp),
        ) {
            ProfileAvatar(
                size = metrics.avatarSize,
                imageUrl = null,
                monogram = null,
                identifier = "",
                contentDescription = null,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                iconSize = metrics.placeholderIconSize,
                icon = Icons.Outlined.Add,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(Res.string.compose_profile_add_profile),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = metrics.labelSize),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
