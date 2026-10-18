package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.app_fullscreen_enter
import provenio.composeapp.generated.resources.app_fullscreen_exit

/**
 * Whether the app fills the whole screen. Each platform applies it: a desktop window goes
 * fullscreen as with F11, and phones and tablets hide their status and navigation bars.
 */
object AppFullscreen {
    private val state = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> = state.asStateFlow()

    fun set(value: Boolean) {
        state.value = value
    }

    fun toggle() {
        state.value = !state.value
    }
}

/** The button beside the large screen search box that turns fullscreen on and off. */
@Composable
fun FullscreenButton(modifier: Modifier = Modifier) {
    val fullscreen by AppFullscreen.isFullscreen.collectAsState()
    val label = stringResource(if (fullscreen) Res.string.app_fullscreen_exit else Res.string.app_fullscreen_enter)
    WithTooltip(label) {
        FilledTonalIconButton(onClick = AppFullscreen::toggle, modifier = modifier) {
            Icon(
                imageVector = if (fullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                contentDescription = label,
            )
        }
    }
}
