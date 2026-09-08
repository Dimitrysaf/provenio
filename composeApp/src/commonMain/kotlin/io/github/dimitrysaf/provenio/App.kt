package io.github.dimitrysaf.provenio

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.dimitrysaf.provenio.navigation.Routes
import io.github.dimitrysaf.provenio.p2p.P2pRepository
import io.github.dimitrysaf.provenio.player.PlayerRepository
import io.github.dimitrysaf.provenio.player.PlayerScreen
import io.github.dimitrysaf.provenio.stremio.AddonRepository
import io.github.dimitrysaf.provenio.pages.SearchPage
import io.github.dimitrysaf.provenio.pages.SettingsCategory
import io.github.dimitrysaf.provenio.pages.SettingsPage
import io.github.dimitrysaf.provenio.theme.AppTheme
import io.github.dimitrysaf.provenio.theme.MotionTokens
import io.github.dimitrysaf.provenio.theme.ThemeMode
import io.github.dimitrysaf.provenio.theme.isDynamicColorSupported
import io.github.dimitrysaf.provenio.ui.HomeScreen

/**
 * Page transitions. Only ever one surface animates, and it animates over another that
 * stays fully opaque. Cross-fading both at once leaves a window where neither is opaque
 * and the window background shows through as a dark flash.
 *
 * Forward, the arriving page fades in over the one it covers. Back, the leaving page
 * shrinks 100% to 90% over the destination already sitting behind it at full opacity,
 * which is the predictive-back preview for a full-screen surface. Navigation Compose
 * seeks [popExit] with the drag, so that shrink follows the gesture directly and what
 * shows through is the destination, never the background.
 *
 * https://developer.android.com/design/ui/mobile/guides/patterns/predictive-back
 */
private const val BackPreviewScale = 0.9f

/** The leaving page rounds its corners as it pulls away, as the system surfaces do. */
private val BackPreviewCornerRadius = 28.dp

private val fadeSpec = tween<Float>(
    durationMillis = MotionTokens.DurationShort2,
    easing = MotionTokens.Standard,
)

private val scaleSpec = tween<Float>(
    durationMillis = MotionTokens.DurationShort2,
    easing = MotionTokens.StandardDecelerate,
)

private val pushEnter: EnterTransition = fadeIn(fadeSpec)
private val pushExit: ExitTransition = ExitTransition.None
private val popEnter: EnterTransition = EnterTransition.None
private val popExit: ExitTransition =
    scaleOut(animationSpec = scaleSpec, targetScale = BackPreviewScale)

@Composable
fun App() {
    var themeMode by remember { mutableStateOf(ThemeMode.System) }
    var useDynamicColor by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        AddonRepository.load()
        P2pRepository.load()
        PlayerRepository.load()
    }

    AppTheme(themeMode = themeMode, useDynamicColor = useDynamicColor) {
        val navController = rememberNavController()

        NavHost(
            navController = navController,
            startDestination = Routes.Home,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { pushEnter },
            exitTransition = { pushExit },
            popEnterTransition = { popEnter },
            popExitTransition = { popExit },
        ) {
            composable(Routes.Home) {
                PageSurface(applyBottomInset = false) {
                    HomeScreen(
                        onOpenSearch = { navController.navigate(Routes.Search) },
                        onOpenSettings = { navController.navigate(Routes.Settings) },
                        onPlaySample = { navController.navigate(Routes.Player) },
                        onAddAddons = { navController.navigate(Routes.SettingsAddons) },
                    )
                }
            }

            // Deliberately not wrapped in PageSurface. That applies a shape, and a shape
            // on a Surface always clips, which a SurfaceView cannot survive. The player
            // paints its own black background instead.
            composable(Routes.Player) {
                PlayerScreen(
                    url = Routes.SampleVideoUrl,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.Search) {
                PageSurface {
                    SearchPage(
                        onBack = { navController.popBackStack() },
                        onAddAddons = { navController.navigate(Routes.SettingsAddons) },
                    )
                }
            }

            val settings: @Composable (SettingsCategory?) -> Unit = { category ->
                SettingsPage(
                    onBack = { navController.popBackStack() },
                    initialCategory = category,
                    themeMode = themeMode,
                    onThemeModeChange = { themeMode = it },
                    useDynamicColor = useDynamicColor,
                    onUseDynamicColorChange = { useDynamicColor = it },
                    dynamicColorAvailable = isDynamicColorSupported(),
                )
            }

            composable(Routes.Settings) {
                PageSurface { settings(null) }
            }

            composable(Routes.SettingsAddons) {
                PageSurface { settings(SettingsCategory.Addons) }
            }
        }
    }
}

/**
 * Opaque backing for a destination. Material's Surface also blocks pointer events, so a
 * page that covers the navigation swallows touches meant for it.
 *
 * The corner radius is driven by this destination's own enter/exit transition, which
 * Navigation Compose seeks with the back gesture, so the page rounds off as it is dragged
 * away and squares up again if the gesture is cancelled.
 *
 * Home declines the bottom inset because its own navigation bar already applies one.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun AnimatedVisibilityScope.PageSurface(
    applyBottomInset: Boolean = true,
    content: @Composable () -> Unit,
) {
    val cornerRadius by transition.animateDp(
        transitionSpec = {
            tween(
                durationMillis = MotionTokens.DurationShort2,
                easing = MotionTokens.StandardDecelerate,
            )
        },
        label = "backCorner",
    ) { state ->
        if (state == EnterExitState.Visible) 0.dp else BackPreviewCornerRadius
    }

    Page(
        applyBottomInset = applyBottomInset,
        shape = RoundedCornerShape(cornerRadius),
        content = content,
    )
}

@Composable
private fun Page(
    applyBottomInset: Boolean,
    shape: Shape,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (applyBottomInset) {
                    Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                } else {
                    Modifier
                },
            ),
        shape = shape,
        color = MaterialTheme.colorScheme.background,
        content = content,
    )
}
