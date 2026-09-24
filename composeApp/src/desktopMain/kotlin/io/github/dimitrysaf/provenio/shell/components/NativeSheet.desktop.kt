package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

// Desktop uses the Material bottom sheet; the native sheet is an iOS presentation.
internal actual val usesNativeBottomSheet: Boolean = false

@Composable
internal actual fun NativeModalSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    containerColor: Color,
    contentColor: Color,
    showDragHandle: Boolean,
    fullHeight: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) = Unit

internal actual fun dismissNativeBottomSheet() = Unit
