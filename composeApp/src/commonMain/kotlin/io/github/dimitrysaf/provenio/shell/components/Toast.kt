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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow

/** A snackbar's text, with a button that runs [onAction] when [actionLabel] is given. */
data class ToastMessage(
    val text: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
)

/** Brief messages that go away on their own, shown as a Material 3 snackbar by the app's root. */
object ToastController {
    // Conflated so a burst of messages shows only the latest, and each reaches exactly one host.
    private val messages = Channel<ToastMessage>(Channel.CONFLATED)
    private val dismissals = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    fun show(message: String) {
        messages.trySend(ToastMessage(message))
    }

    /** Shows a message with an action; it stays up longer so there is time to press it. */
    fun show(message: ToastMessage) {
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
    val windowToken = LocalWindowToken.current
    LaunchedEffect(hostState, windowToken) {
        ActiveWindow.token.collectLatest { activeWindow ->
            if (activeWindow != null && windowToken != null && activeWindow !== windowToken) return@collectLatest
            // A newer message replaces the one on screen instead of queueing behind it.
            ToastController.incoming.collectLatest { message ->
                val result = hostState.showSnackbar(
                    message = message.text,
                    actionLabel = message.actionLabel,
                    duration = if (message.actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short,
                )
                if (result == SnackbarResult.ActionPerformed) message.onAction?.invoke()
            }
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
    ) { data ->
        key(data) { SwipeableSnackbar(data) }
    }
}

/** A snackbar that a swipe to either side dismisses. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableSnackbar(data: SnackbarData) {
    val state = rememberSwipeToDismissBoxState()
    LaunchedEffect(state.currentValue) {
        if (state.currentValue != SwipeToDismissBoxValue.Settled) data.dismiss()
    }
    SwipeToDismissBox(state = state, backgroundContent = {}) {
        Snackbar(snackbarData = data)
    }
}

// How much of the bottom edge app chrome such as the navigation bar covers, so snackbars sit above it.
internal object SnackbarAnchor {
    val bottomInset = mutableStateOf(0.dp)
}
