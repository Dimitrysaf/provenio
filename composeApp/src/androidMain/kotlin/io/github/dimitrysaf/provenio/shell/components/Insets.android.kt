package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal actual val platformExtraTopPadding: Dp = 0.dp

internal actual val platformExtraBottomPadding: Dp = 0.dp

@Composable
internal actual fun bottomNavigationBarInsets(): WindowInsets = WindowInsets.navigationBars

@Composable
internal actual fun platformPhysicalTopInset(): Dp =
    WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
