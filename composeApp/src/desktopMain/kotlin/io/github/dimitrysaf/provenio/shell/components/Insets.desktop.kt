package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// A desktop window has no system bars drawn over its content.
internal actual val platformExtraTopPadding: Dp = 0.dp
internal actual val platformExtraBottomPadding: Dp = 0.dp

@Composable
internal actual fun bottomNavigationBarInsets(): WindowInsets = WindowInsets(0, 0, 0, 0)

@Composable
internal actual fun platformPhysicalTopInset(): Dp = 0.dp
