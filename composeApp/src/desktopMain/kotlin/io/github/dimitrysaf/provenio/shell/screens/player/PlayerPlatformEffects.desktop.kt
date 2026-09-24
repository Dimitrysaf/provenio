package io.github.dimitrysaf.provenio.shell.screens.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

// A desktop window has no orientation, system bars or picture-in-picture mode.
@Composable
actual fun LockPlayerToLandscape() = Unit

/** An in-window layer over everything else, rather than a second window. */
@Composable
actual fun FullscreenPlayerDialog(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true, dismissOnClickOutside = false),
    ) {
        Box(Modifier.fillMaxSize()) { content() }
    }
}

@Composable
actual fun HidePlayerSystemBars() = Unit

@Composable
actual fun EnterImmersivePlayerMode(keepScreenAwake: Boolean) = Unit

@Composable
actual fun ManagePlayerPictureInPicture(
    isPlaying: Boolean,
    videoSize: IntSize,
) = Unit

@Composable
actual fun rememberIsInPictureInPicture(): Boolean = false

// Brightness and volume swipes are touch gestures; the mouse wheel and keys cover them here.
@Composable
actual fun rememberPlayerGestureController(): PlayerGestureController? = null
