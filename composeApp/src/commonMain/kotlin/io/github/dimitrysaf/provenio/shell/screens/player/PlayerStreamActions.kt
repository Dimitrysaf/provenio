package io.github.dimitrysaf.provenio.shell.screens.player

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import io.github.dimitrysaf.provenio.core.debrid.DirectDebridPlayableResult
import io.github.dimitrysaf.provenio.core.debrid.DirectDebridPlaybackResolver
import io.github.dimitrysaf.provenio.core.debrid.toastMessage
import io.github.dimitrysaf.provenio.core.metadata.MetaVideo
import io.github.dimitrysaf.provenio.core.p2p.buildP2pMagnetUri
import io.github.dimitrysaf.provenio.core.playback.ExternalPlayerPlatform
import io.github.dimitrysaf.provenio.core.playback.ExternalPlayerPlaybackRequest
import io.github.dimitrysaf.provenio.core.playback.PlayerStreamsRepository
import io.github.dimitrysaf.provenio.core.streams.StreamItem
import io.github.dimitrysaf.provenio.shell.components.MediaActionsSheet
import io.github.dimitrysaf.provenio.shell.components.MediaSheetAction
import io.github.dimitrysaf.provenio.shell.components.ToastController
import io.github.dimitrysaf.provenio.shell.screens.streams.copyStreamLink
import io.github.dimitrysaf.provenio.shell.screens.streams.downloadStream
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.streams_copy_link
import provenio.composeapp.generated.resources.streams_download_file
import provenio.composeapp.generated.resources.streams_link_copied
import provenio.composeapp.generated.resources.streams_no_direct_link
import provenio.composeapp.generated.resources.streams_open_external_player

internal data class PlayerStreamActionsTarget(
    val stream: StreamItem,
    val episode: MetaVideo?,
)

@Composable
internal fun PlayerScreenRuntime.PlayerStreamActionsSheet(
    target: PlayerStreamActionsTarget,
    onDismiss: () -> Unit,
) {
    val stream = target.stream
    val episode = target.episode
    val clipboardManager = LocalClipboardManager.current
    val copiedText = stringResource(Res.string.streams_link_copied)
    val noLinkText = stringResource(Res.string.streams_no_direct_link)
    val seasonNumber = episode?.season ?: activeSeasonNumber
    val episodeNumber = episode?.episode ?: activeEpisodeNumber
    val externalPlayerId = playerSettingsUiState.externalPlayerId
    val externalPlayerConfigured = remember(externalPlayerId) {
        !externalPlayerId.isNullOrBlank() &&
            ExternalPlayerPlatform.availablePlayers().any { it.id == externalPlayerId }
    }
    val openExternal = args.onOpenInExternalPlayer?.takeIf { externalPlayerConfigured }

    MediaActionsSheet(
        imageUrl = episode?.thumbnail ?: activeEpisodeThumbnail ?: background ?: poster,
        title = stream.streamLabel,
        subtitle = stream.streamSubtitle?.takeIf { it.isNotBlank() } ?: stream.addonName,
        landscapeThumbnail = true,
        onDismiss = onDismiss,
        actions = listOfNotNull(
            MediaSheetAction(
                icon = Icons.Rounded.ContentCopy,
                label = stringResource(Res.string.streams_copy_link),
                onSelected = {
                    copyStreamLink(
                        stream = stream,
                        seasonNumber = seasonNumber,
                        episodeNumber = episodeNumber,
                        copiedText = copiedText,
                        noLinkText = noLinkText,
                        setClipboard = { url -> clipboardManager.setText(AnnotatedString(url)) },
                        scope = scope,
                    )
                },
            ),
            openExternal?.let { open ->
                MediaSheetAction(
                    icon = Icons.AutoMirrored.Rounded.OpenInNew,
                    label = stringResource(Res.string.streams_open_external_player),
                    onSelected = {
                        openStreamInExternalPlayer(
                            stream = stream,
                            episode = episode,
                            seasonNumber = seasonNumber,
                            episodeNumber = episodeNumber,
                            noLinkText = noLinkText,
                            open = open,
                        )
                    },
                )
            },
            MediaSheetAction(
                icon = Icons.Rounded.Download,
                label = stringResource(Res.string.streams_download_file),
                onSelected = {
                    downloadStream(
                        stream = stream,
                        type = contentType ?: parentMetaType,
                        videoId = episode?.id ?: activeVideoId ?: parentMetaId,
                        parentMetaId = parentMetaId,
                        parentMetaType = parentMetaType,
                        title = title,
                        logo = logo,
                        poster = poster,
                        background = background,
                        seasonNumber = seasonNumber,
                        episodeNumber = episodeNumber,
                        episodeTitle = episode?.title ?: activeEpisodeTitle,
                        episodeThumbnail = episode?.thumbnail ?: activeEpisodeThumbnail,
                        scope = scope,
                    )
                },
            ),
        ),
    )
}

private fun PlayerScreenRuntime.openStreamInExternalPlayer(
    stream: StreamItem,
    episode: MetaVideo?,
    seasonNumber: Int?,
    episodeNumber: Int?,
    noLinkText: String,
    open: (ExternalPlayerPlaybackRequest) -> Unit,
) {
    fun launch(sourceUrl: String, resolvedStream: StreamItem) {
        PlayerStreamsRepository.pauseSearchForPlayback()
        open(
            ExternalPlayerPlaybackRequest(
                sourceUrl = sourceUrl,
                title = title,
                streamTitle = resolvedStream.streamLabel,
                sourceHeaders = sanitizePlaybackHeaders(resolvedStream.behaviorHints.proxyHeaders?.request),
                resumePositionMs = if (episode == null) playbackSnapshot.positionMs else 0L,
                season = seasonNumber,
                episode = episodeNumber,
                episodeTitle = episode?.title ?: activeEpisodeTitle,
            ),
        )
    }

    val directUrl = stream.playableDirectUrl ?: stream.externalOpenUrl
    if (!directUrl.isNullOrBlank()) {
        launch(directUrl, stream)
        return
    }
    if (DirectDebridPlaybackResolver.shouldResolveToPlayableStream(stream)) {
        scope.launch {
            when (
                val resolved = DirectDebridPlaybackResolver.resolveToPlayableStream(
                    stream = stream,
                    season = seasonNumber,
                    episode = episodeNumber,
                )
            ) {
                is DirectDebridPlayableResult.Success -> {
                    val resolvedUrl = resolved.stream.playableDirectUrl
                    if (resolvedUrl.isNullOrBlank()) {
                        ToastController.show(noLinkText)
                    } else {
                        launch(resolvedUrl, resolved.stream)
                    }
                }
                else -> resolved.toastMessage()?.let(ToastController::show)
            }
        }
        return
    }
    val infoHash = stream.p2pInfoHash
    if (infoHash != null) {
        launch(buildP2pMagnetUri(infoHash, stream.p2pTrackers), stream)
        return
    }
    ToastController.show(noLinkText)
}
