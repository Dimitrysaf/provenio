@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.github.dimitrysaf.provenio.shell.screens.onboarding

import io.github.dimitrysaf.provenio.shell.components.clearFocusOnTap
import io.github.dimitrysaf.provenio.shell.components.singleKeyPresses
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.provenio.core.profiles.ProfileRepository
import io.github.dimitrysaf.provenio.shell.components.BackButton
import io.github.dimitrysaf.provenio.shell.components.M3Motion
import io.github.dimitrysaf.provenio.shell.components.PlatformBackHandler
import io.github.dimitrysaf.provenio.shell.components.WindowBreakpoint
import io.github.dimitrysaf.provenio.shell.components.openedWithKeys
import io.github.dimitrysaf.provenio.shell.screens.profiles.PROFILE_COLORS
import io.github.dimitrysaf.provenio.shell.screens.settings.AppBrandSymbol
import kotlinx.coroutines.delay

internal enum class OnboardingStep {
    Welcome,
    Appearance,
    Profile,
    Addons,
    Tracking,
    Done,
}

internal enum class OnboardingProfilePath {
    New,
    Bring,
}

internal data class OnboardingPage(
    val step: OnboardingStep,
    val profilePath: OnboardingProfilePath?,
)

/**
 * Where a device being set up is in the onboarding. It is kept outside the screen, so a change of
 * language, which restarts the screen, does not send the user back to the start.
 */
@Stable
internal class OnboardingState(val preview: Boolean) {
    var step by mutableStateOf(OnboardingStep.Welcome)
        private set
    var forward by mutableStateOf(true)
        private set
    var profilePath by mutableStateOf<OnboardingProfilePath?>(null)
    var profileName by mutableStateOf("")
    var profileColorHex by mutableStateOf(PROFILE_COLORS.first())

    /** The name of the profile made here, once it is made; nothing is made in a preview. */
    var createdProfileName by mutableStateOf<String?>(null)
    var creatingProfile by mutableStateOf(false)

    val canGoBack: Boolean
        get() = step != OnboardingStep.Welcome

    /** What is on screen: the step, and on the profile step, which way of getting one was chosen. */
    val page: OnboardingPage
        get() = OnboardingPage(step, profilePath.takeIf { step == OnboardingStep.Profile })

    /** Choosing how to get a profile moves on to a page of its own, as moving to a step does. */
    fun choosePath(path: OnboardingProfilePath) {
        forward = true
        profilePath = path
    }

    fun goTo(target: OnboardingStep) {
        if (target == step) return
        forward = target.ordinal > step.ordinal
        step = target
    }

    fun next() {
        OnboardingStep.entries.getOrNull(step.ordinal + 1)?.let(::goTo)
    }

    fun back() {
        // Choosing how to bring a profile is a step of its own, undone before leaving the page.
        if (step == OnboardingStep.Profile && profilePath != null && createdProfileName == null) {
            forward = false
            profilePath = null
            return
        }
        OnboardingStep.entries.getOrNull(step.ordinal - 1)?.let(::goTo)
    }
}

/** The onboarding of a device with no profiles yet, while it runs. */
internal object OnboardingSession {
    /** While true, the profile made during the onboarding does not open the app by itself. */
    var running by mutableStateOf(false)
        private set
    private var state: OnboardingState? = null

    fun state(): OnboardingState = state ?: OnboardingState(preview = false).also { state = it }

    fun begin() {
        running = true
    }

    fun finish() {
        running = false
        state = null
    }
}

/** The onboarding shown again from Settings, to look at: Back closes it, and nothing is set up. */
internal object OnboardingPreview {
    var state by mutableStateOf<OnboardingState?>(null)
        private set

    fun open() {
        state = OnboardingState(preview = true)
    }

    fun close() {
        state = null
    }
}

@Composable
internal fun OnboardingPreviewHost() {
    val state = OnboardingPreview.state ?: return
    // Full screen over the app, which takes neither a press nor focus while it shows.
    Dialog(
        onDismissRequest = OnboardingPreview::close,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Onboarding(
            state = state,
            onFinish = OnboardingPreview::close,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * Setting up Provenio, a step at a time: a welcome, the look, a profile, addons, tracking, and a
 * summary. Compact windows stack each step; wide ones, tablets, desktops and TVs alike, keep the
 * step's picture and title in a pane of their own beside what is chosen.
 */
@Composable
internal fun Onboarding(
    state: OnboardingState,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val profileState by ProfileRepository.state.collectAsStateWithLifecycle()
    if (!state.preview) {
        // Profiles that arrive from another device finish the setup: they bring their own.
        val hasProfiles = profileState.profiles.isNotEmpty()
        LaunchedEffect(hasProfiles) {
            if (hasProfiles && state.createdProfileName == null && !state.creatingProfile) onFinish()
        }
        PlatformBackHandler(enabled = state.canGoBack, onBack = state::back)
    }

    val primaryFocus = remember { FocusRequester() }
    val withKeys = openedWithKeys()
    // With a remote or keys, each step starts on what moves it forward, once it has slid in.
    LaunchedEffect(state.step, state.profilePath) {
        if (!withKeys) return@LaunchedEffect
        delay(StepFocusDelayMillis)
        runCatching { primaryFocus.requestFocus() }
    }

    val actions = onboardingActions(state, onFinish)

    // The window's own height: the keyboard, which the content makes room for, does not shrink it.
    val windowHeight = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }

    // Its own record of the keys pressed, which lets the arrows into a text field: the preview is a
    // window of its own, which the app's record does not reach.
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.singleKeyPresses().clearFocusOnTap()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            val breakpoint = WindowBreakpoint.forWidth(maxWidth)
            val twoPane = breakpoint.isTwoPane && maxHeight >= TwoPaneMinHeight
            AnimatedContent(
                targetState = state.step == OnboardingStep.Welcome,
                transitionSpec = { M3Motion.fadeThrough() },
                label = "OnboardingWelcome",
            ) { welcome ->
                when {
                    welcome -> WelcomeOnboarding(state, actions, primaryFocus, breakpoint)
                    twoPane -> TwoPaneOnboarding(state, actions, primaryFocus, breakpoint)
                    else -> SinglePaneOnboarding(state, actions, primaryFocus, breakpoint, windowHeight)
                }
            }
        }
    }
}

/**
 * The welcome, on a page of its own at every size: the greeting, a slideshow of what Provenio
 * does, and the way in. The slideshow takes the room there is, so nothing has to scroll.
 */
@Composable
private fun WelcomeOnboarding(
    state: OnboardingState,
    actions: OnboardingActions,
    primaryFocus: FocusRequester,
    breakpoint: WindowBreakpoint,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = breakpoint.margin, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppBrandSymbol(modifier = Modifier.size(if (breakpoint.isTwoPane) 44.dp else 36.dp))
            StepHeading(step = OnboardingStep.Welcome, state = state, centered = true, large = breakpoint.isTwoPane)
        }
        Spacer(modifier = Modifier.height(24.dp))
        WelcomeSlideshow(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = WelcomeSlideshowMaxWidth)
                .fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(24.dp))
        OnboardingButtons(
            actions = actions,
            primaryFocus = primaryFocus,
            stacked = true,
            modifier = Modifier
                .widthIn(max = WelcomeButtonsMaxWidth)
                .fillMaxWidth(),
        )
    }
}

/** What a step's buttons do: its main action, and the one beside it, if any. */
internal class OnboardingActions(
    val primaryLabel: String?,
    val primaryEnabled: Boolean,
    val onPrimary: () -> Unit,
    val secondaryLabel: String? = null,
    val onSecondary: () -> Unit = {},
)

@Composable
private fun SinglePaneOnboarding(
    state: OnboardingState,
    actions: OnboardingActions,
    primaryFocus: FocusRequester,
    breakpoint: WindowBreakpoint,
    windowHeight: Dp,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OnboardingTopBar(
            state = state,
            modifier = Modifier.padding(horizontal = breakpoint.margin, vertical = 8.dp),
        )
        StepTransition(state, modifier = Modifier.weight(1f)) { page ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = breakpoint.margin)
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier.widthIn(max = SinglePaneMaxWidth).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // A short window, a phone on its side, keeps the room for what is chosen.
                    val heroSize = when {
                        windowHeight < 640.dp -> 96.dp
                        page.step == OnboardingStep.Done -> 160.dp
                        else -> 120.dp
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OnboardingHero(step = page.step, state = state, size = heroSize)
                    Spacer(modifier = Modifier.height(20.dp))
                    StepHeading(step = page.step, state = state, centered = true)
                    Spacer(modifier = Modifier.height(24.dp))
                    StepContent(step = page.step, state = state)
                }
            }
        }
        OnboardingButtons(
            actions = actions,
            primaryFocus = primaryFocus,
            stacked = true,
            modifier = Modifier
                .widthIn(max = SinglePaneMaxWidth)
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = breakpoint.margin, vertical = 16.dp),
        )
    }
}

@Composable
private fun TwoPaneOnboarding(
    state: OnboardingState,
    actions: OnboardingActions,
    primaryFocus: FocusRequester,
    breakpoint: WindowBreakpoint,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(breakpoint.margin),
        horizontalArrangement = Arrangement.spacedBy(breakpoint.margin),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = ShapeDefaults.ExtraLarge,
            modifier = Modifier
                .weight(0.44f)
                .fillMaxHeight(),
        ) {
            StepTransition(state, modifier = Modifier.fillMaxSize()) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 48.dp, vertical = 40.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start,
                ) {
                    OnboardingHero(step = page.step, state = state, size = 200.dp)
                    Spacer(modifier = Modifier.height(40.dp))
                    StepHeading(step = page.step, state = state, centered = false, large = true)
                }
            }
        }
        Column(
            modifier = Modifier
                .weight(0.56f)
                .fillMaxHeight(),
        ) {
            OnboardingTopBar(state = state, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp))
            StepTransition(state, modifier = Modifier.weight(1f)) { page ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = TwoPaneContentMaxWidth)
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 24.dp),
                    ) {
                        StepContent(step = page.step, state = state)
                    }
                }
            }
            OnboardingButtons(
                actions = actions,
                primaryFocus = primaryFocus,
                stacked = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            )
        }
    }
}

/** Back, and how far along the setup is, as a wavy line: none on the welcome. */
@Composable
private fun OnboardingTopBar(state: OnboardingState, modifier: Modifier = Modifier) {
    val progress by animateFloatAsState(
        targetValue = state.step.ordinal / (OnboardingStep.entries.size - 1).toFloat(),
        animationSpec = tween(600, easing = M3Motion.Emphasized),
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state.canGoBack) {
            BackButton(onClick = state::back)
        } else {
            Spacer(modifier = Modifier.size(40.dp))
        }
        if (state.step != OnboardingStep.Welcome) {
            LinearWavyProgressIndicator(
                progress = { progress },
                modifier = Modifier.weight(1f),
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.size(40.dp))
    }
}

@Composable
private fun StepTransition(
    state: OnboardingState,
    modifier: Modifier = Modifier,
    content: @Composable (OnboardingPage) -> Unit,
) {
    val slide = with(LocalDensity.current) { M3Motion.SharedAxisSlide.roundToPx() }
    AnimatedContent(
        targetState = state.page,
        transitionSpec = { M3Motion.sharedAxisX(forward = state.forward, slidePx = slide) },
        modifier = modifier,
        label = "OnboardingStep",
    ) { page -> content(page) }
}

@Composable
private fun StepHeading(
    step: OnboardingStep,
    state: OnboardingState,
    centered: Boolean,
    large: Boolean = false,
) {
    val headline = when {
        step == OnboardingStep.Welcome && large -> MaterialTheme.typography.displaySmall
        step == OnboardingStep.Welcome -> MaterialTheme.typography.headlineMedium
        large -> MaterialTheme.typography.displaySmall
        else -> MaterialTheme.typography.headlineMedium
    }
    Text(
        text = stepTitle(step, state),
        style = headline,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = if (centered) TextAlign.Center else TextAlign.Start,
    )
}

@Composable
private fun StepContent(step: OnboardingStep, state: OnboardingState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when (step) {
            OnboardingStep.Welcome -> Unit
            OnboardingStep.Appearance -> AppearanceStep()
            OnboardingStep.Profile -> ProfileStep(state)
            OnboardingStep.Addons -> AddonsStep()
            OnboardingStep.Tracking -> TrackingStep()
            OnboardingStep.Done -> DoneStep(state)
        }
    }
}

/**
 * The step's main button, at the end of the window where a thumb or the eye ends up, and a second
 * one beside it on the welcome. Phones stack them full width; wide windows put them in a row.
 */
@Composable
private fun OnboardingButtons(
    actions: OnboardingActions,
    primaryFocus: FocusRequester,
    stacked: Boolean,
    modifier: Modifier = Modifier,
) {
    if (actions.primaryLabel == null && actions.secondaryLabel == null) return
    val padding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight)
    val primary: @Composable (Modifier) -> Unit = { buttonModifier ->
        actions.primaryLabel?.let { label ->
            Button(
                onClick = actions.onPrimary,
                enabled = actions.primaryEnabled,
                contentPadding = padding,
                modifier = buttonModifier
                    .heightIn(min = ButtonDefaults.MediumContainerHeight)
                    .focusRequester(primaryFocus),
            ) {
                Text(label, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
    if (stacked) {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            primary(Modifier.fillMaxWidth())
            actions.secondaryLabel?.let { label ->
                TextButton(
                    onClick = actions.onSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = ButtonDefaults.MediumContainerHeight),
                ) {
                    Text(label, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    } else {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The second button leads, so the main one ends the row.
            actions.secondaryLabel?.let { label ->
                FilledTonalButton(
                    onClick = actions.onSecondary,
                    contentPadding = padding,
                    modifier = Modifier.heightIn(min = ButtonDefaults.MediumContainerHeight),
                ) {
                    Text(label, style = MaterialTheme.typography.titleMedium)
                }
            }
            primary(Modifier)
        }
    }
}

private val SinglePaneMaxWidth = 560.dp
private val WelcomeButtonsMaxWidth = 400.dp
private val WelcomeSlideshowMaxWidth = 720.dp
private val TwoPaneContentMaxWidth = 600.dp
private val TwoPaneMinHeight = 480.dp
private const val StepFocusDelayMillis = 450L
