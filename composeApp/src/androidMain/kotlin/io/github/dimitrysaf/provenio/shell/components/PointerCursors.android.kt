package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.LocalView

@Composable
internal actual fun rememberPointerCursorTracking(onCursor: (PointerCursorKind) -> Unit): Modifier {
    val root = LocalView.current as? RootForTest ?: return Modifier
    val currentOnCursor by rememberUpdatedState(onCursor)
    return Modifier.pointerInput(root) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull() ?: continue
                if (change.type != PointerType.Mouse) continue
                val kind = if (event.type == PointerEventType.Exit) {
                    PointerCursorKind.Default
                } else {
                    root.semanticsOwner.unmergedRootSemanticsNode
                        .pointerCursorAt(change.position) { boundsInRoot }
                        ?: PointerCursorKind.Default
                }
                currentOnCursor(kind)
            }
        }
    }
}

internal actual fun blockedPointerIcon(): PointerIcon? = BlockedPointerIcon

private val BlockedPointerIcon = PointerIcon(android.view.PointerIcon.TYPE_NO_DROP)
