package io.github.dimitrysaf.provenio.ui.responsive

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WindowSizeClass { Compact, Medium, Expanded }

fun windowSizeClassOf(width: Dp): WindowSizeClass = when {
    width < 600.dp -> WindowSizeClass.Compact
    width < 840.dp -> WindowSizeClass.Medium
    else -> WindowSizeClass.Expanded
}

/** Side padding as a percentage of the available width, scaled by breakpoint. */
fun contentHorizontalPadding(width: Dp): Dp {
    val percentOfWidth = when (windowSizeClassOf(width)) {
        WindowSizeClass.Compact -> 0.04f
        WindowSizeClass.Medium -> 0.08f
        WindowSizeClass.Expanded -> 0.16f
    }
    return width * percentOfWidth
}

/** Caps content width on very wide (desktop) windows so text/lists stay readable. */
val maxContentWidth: Dp = 960.dp
