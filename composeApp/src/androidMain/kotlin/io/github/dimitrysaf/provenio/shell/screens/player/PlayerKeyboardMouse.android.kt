package io.github.dimitrysaf.provenio.shell.screens.player

import androidx.compose.ui.Modifier

internal actual fun togglePlayerFullscreen(): Boolean = false

internal actual fun Modifier.playerCursorHidden(hidden: Boolean): Modifier = this
