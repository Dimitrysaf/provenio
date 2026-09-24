package io.github.dimitrysaf.provenio.core.playback

// Handing a stream to another player is not wired up on desktop yet.
internal actual object ExternalPlayerPlatform {
    actual fun defaultPlayerId(): String? = null

    actual fun availablePlayers(): List<ExternalPlayerApp> = emptyList()

    actual fun open(
        request: ExternalPlayerPlaybackRequest,
        playerId: String?,
    ): ExternalPlayerOpenResult = ExternalPlayerOpenResult.NoPlayerAvailable

    actual fun buildIntent(
        request: ExternalPlayerPlaybackRequest,
        playerId: String?,
    ): ExternalPlayerIntentResult = ExternalPlayerIntentResult.NotConfigured
}
