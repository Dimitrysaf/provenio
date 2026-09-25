package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow

/** Brief messages that go away on their own, shown as a Material 3 snackbar by the app's root. */
object ToastController {
    // Conflated so a burst of messages shows only the latest, and each reaches exactly one host.
    private val messages = Channel<String>(Channel.CONFLATED)
    private val dismissals = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    fun show(message: String) {
        messages.trySend(message)
    }

    fun dismiss() {
        dismissals.tryEmit(Unit)
    }

    internal val incoming = messages.receiveAsFlow()
    internal val dismissRequests = dismissals
}

/** Shows ToastController's messages; the app places one at its root, above every screen. */
@Composable
fun AppSnackbarHost(modifier: Modifier = Modifier) {
    val hostState = remember { SnackbarHostState() }
    LaunchedEffect(hostState) {
        // A newer message replaces the one on screen instead of queueing behind it.
        ToastController.incoming.collectLatest { message ->
            hostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
        }
    }
    LaunchedEffect(hostState) {
        ToastController.dismissRequests.collect { hostState.currentSnackbarData?.dismiss() }
    }
    SnackbarHost(
        hostState = hostState,
        modifier = modifier.windowInsetsPadding(WindowInsets.safeDrawing),
    )
}
