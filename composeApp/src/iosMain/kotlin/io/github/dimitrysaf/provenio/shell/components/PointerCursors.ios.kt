package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon

@Composable
internal actual fun rememberPointerCursorTracking(onCursor: (PointerCursorKind) -> Unit): Modifier = Modifier

internal actual fun blockedPointerIcon(): PointerIcon? = null
