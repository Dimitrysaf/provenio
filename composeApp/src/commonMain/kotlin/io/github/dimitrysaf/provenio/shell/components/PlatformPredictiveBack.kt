package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow

internal data class BackGestureEvent(val progress: Float, val fromRightEdge: Boolean)

// Completing the flow commits the back, and cancelling the collecting coroutine means the gesture was abandoned.
@Composable
internal expect fun PlatformPredictiveBackHandler(
    enabled: Boolean,
    onBack: suspend (Flow<BackGestureEvent>) -> Unit,
)
