package io.github.dimitrysaf.provenio.shell

import androidx.compose.foundation.LocalContextMenuRepresentation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import io.github.dimitrysaf.provenio.desktop.DesktopDisplayScale
import io.github.dimitrysaf.provenio.desktop.DesktopWindowState
import io.github.dimitrysaf.provenio.desktop.ProvideDesktopDisplayScale
import io.github.dimitrysaf.provenio.shell.components.AppSnackbarHost
import io.github.dimitrysaf.provenio.desktop.MaterialContextMenuRepresentation
import org.jetbrains.compose.resources.painterResource
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.app_icon_arctic_blue

internal actual val playerWindowSupported: Boolean = true

private object PlayerWindowGeometry {
    var size: DpSize = DesktopDisplayScale.initialWindowSize(DpSize(1280.dp, 720.dp))
    var position: WindowPosition = WindowPosition.PlatformDefault
}

@Composable
internal actual fun DetachedPlayerWindow(
    title: String,
    onCloseRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    val state = rememberWindowState(
        size = PlayerWindowGeometry.size,
        position = PlayerWindowGeometry.position,
    )
    val fullscreen by DesktopWindowState.isPlayerFullscreen.collectAsState()
    LaunchedEffect(fullscreen) {
        state.placement = if (fullscreen) WindowPlacement.Fullscreen else WindowPlacement.Floating
    }
    LaunchedEffect(state.isMinimized) {
        DesktopWindowState.setPlayerWindowVisible(!state.isMinimized)
    }
    DisposableEffect(Unit) {
        DesktopWindowState.setPlayerWindowOpen(true)
        onDispose {
            if (state.placement == WindowPlacement.Floating) {
                PlayerWindowGeometry.size = state.size
                PlayerWindowGeometry.position = state.position
            }
            if (DetachedPlayer.route.value == null) {
                DesktopWindowState.setPlayerWindowOpen(false)
                DesktopWindowState.setPlayerFullscreen(false)
            }
        }
    }
    Window(
        onCloseRequest = onCloseRequest,
        state = state,
        title = title.ifBlank { "Provenio" },
        icon = painterResource(Res.drawable.app_icon_arctic_blue),
        onPreviewKeyEvent = { event ->
            when {
                event.type != KeyEventType.KeyDown -> false
                event.key == Key.F11 -> {
                    DesktopWindowState.setPlayerFullscreen(!fullscreen)
                    true
                }
                event.key == Key.Escape && fullscreen -> {
                    DesktopWindowState.setPlayerFullscreen(false)
                    true
                }
                else -> false
            }
        },
    ) {
        ProvideDesktopDisplayScale {
            CompositionLocalProvider(LocalContextMenuRepresentation provides MaterialContextMenuRepresentation) {
                AppThemeEnvironment {
                    Box(modifier = Modifier.fillMaxSize()) {
                        content()
                        AppSnackbarHost(modifier = Modifier.align(Alignment.BottomCenter))
                    }
                }
            }
        }
    }
}
