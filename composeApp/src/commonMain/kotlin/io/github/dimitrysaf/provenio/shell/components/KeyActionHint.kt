package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.core.build.isDesktop
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.key_hint_more_options_keyboard
import provenio.composeapp.generated.resources.key_hint_more_options_remote

/**
 * Whether what has focus has more actions than a press. With a finger or a mouse they are a long
 * press or a right-click away, and need no telling; with a remote or a keyboard the item says how
 * to reach them while it has focus. Only something focused with keys shows it, as nothing takes
 * focus from a touch or a click.
 */
internal object KeyActionHint {
    private var owner by mutableStateOf<Any?>(null)

    val visible: Boolean
        get() = owner != null

    fun show(token: Any) {
        owner = token
    }

    fun hide(token: Any) {
        if (owner === token) owner = null
    }
}

/**
 * The hint for [KeyActionHint], at the foot of the window. It waits a moment before it shows, so
 * moving across a row of posters does not flash it at each one.
 */
@Composable
fun KeyActionHintHost(modifier: Modifier = Modifier) {
    val focused = KeyActionHint.visible
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(focused) {
        if (focused) {
            delay(HintDelayMillis)
            shown = true
        } else {
            shown = false
        }
    }
    AnimatedVisibility(
        visible = shown,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(bottom = 24.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        ) {
            Text(
                text = stringResource(
                    if (isDesktop) Res.string.key_hint_more_options_keyboard else Res.string.key_hint_more_options_remote,
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

private const val HintDelayMillis = 600L
