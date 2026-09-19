package com.nuvio.app.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
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
fun NuvioModalBottomSheet(
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
    if (usesNativeNuvioBottomSheet) {
        NuvioNativeModalBottomSheet(
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

@Composable
fun NuvioBottomSheetDivider(
    modifier: Modifier = Modifier,
) {
    HorizontalDivider(
        modifier = modifier,
        color = MaterialTheme.nuvio.colors.borderSubtle,
    )
}

@Composable
fun NuvioBottomSheetActionRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
) {
    val tokens = MaterialTheme.nuvio
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = tokens.spacing.screenHorizontal, vertical = tokens.spacing.screenHorizontal),
        horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s14),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tokens.colors.accent,
                modifier = Modifier.size(NuvioTokens.Icon.md),
            )
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            color = tokens.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        trailingContent?.invoke(this)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
suspend fun dismissNuvioBottomSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
) {
    if (usesNativeNuvioBottomSheet) {
        dismissNativeNuvioBottomSheet()
    } else if (sheetState.isVisible) {
        sheetState.hide()
    }
    onDismiss()
}

