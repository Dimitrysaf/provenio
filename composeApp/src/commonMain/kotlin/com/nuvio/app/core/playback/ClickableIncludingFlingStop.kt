package com.nuvio.app.core.playback

import androidx.compose.runtime.getValue

internal fun shouldDeliverFlingStopClick(
    downConsumed: Boolean,
    movement: Float,
    touchSlop: Float,
    pointerUp: Boolean,
): Boolean = downConsumed && pointerUp && movement <= touchSlop
