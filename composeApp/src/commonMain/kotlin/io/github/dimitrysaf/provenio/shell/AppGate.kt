package io.github.dimitrysaf.provenio.shell

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.provenio.core.auth.AuthRepository
import io.github.dimitrysaf.provenio.core.auth.AuthState
import io.github.dimitrysaf.provenio.core.network.NetworkStatusRepository
import io.github.dimitrysaf.provenio.shell.components.NativeProfileSwitcherController
import io.github.dimitrysaf.provenio.shell.components.NativeTabBridge
import io.github.dimitrysaf.provenio.shell.theme.Tokens
import io.github.dimitrysaf.provenio.shell.components.M3Motion
import io.github.dimitrysaf.provenio.shell.components.PlatformBackHandler
import io.github.dimitrysaf.provenio.core.profiles.AvatarRepository
import io.github.dimitrysaf.provenio.core.profiles.Profile
import io.github.dimitrysaf.provenio.shell.screens.profiles.ProfileEditScreen
import io.github.dimitrysaf.provenio.core.profiles.ProfileRepository
import io.github.dimitrysaf.provenio.shell.screens.profiles.ProfileSelectionScreen
import io.github.dimitrysaf.provenio.shell.screens.profiles.profileAvatarImageUrl
import io.github.dimitrysaf.provenio.shell.nav.AppRoute

private enum class AppGateScreen {
    Loading,
    ProfileSelection,
    ProfileEdit,
    Main,
}

// Which gate screen shows, and the flags around moving between profile selection and the app.
// Built each composition over saveable state, so every copy reads and writes the same values.
private class AppGateState(
    screenState: MutableState<String>,
    autoSkipProfileSelectionState: MutableState<Boolean>,
    profileSelectionLoadingState: MutableState<Boolean>,
    profileSelectionTransitionActiveState: MutableState<Boolean>,
    skipProfileSelectionEnterAnimationState: MutableState<Boolean>,
    private val renderMainContent: Boolean,
    private val appGateController: AppGateController?,
) {
    var screen by screenState
    var autoSkipProfileSelection by autoSkipProfileSelectionState
    var profileSelectionLoading by profileSelectionLoadingState
    var profileSelectionTransitionActive by profileSelectionTransitionActiveState
    var skipProfileSelectionEnterAnimation by skipProfileSelectionEnterAnimationState

    fun isOn(gate: AppGateScreen): Boolean = screen == gate.name

    fun show(gate: AppGateScreen) {
        screen = gate.name
    }

    // Back to profile selection from the app, with nothing pending.
    fun openProfileSelection(skipEnterAnimation: Boolean) {
        autoSkipProfileSelection = false
        profileSelectionLoading = false
        profileSelectionTransitionActive = false
        skipProfileSelectionEnterAnimation = skipEnterAnimation
        show(AppGateScreen.ProfileSelection)
    }

    fun selectProfile(profile: Profile) {
        if (!renderMainContent) {
            appGateController?.beginContentReload()
        }
        ProfileRepository.selectProfile(profile.profileIndex)
    }

    // Goes straight into the app when the profile to use is already known.
    fun tryAutoSelectProfile(profiles: List<Profile>): Boolean {
        val profile = rememberedStartupProfile(profiles)
            ?: profiles.singleOrNull()?.takeUnless { it.pinEnabled }
            ?: return false
        selectProfile(profile)
        show(AppGateScreen.Main)
        autoSkipProfileSelection = false
        return true
    }

    fun enterProfileGate(profiles: List<Profile>) {
        profileSelectionLoading = false
        profileSelectionTransitionActive = false
        if (profiles.isEmpty()) {
            autoSkipProfileSelection = true
            show(AppGateScreen.ProfileSelection)
            return
        }
        autoSkipProfileSelection = true
        if (!tryAutoSelectProfile(profiles)) {
            show(AppGateScreen.ProfileSelection)
        }
    }

    private fun rememberedStartupProfile(profiles: List<Profile>): Profile? {
        val currentProfileState = ProfileRepository.state.value
        if (
            !currentProfileState.rememberLastProfileEnabled ||
            !currentProfileState.hasEverSelectedProfile
        ) {
            return null
        }
        return profiles
            .find { it.profileIndex == ProfileRepository.activeProfileId }
            ?.takeUnless { it.pinEnabled }
    }
}

@Composable
internal fun AppGate(
    initialTab: AppScreenTab,
    initialRoute: AppRoute,
    useNativeNavigation: Boolean,
    useNativeTabBar: Boolean,
    useTabletFloatingTabBar: Boolean,
    ownsAppRuntime: Boolean,
    bypassAppGate: Boolean,
    renderMainContent: Boolean,
    onNavigate: ((AppRoute, launchSingleTop: Boolean) -> Unit)?,
    onGoBack: (() -> Unit)?,
    onReplace: ((AppRoute) -> Unit)?,
    onActivate: ((AppScreenTab) -> Unit)?,
    onAppReady: ((Boolean) -> Unit)?,
    onMainContentMountChanged: ((Boolean) -> Unit)?,
    onMainContentVisibleChanged: ((Boolean) -> Unit)?,
    onTabTitles: ((home: String, search: String, library: String, profile: String, switchProfile: String, addProfile: String) -> Unit)?,
    nativeProfileSwitcherController: NativeProfileSwitcherController?,
    appGateController: AppGateController?,
) {
    if (bypassAppGate) {
        MainAppContent(
            initialTab = initialTab,
            initialRoute = initialRoute,
            useNativeNavigation = useNativeNavigation,
            useNativeTabBar = useNativeTabBar,
            useTabletFloatingTabBar = useTabletFloatingTabBar,
            ownsAppRuntime = ownsAppRuntime,
            onNavigate = onNavigate,
            onGoBack = onGoBack,
            onReplace = onReplace,
            onActivate = onActivate,
            onTabTitles = onTabTitles,
            appGateController = appGateController,
            onRootContentReady = appGateController?.let { controller ->
                controller::reportMainContentReady
            },
            onSwitchProfile = appGateController?.let { controller ->
                controller::requestProfileSelection
            } ?: {},
        )
        return
    }

    AppGateStartupEffects(ownsAppRuntime)

    val authState by AuthRepository.state.collectAsStateWithLifecycle()
    val profileState by ProfileRepository.state.collectAsStateWithLifecycle()

    ProfileTabIconEffect(profileState.activeProfile)

    val gate = AppGateState(
        screenState = rememberSaveable { mutableStateOf(AppGateScreen.Loading.name) },
        autoSkipProfileSelectionState = rememberSaveable { mutableStateOf(false) },
        profileSelectionLoadingState = rememberSaveable { mutableStateOf(false) },
        profileSelectionTransitionActiveState = rememberSaveable { mutableStateOf(false) },
        skipProfileSelectionEnterAnimationState = remember { mutableStateOf(false) },
        renderMainContent = renderMainContent,
        appGateController = appGateController,
    )
    var editingProfile by remember { mutableStateOf<Profile?>(null) }
    val externalMainContentReady = if (!renderMainContent && appGateController != null) {
        val ready by appGateController.mainContentReady.collectAsStateWithLifecycle()
        ready
    } else {
        false
    }

    LaunchedEffect(gate.screen, onAppReady) {
        if (!gate.isOn(AppGateScreen.Main)) {
            onAppReady?.invoke(false)
        }
    }

    if (!renderMainContent) {
        ExternalMainContentEffects(
            gate = gate,
            externalMainContentReady = externalMainContentReady,
            appGateController = appGateController,
            nativeProfileSwitcherController = nativeProfileSwitcherController,
            onActivate = onActivate,
            onMainContentMountChanged = onMainContentMountChanged,
            onMainContentVisibleChanged = onMainContentVisibleChanged,
        )
    }

    // The local user is ready almost at once; until then the gate keeps its loading backdrop.
    LaunchedEffect(authState) {
        val state = authState as? AuthState.Authenticated ?: return@LaunchedEffect
        ProfileRepository.ensureLoaded(state.userId)
        if (gate.isOn(AppGateScreen.Loading)) {
            gate.enterProfileGate(ProfileRepository.state.value.profiles)
        }
    }

    LaunchedEffect(
        gate.screen,
        gate.autoSkipProfileSelection,
        profileState.profiles,
        profileState.hasEverSelectedProfile,
        profileState.rememberLastProfileEnabled,
        profileState.activeProfile?.profileIndex,
        profileState.activeProfile?.pinEnabled,
    ) {
        if (gate.autoSkipProfileSelection && gate.isOn(AppGateScreen.ProfileSelection)) {
            gate.tryAutoSelectProfile(profileState.profiles)
        }
    }

    val profileOverlayVisible = gate.isOn(AppGateScreen.ProfileSelection)
    val profileOverlayState = remember {
        MutableTransitionState(profileOverlayVisible)
    }
    profileOverlayState.targetState = profileOverlayVisible
    val launchOverlayVisible =
        !renderMainContent &&
            gate.isOn(AppGateScreen.Main) &&
            !externalMainContentReady
    val launchOverlayState = remember {
        MutableTransitionState(launchOverlayVisible)
    }
    launchOverlayState.targetState = launchOverlayVisible

    LaunchedEffect(
        renderMainContent,
        gate.screen,
        externalMainContentReady,
        gate.profileSelectionLoading,
        profileOverlayState.currentState,
        profileOverlayState.isIdle,
        launchOverlayState.currentState,
        launchOverlayState.isIdle,
        onAppReady,
    ) {
        if (renderMainContent) return@LaunchedEffect
        val overlaysHidden =
            profileOverlayState.isIdle &&
                !profileOverlayState.currentState &&
                launchOverlayState.isIdle &&
                !launchOverlayState.currentState
        onAppReady?.invoke(
            gate.isOn(AppGateScreen.Main) &&
                externalMainContentReady &&
                !gate.profileSelectionLoading &&
                overlaysHidden,
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = gate.screen,
            label = "app_gate",
            transitionSpec = { M3Motion.fadeThrough() },
        ) { currentGate ->
            when (currentGate) {
                // The system splash stays up while the gate is loading, so this is only its backdrop.
                AppGateScreen.Loading.name -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface),
                    )
                }
                AppGateScreen.ProfileSelection.name -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface),
                    )
                }
                AppGateScreen.ProfileEdit.name -> {
                    PlatformBackHandler(enabled = gate.isOn(AppGateScreen.ProfileEdit)) {
                        gate.show(AppGateScreen.ProfileSelection)
                    }
                    ProfileEditScreen(
                        profile = editingProfile,
                        onBack = { gate.show(AppGateScreen.ProfileSelection) },
                        onSaved = { gate.show(AppGateScreen.ProfileSelection) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                AppGateScreen.Main.name -> {
                    if (renderMainContent) {
                        MainAppContent(
                            initialTab = initialTab,
                            initialRoute = initialRoute,
                            useNativeNavigation = useNativeNavigation,
                            useNativeTabBar = useNativeTabBar,
                            useTabletFloatingTabBar = useTabletFloatingTabBar,
                            ownsAppRuntime = ownsAppRuntime,
                            onNavigate = onNavigate,
                            onGoBack = onGoBack,
                            onReplace = onReplace,
                            onActivate = onActivate,
                            onTabTitles = onTabTitles,
                            appGateController = appGateController,
                            onRootContentReady = { ready ->
                                if (ready) {
                                    gate.profileSelectionLoading = false
                                }
                                onAppReady?.invoke(ready && gate.isOn(AppGateScreen.Main))
                            },
                            onSwitchProfile = { gate.openProfileSelection(skipEnterAnimation = false) },
                        )
                    }
                }
            }
        }

        androidx.compose.animation.AnimatedVisibility(
            visibleState = launchOverlayState,
            enter = fadeIn(M3Motion.fadeThroughInSpec()) + scaleIn(M3Motion.fadeThroughInSpec(), initialScale = 0.92f),
            exit = fadeOut(M3Motion.fadeThroughOutSpec()),
            modifier = Modifier.fillMaxSize(),
        ) {
            AppLaunchOverlay(
                profile = profileState.activeProfile ?: profileState.profiles.firstOrNull(),
                modifier = Modifier.fillMaxSize(),
            )
        }

        ProfileSelectionOverlay(
            gate = gate,
            visibleState = profileOverlayState,
            activeProfileIndex = profileState.activeProfile?.profileIndex,
            renderMainContent = renderMainContent,
            onActivate = onActivate,
            onEditProfile = { profile ->
                editingProfile = profile
                gate.skipProfileSelectionEnterAnimation = false
                gate.show(AppGateScreen.ProfileEdit)
            },
        )
    }
}

// Starts auth, network, membership and profile loading when this gate owns the app runtime.
@Composable
private fun AppGateStartupEffects(ownsAppRuntime: Boolean) {
    LaunchedEffect(Unit) {
        if (!ownsAppRuntime) return@LaunchedEffect
        AuthRepository.initialize()
    }

    LaunchedEffect(Unit) {
        if (!ownsAppRuntime) return@LaunchedEffect
        NetworkStatusRepository.ensureStarted()
        ProfileRepository.loadCachedProfiles()
        AvatarRepository.fetchAvatars()
    }
}

// Keeps the native tab bar's profile icon in step with the active profile.
@Composable
private fun ProfileTabIconEffect(activeProfile: Profile?) {
    val profileAvatars by AvatarRepository.avatars.collectAsStateWithLifecycle()
    LaunchedEffect(
        activeProfile?.profileIndex,
        activeProfile?.name,
        activeProfile?.avatarColorHex,
        activeProfile?.avatarId,
        activeProfile?.avatarUrl,
        profileAvatars,
    ) {
        val avatarItem = activeProfile?.avatarId?.let { avatarId ->
            profileAvatars.find { it.id == avatarId }
        }
        NativeTabBridge.publishProfileTabIcon(
            name = activeProfile?.name,
            avatarColorHex = activeProfile?.avatarColorHex,
            avatarImageUrl = activeProfile?.let { profileAvatarImageUrl(it, avatarItem) },
            avatarBackgroundColorHex = avatarItem?.bgColor,
        )
    }
}

// When the app content lives outside this gate, reports its mount and visibility to the host
// and listens for profile switches coming from the host and the native profile switcher.
@Composable
private fun ExternalMainContentEffects(
    gate: AppGateState,
    externalMainContentReady: Boolean,
    appGateController: AppGateController?,
    nativeProfileSwitcherController: NativeProfileSwitcherController?,
    onActivate: ((AppScreenTab) -> Unit)?,
    onMainContentMountChanged: ((Boolean) -> Unit)?,
    onMainContentVisibleChanged: ((Boolean) -> Unit)?,
) {
    var mainContentStarted by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(gate.screen, onMainContentMountChanged) {
        when (gate.screen) {
            AppGateScreen.Main.name -> {
                mainContentStarted = true
                onMainContentMountChanged?.invoke(true)
            }
            AppGateScreen.Loading.name -> {
                mainContentStarted = false
                appGateController?.reportMainContentReady(false)
                onMainContentMountChanged?.invoke(false)
            }
            else -> onMainContentMountChanged?.invoke(mainContentStarted)
        }
    }

    LaunchedEffect(appGateController) {
        appGateController?.profileSelectionRequests?.collect {
            gate.openProfileSelection(skipEnterAnimation = true)
        }
    }

    LaunchedEffect(nativeProfileSwitcherController, appGateController) {
        if (appGateController == null) return@LaunchedEffect
        nativeProfileSwitcherController?.requestedManageProfiles?.collect {
            appGateController.requestProfileSelection()
        }
    }

    LaunchedEffect(nativeProfileSwitcherController, appGateController) {
        if (appGateController == null) return@LaunchedEffect
        nativeProfileSwitcherController?.selectedProfileIndices?.collect { profileIndex ->
            if (profileIndex == ProfileRepository.state.value.activeProfile?.profileIndex) return@collect
            val profile = ProfileRepository.state.value.profiles
                .firstOrNull { it.profileIndex == profileIndex }
                ?: return@collect
            gate.autoSkipProfileSelection = false
            gate.profileSelectionLoading = true
            gate.profileSelectionTransitionActive = true
            gate.skipProfileSelectionEnterAnimation = true
            appGateController.beginContentReload()
            ProfileRepository.selectProfile(profile.profileIndex)
            gate.show(AppGateScreen.Main)
            onActivate?.invoke(AppScreenTab.Home)
        }
    }

    LaunchedEffect(externalMainContentReady) {
        if (externalMainContentReady) {
            gate.profileSelectionLoading = false
        }
    }

    LaunchedEffect(gate.screen, externalMainContentReady, onMainContentVisibleChanged) {
        onMainContentVisibleChanged?.invoke(gate.isOn(AppGateScreen.Main) && externalMainContentReady)
    }
}

// The profile picker, drawn over the app so switching profiles does not tear the app down.
@Composable
private fun ProfileSelectionOverlay(
    gate: AppGateState,
    visibleState: MutableTransitionState<Boolean>,
    activeProfileIndex: Int?,
    renderMainContent: Boolean,
    onActivate: ((AppScreenTab) -> Unit)?,
    onEditProfile: (Profile?) -> Unit,
) {
    androidx.compose.animation.AnimatedVisibility(
        visibleState = visibleState,
        enter = if (gate.skipProfileSelectionEnterAnimation) {
            androidx.compose.animation.EnterTransition.None
        } else {
            fadeIn(M3Motion.fadeThroughInSpec()) + scaleIn(M3Motion.fadeThroughInSpec(), initialScale = 0.92f)
        },
        exit = fadeOut(M3Motion.fadeThroughOutSpec()),
        modifier = Modifier
            .fillMaxSize()
            .zIndex(Tokens.Z.dialog),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val onBack: (() -> Unit)? = if (!gate.autoSkipProfileSelection) {
                {
                    gate.skipProfileSelectionEnterAnimation = false
                    gate.show(AppGateScreen.Main)
                }
            } else {
                null
            }
            PlatformBackHandler(
                enabled = gate.isOn(AppGateScreen.ProfileSelection) && !gate.profileSelectionLoading,
            ) {
                onBack?.invoke()
            }
            ProfileSelectionScreen(
                onProfileSelected = { profile ->
                    if (
                        !gate.profileSelectionLoading &&
                        (gate.autoSkipProfileSelection || profile.profileIndex != ProfileRepository.state.value.activeProfile?.profileIndex)
                    ) {
                        gate.profileSelectionLoading = true
                        gate.profileSelectionTransitionActive = true
                        gate.skipProfileSelectionEnterAnimation = false
                        gate.selectProfile(profile = profile)
                        gate.show(AppGateScreen.Main)
                        if (!renderMainContent) {
                            onActivate?.invoke(AppScreenTab.Home)
                        }
                    }
                },
                onEditProfile = onEditProfile,
                onAddProfile = { onEditProfile(null) },
                interactionEnabled = !gate.profileSelectionLoading,
                onBack = onBack,
                activeProfileIndex = if (gate.autoSkipProfileSelection) null else activeProfileIndex,
                contentVisible = !gate.profileSelectionTransitionActive,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
