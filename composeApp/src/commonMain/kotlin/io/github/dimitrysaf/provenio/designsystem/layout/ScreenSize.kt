package io.github.dimitrysaf.provenio.designsystem.layout

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * M3 window width size classes. The Large and ExtraLarge breakpoints were added to the
 * spec after the original three and matter here because the desktop target normally
 * lands in them.
 *
 * https://developer.android.com/develop/ui/compose/layouts/adaptive/window-size-classes
 */
enum class WindowSizeClass { Compact, Medium, Expanded, Large, ExtraLarge }

fun windowSizeClassOf(width: Dp): WindowSizeClass = when {
    width < 600.dp -> WindowSizeClass.Compact
    width < 840.dp -> WindowSizeClass.Medium
    width < 1200.dp -> WindowSizeClass.Expanded
    width < 1600.dp -> WindowSizeClass.Large
    else -> WindowSizeClass.ExtraLarge
}

/** True once the window is wide enough for a rail instead of a bottom bar. */
fun WindowSizeClass.usesRail(): Boolean = this != WindowSizeClass.Compact

/** M3 recommends an expanded rail (or drawer) once there is this much width to spare. */
fun WindowSizeClass.usesExpandedRail(): Boolean =
    this == WindowSizeClass.Large || this == WindowSizeClass.ExtraLarge

/**
 * M3 layout margins: a fixed 16dp in compact, 24dp at every larger breakpoint.
 * Deliberately not a percentage of width — the spec is fixed dp.
 */
fun contentHorizontalPadding(width: Dp): Dp =
    if (windowSizeClassOf(width) == WindowSizeClass.Compact) 16.dp else 24.dp

/**
 * Readability cap for a single column of content on very wide windows. Not an M3 token —
 * the spec caps line length through pane layout rather than a fixed maximum.
 */
val maxContentWidth: Dp = 960.dp
