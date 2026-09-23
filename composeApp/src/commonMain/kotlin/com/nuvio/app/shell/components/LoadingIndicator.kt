package com.nuvio.app.shell.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LoadingIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/**
 * The Material loading indicator, Default (uncontained) configuration: a 38dp active indicator
 * morphing through its shape sequence inside a 48dp container, drawn in the `primary` role.
 * Every value here comes from [LoadingIndicatorDefaults] rather than being restated.
 *
 * m3.material.io/components/loading-indicator/specs
 *
 * [active] is still honoured. The indeterminate indicator animates forever, so when a screen goes
 * off-stage this falls back to the determinate overload pinned at a fixed progress: the same
 * shapes, drawn once, requesting no frames.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NuvioLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = LoadingIndicatorDefaults.indicatorColor,
    size: Dp = LoadingIndicatorDefaults.ContainerWidth,
    active: Boolean = LocalScreenActive.current,
) {
    if (active) {
        LoadingIndicator(
            modifier = modifier.size(size),
            color = color,
        )
    } else {
        LoadingIndicator(
            progress = { 0f },
            modifier = modifier.size(size),
            color = color,
        )
    }
}
