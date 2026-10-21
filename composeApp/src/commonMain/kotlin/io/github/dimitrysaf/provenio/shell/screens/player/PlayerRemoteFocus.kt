package io.github.dimitrysaf.provenio.shell.screens.player

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.focus.FocusRequester

/**
 * The play button's focus, for a remote: arrows or OK bring the controls up with focus on it, so
 * the arrow keys go on from there to the other controls.
 */
internal val LocalPlayerPrimaryControlFocus = staticCompositionLocalOf<FocusRequester?> { null }
