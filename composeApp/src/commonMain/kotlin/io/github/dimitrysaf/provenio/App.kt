package io.github.dimitrysaf.provenio

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import io.github.dimitrysaf.provenio.navigation.Routes
import io.github.dimitrysaf.provenio.pages.AppearancePage
import io.github.dimitrysaf.provenio.pages.SearchPage
import io.github.dimitrysaf.provenio.pages.SettingsPage
import io.github.dimitrysaf.provenio.theme.AppTheme
import io.github.dimitrysaf.provenio.theme.MotionTokens
import io.github.dimitrysaf.provenio.theme.ThemeMode
import io.github.dimitrysaf.provenio.theme.isDynamicColorSupported
import io.github.dimitrysaf.provenio.ui.HomeScreen

/**
 * Page transition, per the predictive-back specification for full-screen surfaces.
 *
 * Going back, the leaving surface scales 100% → 90% and the destination scales 110% → 100%,
 * with the two fades crossing at [FadeThreshold] so neither is fully opaque at the halfway
 * point. Going forward is the same transform inverted. Navigation Compose seeks these
 * transitions with the back gesture, so the destination is on screen and scaling up while
 * the user drags — which is the whole point of the preview.
 *
 * https://developer.android.com/design/ui/mobile/guides/patterns/predictive-back
 */
private const val PageDuration = MotionTokens.DurationMedium2
private const val FadeThreshold = 0.35f
private const val ExitScale = 0.9f
private const val EnterScale = 1.1f

private val fadeThroughIn = fadeIn(
    animationSpec = keyframes {
        durationMillis = PageDuration
        0f at 0
        0f at (PageDuration * FadeThreshold).toInt()
        1f at PageDuration
    },
)

private val fadeThroughOut = fadeOut(
    animationSpec = keyframes {
        durationMillis = PageDuration
        1f at 0
        0f at (PageDuration * FadeThreshold).toInt()
        0f at PageDuration
    },
)

private val scaleSpec = tween<Float>(
    durationMillis = PageDuration,
    easing = MotionTokens.StandardDecelerate,
)

/** Forward: the arriving page grows into place, the one it covers recedes. */
private val pushEnter: EnterTransition =
    fadeThroughIn + scaleIn(animationSpec = scaleSpec, initialScale = ExitScale)
private val pushExit: ExitTransition =
    fadeThroughOut + scaleOut(animationSpec = scaleSpec, targetScale = EnterScale)

/** Back: the leaving page shrinks away, the destination settles down from above 100%. */
private val popEnter: EnterTransition =
    fadeThroughIn + scaleIn(animationSpec = scaleSpec, initialScale = EnterScale)
private val popExit: ExitTransition =
    fadeThroughOut + scaleOut(animationSpec = scaleSpec, targetScale = ExitScale)

@Composable
fun App() {
    var themeMode by remember { mutableStateOf(ThemeMode.System) }
    var useDynamicColor by remember { mutableStateOf(true) }

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
                Page(applyBottomInset = false) {
                    HomeScreen(
                        onOpenSearch = { navController.navigate(Routes.Search) },
                        onOpenSettings = { navController.navigate(Routes.SettingsGraph) },
                    )
                }
            }

            composable(Routes.Search) {
                Page {
                    SearchPage(onBack = { navController.popBackStack() })
                }
            }

            // Settings' categories are sub-pages of Settings, not siblings of it, so back
            // walks Appearance → Settings → Home rather than dropping straight home.
            navigation(route = Routes.SettingsGraph, startDestination = Routes.SettingsRoot) {
                composable(Routes.SettingsRoot) {
                    Page {
                        SettingsPage(
                            onBack = { navController.popBackStack() },
                            onOpenAppearance = {
                                navController.navigate(Routes.SettingsAppearance)
                            },
                        )
                    }
                }

                composable(Routes.SettingsAppearance) {
                    Page {
                        AppearancePage(
                            onBack = { navController.popBackStack() },
                            themeMode = themeMode,
                            onThemeModeChange = { themeMode = it },
                            useDynamicColor = useDynamicColor,
                            onUseDynamicColorChange = { useDynamicColor = it },
                            dynamicColorAvailable = isDynamicColorSupported(),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Opaque backing for a destination. Material's Surface also blocks pointer events, so a
 * page that covers the navigation swallows touches meant for it.
 *
 * Home declines the bottom inset because its own navigation bar already applies one.
 */
@Composable
private fun Page(
    applyBottomInset: Boolean = true,
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
        color = MaterialTheme.colorScheme.background,
        content = content,
    )
}
