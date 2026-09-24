package io.github.dimitrysaf.provenio.shell.screens.streams

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.provenio.core.build.AppFeaturePolicy
import io.github.dimitrysaf.provenio.core.addons.AddonManifest
import io.github.dimitrysaf.provenio.core.addons.AddonRepository
import io.github.dimitrysaf.provenio.core.addons.ManagedAddon
import io.github.dimitrysaf.provenio.core.metadata.MetaDetailsRepository
import io.github.dimitrysaf.provenio.core.downloads.DownloadsRepository
import io.github.dimitrysaf.provenio.core.plugins.PluginRepository
import io.github.dimitrysaf.provenio.core.plugins.PluginsUiState
import io.github.dimitrysaf.provenio.core.streams.hasCompatiblePlaybackSource

internal class PlaybackAvailability(
    private val addons: List<ManagedAddon>,
    private val plugins: PluginsUiState,
) {
    fun canStream(type: String, videoId: String): Boolean =
        hasCompatiblePlaybackSource(addons, plugins, type, videoId) ||
            MetaDetailsRepository.findEmbeddedStreams(videoId).isNotEmpty()

    fun canPlay(
        type: String,
        videoId: String,
        parentMetaId: String,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null,
    ): Boolean = canStream(type, videoId) || DownloadsRepository.findPlayableDownload(
        parentMetaId = parentMetaId,
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber,
        videoId = videoId,
    ) != null

    companion object {
        fun current(): PlaybackAvailability = PlaybackAvailability(
            addons = AddonRepository.uiState.value.addons,
            plugins = if (AppFeaturePolicy.pluginsEnabled) {
                PluginRepository.uiState.value
            } else {
                PluginsUiState(pluginsEnabled = false)
            },
        )
    }
}

@Composable
internal fun rememberPlaybackAvailability(): PlaybackAvailability {
    val addons by remember {
        AddonRepository.initialize()
        AddonRepository.uiState
    }.collectAsStateWithLifecycle()
    val plugins = if (AppFeaturePolicy.pluginsEnabled) {
        val state by remember {
            PluginRepository.initialize()
            PluginRepository.uiState
        }.collectAsStateWithLifecycle()
        state
    } else {
        PluginsUiState(pluginsEnabled = false)
    }
    val downloads by remember {
        DownloadsRepository.ensureLoaded()
        DownloadsRepository.uiState
    }.collectAsStateWithLifecycle()
    return remember(addons, plugins, downloads) {
        PlaybackAvailability(addons.addons, plugins)
    }
}
