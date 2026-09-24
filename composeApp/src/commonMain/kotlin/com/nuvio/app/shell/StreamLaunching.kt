package com.nuvio.app.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.nuvio.app.core.debrid.DirectDebridPlayableResult
import com.nuvio.app.core.debrid.DirectDebridPlaybackResolver
import com.nuvio.app.core.debrid.toastMessage
import com.nuvio.app.core.metadata.MetaDetailsRepository
import com.nuvio.app.core.playback.PlayerLaunch
import com.nuvio.app.core.streams.CachedStreamLink
import com.nuvio.app.core.streams.StreamBehaviorHints
import com.nuvio.app.core.streams.StreamItem
import com.nuvio.app.core.streams.StreamLaunch
import com.nuvio.app.core.streams.StreamLinkCacheRepository
import com.nuvio.app.core.streams.StreamsRepository
import com.nuvio.app.shell.components.NuvioToastController
import com.nuvio.app.shell.screens.player.resolveContentLanguage
import com.nuvio.app.shell.screens.player.sanitizePlaybackHeaders
import com.nuvio.app.shell.screens.player.sanitizePlaybackResponseHeaders
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.debrid_resolving_stream
import org.jetbrains.compose.resources.getString

// The id to ask streams for, and whether an episode's own id has been looked up yet.
internal data class EpisodeVideoId(val value: String, val isResolved: Boolean)

// Episodes are launched with the series id; the addon wants the episode's own id, so look it up first.
@Composable
internal fun rememberEpisodeVideoId(launch: StreamLaunch): EpisodeVideoId {
    val shouldResolve = launch.parentMetaId != null &&
        launch.seasonNumber != null &&
        launch.episodeNumber != null
    var videoId by rememberSaveable(
        launch.videoId,
        launch.parentMetaId,
        launch.seasonNumber,
        launch.episodeNumber,
    ) {
        mutableStateOf(launch.videoId)
    }
    var isResolved by rememberSaveable(
        launch.videoId,
        launch.parentMetaId,
        launch.seasonNumber,
        launch.episodeNumber,
    ) {
        mutableStateOf(!shouldResolve)
    }

    LaunchedEffect(
        launch.videoId,
        launch.parentMetaId,
        launch.parentMetaType,
        launch.type,
        launch.seasonNumber,
        launch.episodeNumber,
    ) {
        videoId = launch.videoId
        if (!shouldResolve) {
            isResolved = true
            return@LaunchedEffect
        }

        isResolved = false
        val metaType = launch.parentMetaType ?: launch.type
        val metaId = launch.parentMetaId ?: return@LaunchedEffect
        val resolvedVideoId = runCatching {
            MetaDetailsRepository.fetch(metaType, metaId)
        }.getOrNull()
            ?.videos
            ?.firstOrNull { video ->
                video.season == launch.seasonNumber &&
                    video.episode == launch.episodeNumber
            }
            ?.id
            ?.takeIf { it.isNotBlank() }

        videoId = resolvedVideoId ?: launch.videoId
        isResolved = true
    }
    return EpisodeVideoId(videoId, isResolved)
}

internal fun StreamLaunch.linkCacheKey(videoId: String): String =
    StreamLinkCacheRepository.contentKey(
        type = type,
        videoId = videoId,
        parentMetaId = parentMetaId,
        season = seasonNumber,
        episode = episodeNumber,
    )

internal fun StreamLaunch.contentLanguage(videoId: String, fallbackLanguage: String? = null): String? {
    val meta = MetaDetailsRepository.peek(
        type = parentMetaType ?: type,
        id = parentMetaId ?: videoId,
    )
    return resolveContentLanguage(
        language = meta?.language?.takeIf { it.isNotBlank() } ?: fallbackLanguage,
        country = meta?.country,
    )
}

internal fun StreamLaunch.reloadStreams(videoId: String) {
    StreamsRepository.reload(
        type = type,
        videoId = videoId,
        parentMetaId = parentMetaId,
        season = seasonNumber,
        episode = episodeNumber,
        manualSelection = manualSelection,
    )
}

// Remembers a direct link so the next play of this title can skip the sources list.
internal fun StreamLaunch.cacheDirectLink(videoId: String, stream: StreamItem, sourceUrl: String) {
    StreamLinkCacheRepository.save(
        contentKey = linkCacheKey(videoId),
        url = sourceUrl,
        streamName = stream.streamLabel,
        addonName = stream.addonName,
        addonId = stream.addonId,
        requestHeaders = sanitizePlaybackHeaders(stream.behaviorHints.proxyHeaders?.request),
        responseHeaders = sanitizePlaybackResponseHeaders(stream.behaviorHints.proxyHeaders?.response),
        filename = stream.behaviorHints.filename,
        videoSize = stream.behaviorHints.videoSize,
        bingeGroup = stream.behaviorHints.bingeGroup,
        streamType = stream.streamType,
        contentLanguage = contentLanguage(videoId),
    )
}

// Remembers a torrent the same way; it has no URL, only its hash and file.
internal fun StreamLaunch.cacheP2pLink(videoId: String, stream: StreamItem, infoHash: String) {
    StreamLinkCacheRepository.save(
        contentKey = linkCacheKey(videoId),
        url = "",
        streamName = stream.streamLabel,
        addonName = stream.addonName,
        addonId = stream.addonId,
        requestHeaders = emptyMap(),
        responseHeaders = emptyMap(),
        filename = stream.behaviorHints.filename,
        videoSize = stream.behaviorHints.videoSize,
        infoHash = infoHash,
        fileIdx = stream.p2pFileIdx,
        sources = stream.sources,
        bingeGroup = stream.behaviorHints.bingeGroup,
        contentLanguage = contentLanguage(videoId),
    )
}

internal fun StreamLaunch.directPlayerLaunch(
    videoId: String,
    stream: StreamItem,
    sourceUrl: String,
    resumePositionMs: Long?,
    resumeProgressFraction: Float?,
): PlayerLaunch = PlayerLaunch(
    profileId = profileId,
    title = title,
    sourceUrl = sourceUrl,
    sourceHeaders = sanitizePlaybackHeaders(stream.behaviorHints.proxyHeaders?.request),
    sourceResponseHeaders = sanitizePlaybackResponseHeaders(stream.behaviorHints.proxyHeaders?.response),
    externalSubtitles = stream.externalSubtitles,
    streamType = stream.streamType,
    logo = logo,
    poster = poster,
    background = background,
    seasonNumber = seasonNumber,
    episodeNumber = episodeNumber,
    episodeTitle = episodeTitle,
    episodeThumbnail = episodeThumbnail,
    streamTitle = stream.streamLabel,
    streamSubtitle = stream.streamSubtitle,
    bingeGroup = stream.behaviorHints.bingeGroup,
    pauseDescription = pauseDescription,
    providerName = stream.addonName,
    providerAddonId = stream.addonId,
    contentType = type,
    videoId = videoId,
    parentMetaId = parentMetaId ?: videoId,
    parentMetaType = parentMetaType ?: type,
    initialPositionMs = resumePositionMs ?: 0L,
    initialProgressFraction = resumeProgressFraction,
    contentLanguage = contentLanguage(videoId),
)

internal fun StreamLaunch.p2pPlayerLaunch(
    videoId: String,
    stream: StreamItem,
    infoHash: String,
    resumePositionMs: Long?,
    resumeProgressFraction: Float?,
): PlayerLaunch = PlayerLaunch(
    profileId = profileId,
    title = title,
    sourceUrl = "torrent://$infoHash${stream.p2pFileIdx?.let { "?index=$it" }.orEmpty()}",
    sourceHeaders = emptyMap(),
    sourceResponseHeaders = emptyMap(),
    streamType = stream.streamType,
    logo = logo,
    poster = poster,
    background = background,
    seasonNumber = seasonNumber,
    episodeNumber = episodeNumber,
    episodeTitle = episodeTitle,
    episodeThumbnail = episodeThumbnail,
    streamTitle = stream.streamLabel,
    streamSubtitle = stream.streamSubtitle,
    bingeGroup = stream.behaviorHints.bingeGroup,
    pauseDescription = pauseDescription,
    providerName = stream.addonName,
    providerAddonId = stream.addonId,
    contentType = type,
    videoId = videoId,
    parentMetaId = parentMetaId ?: videoId,
    parentMetaType = parentMetaType ?: type,
    torrentInfoHash = infoHash,
    torrentFileIdx = stream.p2pFileIdx,
    torrentFilename = stream.behaviorHints.filename,
    torrentTrackers = stream.p2pTrackers,
    initialPositionMs = resumePositionMs ?: 0L,
    initialProgressFraction = resumeProgressFraction,
    contentLanguage = contentLanguage(videoId),
)

internal fun StreamLaunch.cachedPlayerLaunch(videoId: String, cached: CachedStreamLink): PlayerLaunch = PlayerLaunch(
    profileId = profileId,
    title = title,
    sourceUrl = cached.url,
    sourceHeaders = sanitizePlaybackHeaders(cached.requestHeaders),
    sourceResponseHeaders = sanitizePlaybackResponseHeaders(cached.responseHeaders),
    externalSubtitles = emptyList(),
    streamType = cached.streamType,
    logo = logo,
    poster = poster,
    background = background,
    seasonNumber = seasonNumber,
    episodeNumber = episodeNumber,
    episodeTitle = episodeTitle,
    episodeThumbnail = episodeThumbnail,
    streamTitle = cached.streamName,
    streamSubtitle = null,
    bingeGroup = cached.bingeGroup,
    pauseDescription = pauseDescription,
    providerName = cached.addonName,
    providerAddonId = cached.addonId,
    contentType = type,
    videoId = videoId,
    parentMetaId = parentMetaId ?: videoId,
    parentMetaType = parentMetaType ?: type,
    initialPositionMs = resumePositionMs ?: 0L,
    initialProgressFraction = resumeProgressFraction,
    contentLanguage = contentLanguage(videoId, cached.contentLanguage),
)

// A cached torrent becomes a stream again so it can go through the usual P2P path.
internal fun CachedStreamLink.toP2pStream(): StreamItem = StreamItem(
    name = streamName,
    url = null,
    infoHash = infoHash,
    fileIdx = fileIdx,
    sources = sources,
    addonName = addonName,
    addonId = addonId,
    behaviorHints = StreamBehaviorHints(
        filename = filename,
        videoSize = videoSize,
        bingeGroup = bingeGroup,
    ),
)

// Turns the stream auto play picked into a playable one, resolving debrid links first.
// Returns null when it could not, after moving auto play on to the next candidate.
internal suspend fun StreamLaunch.resolveAutoPlayStream(selectedStream: StreamItem, videoId: String): StreamItem? {
    if (!DirectDebridPlaybackResolver.shouldResolveToPlayableStream(selectedStream)) return selectedStream
    StreamsRepository.setOverlayVisible(true, getString(Res.string.debrid_resolving_stream))
    val resolved = DirectDebridPlaybackResolver.resolveToPlayableStream(
        stream = selectedStream,
        season = seasonNumber,
        episode = episodeNumber,
    )
    if (resolved is DirectDebridPlayableResult.Success) return resolved.stream
    val hasNextCandidate = StreamsRepository.skipAutoPlayStream(selectedStream)
    if (!hasNextCandidate) {
        resolved.toastMessage()?.let { NuvioToastController.show(it) }
    }
    if (!hasNextCandidate && resolved == DirectDebridPlayableResult.Stale) {
        reloadStreams(videoId)
    }
    return null
}
