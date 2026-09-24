package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal expect val platformExtraTopPadding: Dp

internal expect val platformExtraBottomPadding: Dp

@Composable
internal expect fun bottomNavigationBarInsets(): WindowInsets

/** Physical display-safe top inset, excluding any enclosing native toolbar. */
@Composable
internal expect fun platformPhysicalTopInset(): Dp

internal val LocalBottomNavigationOverlayPadding = staticCompositionLocalOf { 0.dp }

@Composable
internal fun safeBottomPadding(extra: Dp = 0.dp): Dp {
	val navigationBarBottom = bottomNavigationBarInsets()
		.asPaddingValues()
		.calculateBottomPadding()
	return navigationBarBottom.coerceAtLeast(platformExtraBottomPadding) +
		LocalBottomNavigationOverlayPadding.current +
		extra
}
