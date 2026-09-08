package io.github.dimitrysaf.provenio

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
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
 * Page transitions. Only ever one surface animates, and it animates over another that
 * stays fully opaque — cross-fading both at once leaves a window where neither is opaque
 * and the window background shows through as a dark flash.
 *
 * Forward, the arriving page fades in over the one it covers. Back, the leaving page
 * shrinks 100% → 90% over the destination already sitting behind it at full opacity,
 * which is the predictive-back preview for a full-screen surface. Navigation Compose
 * seeks [popExit] with the drag, so that shrink follows the gesture directly and what
 * shows through is the destination, never the background.
 *
 * https://developer.android.com/design/ui/mobile/guides/patterns/predictive-back
 */
private const val BackPreviewScale = 0.9f

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
