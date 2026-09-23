package com.nuvio.app.shell.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The five Material breakpoints, keyed off window width.
 *
 * m3.material.io/foundations/layout/breakpoints/overview
 */
enum class WindowBreakpoint {
    Compact,
    Medium,
    Expanded,
    Large,
    ExtraLarge,
    ;

    /** Leading and trailing window margin: 16dp at compact, 24dp everywhere above it. */
    val margin: Dp get() = if (this == Compact) 16.dp else 24.dp

    /**
     * Two panes are recommended from expanded upwards. Compact and medium stay single-pane, so a
     * second pane's content has to fall back to a full-window destination there.
     */
    val isTwoPane: Boolean get() = this >= Expanded

    /**
     * Default width of the fixed pane in a fixed-and-flexible layout: 360dp at expanded, 412dp at
     * large and extra-large.
     */
    val fixedPaneWidth: Dp get() = if (this == Expanded) 360.dp else 412.dp

    companion object {
        /** Spacer between panes; 24dp at every breakpoint that shows more than one. */
        val PaneSpacer: Dp = 24.dp

        fun forWidth(width: Dp): WindowBreakpoint = when {
            width < 600.dp -> Compact
            width < 840.dp -> Medium
            width < 1200.dp -> Expanded
            width < 1600.dp -> Large
            else -> ExtraLarge
        }
    }
}
