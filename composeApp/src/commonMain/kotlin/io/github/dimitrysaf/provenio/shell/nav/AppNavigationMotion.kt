package io.github.dimitrysaf.provenio.shell.nav

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import io.github.dimitrysaf.provenio.shell.components.M3Motion
import io.github.dimitrysaf.provenio.shell.components.predictiveBackCorners

// The swipe edge value for a gesture from the right, as NavDisplay reports it.
private const val SwipeEdgeRight = 1

internal class AppNavigationMotion(private val slidePx: Int, private val marginPx: Int) {
    val transitionSpec: AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform = {
        M3Motion.sharedAxisX(forward = true, slidePx = slidePx)
    }
    val popTransitionSpec: AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform = {
        M3Motion.sharedAxisX(forward = false, slidePx = slidePx)
    }
    val predictivePopTransitionSpec: AnimatedContentTransitionScope<Scene<NavKey>>.(Int) -> ContentTransform = { edge ->
        M3Motion.predictiveBack(fromRightEdge = edge == SwipeEdgeRight, marginPx = marginPx)
    }
}

@Composable
internal fun rememberAppNavigationMotion(): AppNavigationMotion {
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    return remember(density, rtl) {
        with(density) {
            AppNavigationMotion(
                slidePx = M3Motion.SharedAxisSlide.roundToPx() * if (rtl) -1 else 1,
                marginPx = M3Motion.PredictiveBackMargin.roundToPx(),
            )
        }
    }
}

// Rounds a page's corners while it leaves, so the back preview shows it as a card; sheets draw their own shape.
internal fun predictiveBackCornersDecorator(): NavEntryDecorator<NavKey> = NavEntryDecorator<NavKey>(
    decorate = { entry ->
        if (entry.metadata[SheetRouteMetadataKey] == true) {
            entry.Content()
        } else {
            val corners = LocalNavAnimatedContentScope.current.transition.predictiveBackCorners()
            Box(modifier = Modifier.fillMaxSize().then(corners)) { entry.Content() }
        }
    },
)
