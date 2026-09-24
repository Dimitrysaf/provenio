package io.github.dimitrysaf.provenio.core.playback


internal fun shouldDeliverFlingStopClick(
    downConsumed: Boolean,
    movement: Float,
    touchSlop: Float,
    pointerUp: Boolean,
): Boolean = downConsumed && pointerUp && movement <= touchSlop
