package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal expect fun NativeModalSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    containerColor: Color,
    contentColor: Color,
    showDragHandle: Boolean,
    fullHeight: Boolean,
    content: @Composable ColumnScope.() -> Unit,
)

internal expect val usesNativeBottomSheet: Boolean

internal expect fun dismissNativeBottomSheet()
