package io.github.dimitrysaf.provenio.ui.backhandler

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import kotlin.coroutines.cancellation.CancellationException

@Composable
actual fun SystemBackHandler(
    enabled: Boolean,
    onProgress: (BackGesture) -> Unit,
    onCancel: () -> Unit,
    onBack: () -> Unit,
) {
    // The handler lambda is suspended across the whole gesture, so it captures the
    // callbacks from the composition that started it; keep them current.
    val currentProgress by rememberUpdatedState(onProgress)
    val currentCancel by rememberUpdatedState(onCancel)
    val currentBack by rememberUpdatedState(onBack)

    PredictiveBackHandler(enabled = enabled) { events ->
        try {
            events.collect { event ->
                currentProgress(
                    BackGesture(
                        progress = event.progress,
                        fromStart = event.swipeEdge == BackEventCompat.EDGE_LEFT,
                    ),
                )
            }
            currentBack()
        } catch (cancellation: CancellationException) {
            currentCancel()
            throw cancellation
        }
    }
}
