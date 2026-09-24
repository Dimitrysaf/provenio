package io.github.dimitrysaf.provenio.core.p2p

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual object P2pStreamingEngine {
    private val _state = MutableStateFlow<P2pStreamingState>(P2pStreamingState.Idle)
    actual val state: StateFlow<P2pStreamingState> = _state.asStateFlow()
    private val _cacheState = MutableStateFlow(P2pCacheUiState())
    actual val cacheState: StateFlow<P2pCacheUiState> = _cacheState.asStateFlow()
    // The iOS engine build does not expose per-torrent details yet.
    actual val torrentDetails: StateFlow<P2pTorrentDetails?> =
        MutableStateFlow<P2pTorrentDetails?>(null).asStateFlow()
    actual fun acquireTorrentDetails() = Unit
    actual fun releaseTorrentDetails() = Unit

    actual suspend fun startStream(request: P2pStreamRequest): String {
        val message = "P2P streaming is not available on this platform"
        _state.value = P2pStreamingState.Error(message)
        throw P2pStreamingException(message)
    }

    actual suspend fun clearCache(): P2pCacheClearResult =
        P2pCacheClearResult(reclaimedBytes = 0L, remainingBytes = 0L, protectedBytes = 0L)

    actual fun stopStream() {
        _state.value = P2pStreamingState.Idle
    }

    actual fun shutdown() {
        _state.value = P2pStreamingState.Idle
    }
}
