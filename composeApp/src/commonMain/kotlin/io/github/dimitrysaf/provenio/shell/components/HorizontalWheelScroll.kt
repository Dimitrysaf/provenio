package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

fun Modifier.horizontalWheelScroll(state: ScrollableState): Modifier = pointerInput(state) {
    val stepPx = WheelStep.toPx()
    var remaining = 0f
    var job: Job? = null
    coroutineScope {
        val scope: CoroutineScope = this
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                val change = event.verticalWheelChange() ?: continue
                val forward = change.scrollDelta.y > 0
                if (forward && !state.canScrollForward || !forward && !state.canScrollBackward) continue
                change.consume()
                remaining += change.scrollDelta.y * stepPx
                if (job?.isActive != true) {
                    job = scope.launch {
                        state.scroll {
                            while (abs(remaining) > 0.5f) {
                                val step = if (abs(remaining) < 2f) remaining else remaining * WheelEasing
                                val consumed = scrollBy(step)
                                remaining -= step
                                if (abs(consumed) < abs(step) / 2) break
                                withFrameNanos { }
                            }
                            remaining = 0f
                        }
                    }
                }
            }
        }
    }
}

fun Modifier.horizontalWheelPaging(
    key: Any,
    canStep: (forward: Boolean) -> Boolean,
    step: suspend (forward: Boolean) -> Unit,
): Modifier = pointerInput(key) {
    var accumulated = 0f
    var job: Job? = null
    coroutineScope {
        val scope: CoroutineScope = this
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                val change = event.verticalWheelChange() ?: continue
                val forward = change.scrollDelta.y > 0
                if (!canStep(forward)) {
                    accumulated = 0f
                    continue
                }
                change.consume()
                if (job?.isActive == true) continue
                if (accumulated != 0f && (accumulated > 0) != forward) accumulated = 0f
                accumulated += change.scrollDelta.y
                if (abs(accumulated) < 1f) continue
                accumulated = 0f
                job = scope.launch { step(forward) }
            }
        }
    }
}

fun Modifier.horizontalScrollWithWheel(state: ScrollState): Modifier =
    horizontalWheelScroll(state).horizontalScroll(state)

private fun PointerEvent.verticalWheelChange(): PointerInputChange? {
    if (type != PointerEventType.Scroll) return null
    val change = changes.firstOrNull() ?: return null
    if (change.isConsumed) return null
    val delta: Offset = change.scrollDelta
    if (delta.y == 0f || abs(delta.y) <= abs(delta.x)) return null
    return change
}

private val WheelStep = 96.dp
private const val WheelEasing = 0.3f
