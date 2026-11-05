package io.github.dimitrysaf.provenio.shell.screens.streams

import io.github.dimitrysaf.provenio.core.addons.AddonRepository
import io.github.dimitrysaf.provenio.core.addons.buildAddonResourceUrl
import io.github.dimitrysaf.provenio.core.addons.enabledAddons
import io.github.dimitrysaf.provenio.core.addons.fetchAddonResponseText
import io.github.dimitrysaf.provenio.core.debrid.DirectDebridPlayableResult
import io.github.dimitrysaf.provenio.core.debrid.DirectDebridPlaybackResolver
import io.github.dimitrysaf.provenio.core.downloads.DownloadEnqueueResult
import io.github.dimitrysaf.provenio.core.downloads.DownloadsRepository
import io.github.dimitrysaf.provenio.core.metadata.MetaDetailsRepository
import io.github.dimitrysaf.provenio.core.metadata.MetaVideo
import io.github.dimitrysaf.provenio.core.streams.StreamItem
import io.github.dimitrysaf.provenio.core.streams.StreamParser
import io.github.dimitrysaf.provenio.core.streams.matchEpisodeStream
import io.github.dimitrysaf.provenio.core.streams.streamAddonInstanceId
import io.github.dimitrysaf.provenio.core.watch.progress.CurrentDateProvider
import io.github.dimitrysaf.provenio.core.watch.progress.buildPlaybackVideoId
import io.github.dimitrysaf.provenio.core.watch.watched.releasedEpisodesForSeason
import io.github.dimitrysaf.provenio.shell.components.ToastController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.streams_download_season_done
import provenio.composeapp.generated.resources.streams_download_season_started

/**
 * Downloads every aired episode of a season from one source picked in the Sources sheet.
 *
 * The picked source is the template: for each other episode the same add-on is asked for that
 * episode's sources, and the one that is the same release (the same torrent for a season pack,
 * else the same binge group or release name) is queued. One episode at a time, so a season does
 * not ask an add-on for a dozen episodes at once.
 */
internal object SeasonDownloads {
    // Outlives the sheet, which closes as soon as the action is picked.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun start(
        template: StreamItem,
        type: String,
        templateVideoId: String,
        parentMetaId: String,
        parentMetaType: String,
        title: String,
        logo: String?,
        poster: String?,
        background: String?,
        season: Int,
    ) {
        scope.launch {
            val meta = MetaDetailsRepository.peek(parentMetaType, parentMetaId)
                ?: MetaDetailsRepository.fetch(parentMetaType, parentMetaId)
            val episodes = meta?.releasedEpisodesForSeason(season, CurrentDateProvider.todayIsoDate()).orEmpty()
            if (episodes.isEmpty()) return@launch
            ToastController.show(getString(Res.string.streams_download_season_started, episodes.size))

            var queued = 0
            episodes.forEach { episode ->
                val videoId = episode.streamVideoId(parentMetaId)
                val stream = if (videoId == templateVideoId) {
                    template
                } else {
                    matchEpisodeStream(template, addonStreams(template, type, videoId))
                }
                if (stream != null && enqueue(stream, episode, videoId, type, parentMetaId, parentMetaType, title, logo, poster, background)) {
                    queued++
                }
            }
            ToastController.show(getString(Res.string.streams_download_season_done, queued, episodes.size))
        }
    }

    /** The episode's sources from the add-on that gave [template]; none when it cannot be asked. */
    private suspend fun addonStreams(template: StreamItem, type: String, videoId: String): List<StreamItem> {
        val addon = AddonRepository.uiState.value.addons.enabledAddons().firstOrNull { addon ->
            val manifest = addon.manifest ?: return@firstOrNull false
            addon.streamAddonInstanceId(manifest.id) == template.addonId
        } ?: return emptyList()
        val manifest = addon.manifest ?: return emptyList()
        return try {
            StreamParser.parse(
                payload = fetchAddonResponseText(
                    buildAddonResourceUrl(
                        manifestUrl = manifest.transportUrl,
                        resource = "stream",
                        type = type,
                        id = videoId,
                    ),
                ),
                addonName = template.addonName,
                addonId = template.addonId,
                addonLogo = template.addonLogo,
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // One episode the add-on cannot answer for is left out; the summary counts it.
            emptyList()
        }
    }

    private suspend fun enqueue(
        stream: StreamItem,
        episode: MetaVideo,
        videoId: String,
        type: String,
        parentMetaId: String,
        parentMetaType: String,
        title: String,
        logo: String?,
        poster: String?,
        background: String?,
    ): Boolean {
        val ready = if (DirectDebridPlaybackResolver.shouldResolveToPlayableStream(stream)) {
            val resolved = DirectDebridPlaybackResolver.resolveToPlayableStream(
                stream = stream,
                season = episode.season,
                episode = episode.episode,
            )
            (resolved as? DirectDebridPlayableResult.Success)?.stream ?: return false
        } else {
            stream
        }
        val result = DownloadsRepository.enqueueFromStream(
            contentType = type,
            videoId = videoId,
            parentMetaId = parentMetaId,
            parentMetaType = parentMetaType,
            title = title,
            logo = logo,
            poster = poster,
            background = background,
            seasonNumber = episode.season,
            episodeNumber = episode.episode,
            episodeTitle = episode.title,
            episodeThumbnail = episode.thumbnail,
            stream = ready,
        )
        return result == DownloadEnqueueResult.Started || result == DownloadEnqueueResult.Replaced
    }
}

// The id the Sources sheet asks add-ons with, as the Details page plays an episode.
private fun MetaVideo.streamVideoId(parentMetaId: String): String =
    id.takeIf(String::isNotBlank) ?: buildPlaybackVideoId(
        parentMetaId = parentMetaId,
        seasonNumber = season,
        episodeNumber = episode,
        fallbackVideoId = id,
    )
