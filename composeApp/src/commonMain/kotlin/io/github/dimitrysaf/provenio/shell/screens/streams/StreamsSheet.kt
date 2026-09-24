package io.github.dimitrysaf.provenio.shell.screens.streams

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import coil3.compose.AsyncImage
import io.github.dimitrysaf.provenio.shell.components.MediaActionsSheet
import io.github.dimitrysaf.provenio.shell.components.MediaSheetAction
import io.github.dimitrysaf.provenio.shell.components.ModalSheet
import io.github.dimitrysaf.provenio.shell.components.ToastController
import io.github.dimitrysaf.provenio.shell.components.dismissBottomSheet
import io.github.dimitrysaf.provenio.shell.components.safeBottomPadding
import io.github.dimitrysaf.provenio.core.downloads.DownloadsRepository
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.provenio.core.debrid.DebridSettingsRepository
import io.github.dimitrysaf.provenio.core.debrid.DirectDebridPlayableResult
import io.github.dimitrysaf.provenio.core.debrid.DirectDebridPlaybackResolver
import io.github.dimitrysaf.provenio.core.debrid.toastMessage
import io.github.dimitrysaf.provenio.core.playback.PlayerSettingsRepository
import io.github.dimitrysaf.provenio.core.watch.progress.WatchProgressRepository
import io.github.dimitrysaf.provenio.core.watch.progress.WatchProgressEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import provenio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import io.github.dimitrysaf.provenio.core.streams.AddonStreamGroup
import io.github.dimitrysaf.provenio.core.streams.StreamBadgeSettingsRepository
import io.github.dimitrysaf.provenio.core.streams.StreamItem
import io.github.dimitrysaf.provenio.core.streams.resolveStreamResumeState
import io.github.dimitrysaf.provenio.core.streams.StreamsRepository

// ---------------------------------------------------------------------------
// Streams sheet
// ---------------------------------------------------------------------------

/**
 * Where to play something from.
 *
 * A sheet rather than a screen: picking a source is a decision taken on the way to the player,
 * not a place to be, so it comes up over what you were looking at and goes away when it is done.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamsSheet(
    type: String,
    videoId: String,
    parentMetaId: String,
    parentMetaType: String,
    title: String,
    logo: String? = null,
    poster: String? = null,
    background: String? = null,
    seasonNumber: Int? = null,
    episodeNumber: Int? = null,
    episodeTitle: String? = null,
    episodeThumbnail: String? = null,
    resumePositionMs: Long? = null,
    resumeProgressFraction: Float? = null,
    manualSelection: Boolean = false,
    startFromBeginning: Boolean = false,
    showLoadingScreen: Boolean = false,
    /**
     * The episode's own id is still being looked up, so there is nothing to ask the add-ons for
     * yet. The sheet still opens: waiting belongs inside it, not on a page of its own.
     */
    preparing: Boolean = false,
    onStreamSelected: (stream: StreamItem, resumePositionMs: Long?, resumeProgressFraction: Float?) -> Unit = { _, _, _ -> },
    onStreamActionOpen: (
        stream: StreamItem,
        openExternally: Boolean,
        resumePositionMs: Long?,
        resumeProgressFraction: Float?,
    ) -> Unit = { _, _, _, _ -> },
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by StreamsRepository.uiState.collectAsStateWithLifecycle()
    val streamDisplaySettings by remember {
        StreamBadgeSettingsRepository.ensureLoaded()
        StreamBadgeSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val playerSettings by remember {
        PlayerSettingsRepository.ensureLoaded()
        PlayerSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val debridSettings by remember {
        DebridSettingsRepository.ensureLoaded()
        DebridSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val watchProgressUiState by remember {
        WatchProgressRepository.ensureLoaded()
        WatchProgressRepository.uiState
    }.collectAsStateWithLifecycle()
    remember {
        DownloadsRepository.ensureLoaded()
    }
    val clipboardManager = LocalClipboardManager.current
    val streamLinkCopiedText = stringResource(Res.string.streams_link_copied)
    val noDirectStreamLinkText = stringResource(Res.string.streams_no_direct_link)
    var streamActionsTarget by remember(videoId) { mutableStateOf<StreamItem?>(null) }
    val actionScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dismissScope = rememberCoroutineScope()
    val episodeProgress = watchProgressUiState.progressForVideo(
        videoId = videoId,
        parentMetaId = parentMetaId,
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber,
    )
    val resumeState = resolveStreamResumeState(
        progress = episodeProgress,
        initialPositionMs = resumePositionMs,
        initialProgressFraction = resumeProgressFraction,
        startFromBeginning = startFromBeginning,
    )
    val effectiveResumePositionMs = resumeState.positionMs
    val effectiveResumeProgressFraction = resumeState.progressFraction

    LaunchedEffect(type, videoId, seasonNumber, episodeNumber, manualSelection, preparing) {
        if (preparing) return@LaunchedEffect
        StreamsRepository.load(
            type = type,
            videoId = videoId,
            parentMetaId = parentMetaId,
            season = seasonNumber,
            episode = episodeNumber,
            manualSelection = manualSelection,
        )
    }

    if (showLoadingScreen) return

    val expansion = rememberStreamGroupExpansion(uiState.requestToken)
    val formatStreamSize = rememberStreamSizeLabelFormat()
    val torrentNotSupportedText = stringResource(Res.string.streams_torrent_not_supported)
    // An add-on earns a row as soon as it is asked, so a group with nothing in it is still
    // something to draw as long as it is still answering.
    val hasVisibleRows = uiState.groups.any { it.streams.isNotEmpty() || it.isLoading }
    val stillLoading = preparing || uiState.isAnyLoading
    val subtitle = if (seasonNumber != null && episodeNumber != null) {
        val code = stringResource(Res.string.streams_episode_badge, seasonNumber, episodeNumber)
        episodeTitle?.takeIf { it.isNotBlank() }?.let { "$code · $it" } ?: code
    } else {
        null
    }

    ModalSheet(
        onDismissRequest = {
            dismissScope.launch {
                dismissBottomSheet(sheetState = sheetState, onDismiss = onBack)
            }
        },
        sheetState = sheetState,
        modifier = modifier,
        fullHeight = true,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = StreamsHorizontalPadding, end = 8.dp)
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // What is about to be played, shown the way the actions sheet shows it: an episode
            // by its own still, anything else by its artwork.
            StreamsSheetThumbnail(
                imageUrl = episodeThumbnail?.takeIf { it.isNotBlank() }
                    ?: background?.takeIf { it.isNotBlank() }
                    ?: poster,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = { reloadStreams(type, videoId, parentMetaId, seasonNumber, episodeNumber, manualSelection) }, enabled = !uiState.isAnyLoading) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = stringResource(Res.string.streams_refresh),
                )
            }
        }

        HorizontalDivider()

        CompositionLocalProvider(LocalStreamSizeLabelFormat provides formatStreamSize) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                contentPadding = PaddingValues(
                    top = 8.dp,
                    bottom = safeBottomPadding(16.dp),
                ),
            ) {
                when {
                    // Nothing to show a row for yet: no add-on has even been asked, and while
                    // the id is still being looked up the rows still in state belong to whatever
                    // was open last.
                    preparing || (!hasVisibleRows && stillLoading) -> {
                        item(key = "streams_preparing") { StreamsPreparingBlock() }
                    }

                    !hasVisibleRows -> {
                        item(key = "streams_empty") {
                            StreamsEmptyBlock(reason = uiState.emptyStateReason)
                        }
                    }

                    else -> {
                        streamGroups(
                            groups = uiState.groups,
                            expansion = expansion,
                            debridEnabled = debridSettings.canResolvePlayableLinks,
                            appendInstantServiceToDefaultName = debridSettings.canResolvePlayableLinks &&
                                !debridSettings.hasCustomStreamFormatting,
                            showFileSizeBadges = streamDisplaySettings.showFileSizeBadges,
                            showAddonLogo = streamDisplaySettings.showAddonLogo,
                            badgePlacement = streamDisplaySettings.badgePlacement,
                            isStreamSelected = { stream -> ActiveStreamStore.isActive(videoId, stream) },
                            torrentNotSupportedText = torrentNotSupportedText,
                            onStreamSelected = { stream ->
                                onStreamSelected(stream, effectiveResumePositionMs, effectiveResumeProgressFraction)
                            },
                            onStreamLongPress = { stream -> streamActionsTarget = stream },
                            horizontalPadding = StreamsHorizontalPadding,
                        )
                    }
                }
            }
        }
    }

    streamActionsTarget?.let { stream ->
        MediaActionsSheet(
            imageUrl = if (seasonNumber != null) episodeThumbnail ?: background else background ?: poster,
            title = stream.streamLabel,
            subtitle = stream.streamSubtitle?.takeIf { it.isNotBlank() } ?: stream.addonName,
            landscapeThumbnail = true,
            onDismiss = { streamActionsTarget = null },
            actions = listOf(
                MediaSheetAction(
                    icon = Icons.Rounded.ContentCopy,
                    label = stringResource(Res.string.streams_copy_link),
                    onSelected = {
                        copyStreamLink(
                            stream = stream,
                            seasonNumber = seasonNumber,
                            episodeNumber = episodeNumber,
                            copiedText = streamLinkCopiedText,
                            noLinkText = noDirectStreamLinkText,
                            setClipboard = { url -> clipboardManager.setText(AnnotatedString(url)) },
                            scope = actionScope,
                        )
                    },
                ),
                MediaSheetAction(
                    icon = Icons.AutoMirrored.Rounded.OpenInNew,
                    label = stringResource(
                        if (playerSettings.externalPlayerEnabled) {
                            Res.string.streams_open_internal_player
                        } else {
                            Res.string.streams_open_external_player
                        },
                    ),
                    onSelected = {
                        onStreamActionOpen(
                            stream,
                            !playerSettings.externalPlayerEnabled,
                            effectiveResumePositionMs,
                            effectiveResumeProgressFraction,
                        )
                    },
                ),
                MediaSheetAction(
                    icon = Icons.Rounded.Download,
                    label = stringResource(Res.string.streams_download_file),
                    onSelected = {
                        downloadStream(
                            stream = stream,
                            type = type,
                            videoId = videoId,
                            parentMetaId = parentMetaId,
                            parentMetaType = parentMetaType,
                            title = title,
                            logo = logo,
                            poster = poster,
                            background = background,
                            seasonNumber = seasonNumber,
                            episodeNumber = episodeNumber,
                            episodeTitle = episodeTitle,
                            episodeThumbnail = episodeThumbnail,
                            scope = actionScope,
                        )
                    },
                ),
            ),
        )
    }
}

private fun reloadStreams(
    type: String,
    videoId: String,
    parentMetaId: String,
    seasonNumber: Int?,
    episodeNumber: Int?,
    manualSelection: Boolean,
) {
    StreamsRepository.reload(
        type = type,
        videoId = videoId,
        parentMetaId = parentMetaId,
        season = seasonNumber,
        episode = episodeNumber,
        manualSelection = manualSelection,
    )
}

internal fun streamSectionRenderKey(
    groupIndex: Int,
    group: AddonStreamGroup,
): String = "$groupIndex:${group.addonId}"

internal fun streamCardRenderKey(
    sectionKey: String,
    sourceIndex: Int,
    itemIndex: Int,
    stream: StreamItem,
): String = buildString {
    append(sectionKey)
    append(':')
    append(sourceIndex)
    append(':')
    append(itemIndex)
    append(':')
    append(stream.url ?: stream.infoHash ?: stream.clientResolve?.infoHash ?: stream.streamLabel)
    stream.externalUrl?.let {
        append(':')
        append(it)
    }
}

private fun copyStreamLink(
    stream: StreamItem,
    seasonNumber: Int?,
    episodeNumber: Int?,
    copiedText: String,
    noLinkText: String,
    setClipboard: (String) -> Unit,
    scope: CoroutineScope,
) {
    val directUrl = stream.playableDirectUrl ?: stream.externalOpenUrl
    if (!directUrl.isNullOrBlank()) {
        setClipboard(directUrl)
        ToastController.show(copiedText)
        return
    }
    if (!DirectDebridPlaybackResolver.shouldResolveToPlayableStream(stream)) {
        ToastController.show(noLinkText)
        return
    }
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
                if (!resolvedUrl.isNullOrBlank()) {
                    setClipboard(resolvedUrl)
                    ToastController.show(copiedText)
                } else {
                    ToastController.show(noLinkText)
                }
            }
            else -> resolved.toastMessage()?.let(ToastController::show)
        }
    }
}

private fun downloadStream(
    stream: StreamItem,
    type: String,
    videoId: String,
    parentMetaId: String,
    parentMetaType: String,
    title: String,
    logo: String?,
    poster: String?,
    background: String?,
    seasonNumber: Int?,
    episodeNumber: Int?,
    episodeTitle: String?,
    episodeThumbnail: String?,
    scope: CoroutineScope,
) {
    fun enqueue(resolvedStream: StreamItem) {
        val result = DownloadsRepository.enqueueFromStream(
            contentType = type,
            videoId = videoId,
            parentMetaId = parentMetaId,
            parentMetaType = parentMetaType,
            title = title,
            logo = logo,
            poster = poster,
            background = background,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            episodeTitle = episodeTitle,
            episodeThumbnail = episodeThumbnail,
            stream = resolvedStream,
        )
        ToastController.show(result.toastMessage())
    }

    if (!DirectDebridPlaybackResolver.shouldResolveToPlayableStream(stream)) {
        enqueue(stream)
        return
    }
    scope.launch {
        when (
            val resolved = DirectDebridPlaybackResolver.resolveToPlayableStream(
                stream = stream,
                season = seasonNumber,
                episode = episodeNumber,
            )
        ) {
            is DirectDebridPlayableResult.Success -> enqueue(resolved.stream)
            else -> resolved.toastMessage()?.let(ToastController::show)
        }
    }
}

/**
 * What is about to be played, at the head of the sheet.
 *
 * Landscape, because an episode's still is landscape and a title's artwork is too; the container
 * is drawn whether or not there is an image, so the header does not reflow once one arrives.
 */
@Composable
private fun StreamsSheetThumbnail(
    imageUrl: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(StreamsSheetThumbnailWidth)
            .height(StreamsSheetThumbnailHeight)
            .clip(ShapeDefaults.Small)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.size(
                    width = StreamsSheetThumbnailWidth,
                    height = StreamsSheetThumbnailHeight,
                ),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

/** 16:9, the same still the actions sheet shows for an episode. */
private val StreamsSheetThumbnailWidth = 96.dp

private val StreamsSheetThumbnailHeight = 54.dp

/** The sheet's gutter, matching the rest of the app's lists. */
internal val StreamsHorizontalPadding = 16.dp
