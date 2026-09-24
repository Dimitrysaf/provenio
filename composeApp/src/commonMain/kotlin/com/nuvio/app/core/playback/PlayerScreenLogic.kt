package com.nuvio.app.core.playback

internal const val P2pInitialByteProgressMidpoint = 5_242_880L

internal const val P2pInitialBufferTargetMs = 10_000L

internal const val P2pInitialNetworkStageWeight = 0.45f

internal const val P2pInitialDeliveryStageWeight = 0.30f

internal const val P2pInitialPlayerStageStart = 0.75f

internal const val P2pInitialLoadingMaximum = 0.95f

internal fun p2pInitialLoadingProgress(
    bufferedAheadMs: Long,
    downloadedBytes: Long,
    deliveredBytes: Long,
): Float {
    val networkProgress = saturatingProgress(downloadedBytes, P2pInitialByteProgressMidpoint) *
        P2pInitialNetworkStageWeight
    val deliveryProgress = saturatingProgress(deliveredBytes, P2pInitialByteProgressMidpoint) *
        P2pInitialDeliveryStageWeight
    val engineProgress = networkProgress + deliveryProgress
    val playerProgress = if (bufferedAheadMs > 0L) {
        P2pInitialPlayerStageStart +
            (bufferedAheadMs.toFloat() / P2pInitialBufferTargetMs.toFloat()).coerceIn(0f, 1f) *
            (P2pInitialLoadingMaximum - P2pInitialPlayerStageStart)
    } else {
        0f
    }
    return maxOf(engineProgress, playerProgress).coerceIn(0f, P2pInitialLoadingMaximum)
}

private fun saturatingProgress(value: Long, midpoint: Long): Float {
    val safeValue = value.coerceAtLeast(0L).toDouble()
    return (safeValue / (safeValue + midpoint.toDouble())).toFloat()
}

internal const val NEXT_EPISODE_HARD_TIMEOUT_MS = 120_000L
