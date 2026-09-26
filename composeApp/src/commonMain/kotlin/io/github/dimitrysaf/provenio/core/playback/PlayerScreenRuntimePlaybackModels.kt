package io.github.dimitrysaf.provenio.core.playback

import io.github.dimitrysaf.provenio.core.tracking.TrackingMediaReference
import io.github.dimitrysaf.provenio.core.tracking.buildTrackingMediaReference

internal data class TrackingScrobbleItemInputs(
    val contentType: String,
    val parentMetaId: String,
    val videoId: String?,
    val title: String,
    val seasonNumber: Int?,
    val episodeNumber: Int?,
    val episodeTitle: String?,
)

internal fun shouldSendStopScrobble(
    hasActiveScrobble: Boolean,
    progressPercent: Float,
): Boolean = hasActiveScrobble || progressPercent >= 80f

internal fun shouldUpdateTrackingScrobbleAfterSeek(
    hasActiveScrobble: Boolean,
    progressPercent: Float,
): Boolean = hasActiveScrobble && progressPercent >= 1f && progressPercent < 80f

// A trailer gets no identity, so tracking services never hear about it.
internal fun TrackingScrobbleItemInputs.buildMedia(): TrackingMediaReference =
    buildTrackingMediaReference(
        contentType = contentType,
        parentMetaId = if (contentType == TrailerContentType) "" else parentMetaId,
        videoId = videoId.takeUnless { contentType == TrailerContentType },
        title = title.takeUnless { contentType == TrailerContentType },
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber,
        episodeTitle = episodeTitle,
    )
