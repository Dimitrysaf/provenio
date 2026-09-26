package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.WindowInsetsSides
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
    // Above the navigation bar while one is showing, otherwise just clear of the system bars.
    val safeBottom = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()
    SnackbarHost(
        hostState = hostState,
        modifier = modifier
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(bottom = maxOf(SnackbarAnchor.bottomInset.value, safeBottom)),
    )
}

// How much of the bottom edge app chrome such as the navigation bar covers, so snackbars sit above it.
internal object SnackbarAnchor {
    val bottomInset = mutableStateOf(0.dp)
}
