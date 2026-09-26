package io.github.dimitrysaf.provenio.shell.components

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Composable
internal actual fun PlatformPredictiveBackHandler(
    enabled: Boolean,
    onBack: suspend (Flow<BackGestureEvent>) -> Unit,
) {
    PredictiveBackHandler(enabled = enabled) { events ->
        onBack(events.map { BackGestureEvent(it.progress, it.swipeEdge == BackEventCompat.EDGE_RIGHT) })
    }
}
