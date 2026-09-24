package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp

/**
 * The margin around a sheet's body.
 *
 * The bottom sheet spec measures only the sheet's outside — full window width up to 640dp, then
 * 56dp side margins and a 72dp top margin beyond that — and says nothing about what sits inside.
 * At 640dp and below a sheet is a compact pane, so its body takes the compact window margin.
 *
 * m3.material.io/components/bottom-sheets/specs
 */
internal val BottomSheetBodyMargin: Dp = WindowBreakpoint.Compact.margin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModalSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
    modifier: Modifier = Modifier,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    showDragHandle: Boolean = true,
    fullHeight: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (usesNativeBottomSheet) {
        NativeModalSheet(
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            containerColor = containerColor,
            contentColor = contentColor,
            showDragHandle = showDragHandle,
            fullHeight = fullHeight,
            content = content,
        )
    } else {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
            modifier = modifier,
            containerColor = containerColor,
            contentColor = contentColor,
            shape = shape,
            dragHandle = if (showDragHandle) {
                { BottomSheetDefaults.DragHandle() }
            } else {
                null
            },
            content = content,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
suspend fun dismissBottomSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
) {
    if (usesNativeBottomSheet) {
        dismissNativeBottomSheet()
    } else if (sheetState.isVisible) {
        sheetState.hide()
    }
    onDismiss()
}
