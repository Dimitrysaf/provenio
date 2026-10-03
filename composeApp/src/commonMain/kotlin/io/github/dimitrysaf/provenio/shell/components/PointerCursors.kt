package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties

internal enum class PointerCursorKind { Default, Hand, Blocked, Text }

@Composable
internal expect fun rememberPointerCursorTracking(onCursor: (PointerCursorKind) -> Unit): Modifier

internal expect fun blockedPointerIcon(): PointerIcon?

@Composable
fun PointerCursorHost(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    var cursor by remember { mutableStateOf(PointerCursorKind.Default) }
    val tracking = rememberPointerCursorTracking { cursor = it }
    Box(
        modifier = modifier
            .then(tracking)
            .pointerHoverIcon(cursor.pointerIcon()),
        content = content,
    )
}

internal fun SemanticsNode.pointerCursorAt(
    position: Offset,
    bounds: SemanticsNode.() -> Rect,
): PointerCursorKind? {
    if (!bounds().contains(position)) return null
    for (child in children.asReversed()) {
        child.pointerCursorAt(position, bounds)?.let { return it }
    }
    return config.pointerCursorKind()
}

internal fun SemanticsNode.coversPointer(
    position: Offset,
    bounds: SemanticsNode.() -> Rect,
): Boolean = children.any { it.bounds().contains(position) }

private fun SemanticsConfiguration.pointerCursorKind(): PointerCursorKind? {
    val interactive = SemanticsActions.OnClick in this ||
        SemanticsActions.OnLongClick in this ||
        SemanticsActions.SetProgress in this
    return when {
        SemanticsProperties.Disabled in this && (interactive || SemanticsProperties.EditableText in this) ->
            PointerCursorKind.Blocked
        SemanticsProperties.EditableText in this -> PointerCursorKind.Text
        interactive -> PointerCursorKind.Hand
        else -> null
    }
}

internal fun PointerCursorKind.pointerIcon(): PointerIcon = when (this) {
    PointerCursorKind.Default -> PointerIcon.Default
    PointerCursorKind.Hand -> PointerIcon.Hand
    PointerCursorKind.Text -> PointerIcon.Text
    PointerCursorKind.Blocked -> blockedPointerIcon() ?: PointerIcon.Default
}
