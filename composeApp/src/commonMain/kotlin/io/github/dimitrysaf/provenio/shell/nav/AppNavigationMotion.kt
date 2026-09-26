package io.github.dimitrysaf.provenio.shell.nav

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import io.github.dimitrysaf.provenio.shell.components.M3Motion

internal class AppNavigationMotion(private val slidePx: Int) {
    val transitionSpec: AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform = {
        M3Motion.sharedAxisX(forward = true, slidePx = slidePx)
    }
    val popTransitionSpec: AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform = {
        M3Motion.sharedAxisX(forward = false, slidePx = slidePx)
    }
    val predictivePopTransitionSpec: AnimatedContentTransitionScope<Scene<NavKey>>.(Int) -> ContentTransform = {
        M3Motion.predictiveBack()
    }
}

@Composable
internal fun rememberAppNavigationMotion(): AppNavigationMotion {
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    return remember(density, rtl) {
        with(density) {
            AppNavigationMotion(slidePx = M3Motion.SharedAxisSlide.roundToPx() * if (rtl) -1 else 1)
        }
    }
}
