package io.github.dimitrysaf.provenio.core.playback

import co.touchlab.kermit.Logger
import io.github.dimitrysaf.provenio.core.addons.AddonRepository
import io.github.dimitrysaf.provenio.core.addons.buildAddonResourceUrl
import io.github.dimitrysaf.provenio.core.addons.enabledAddons
import io.github.dimitrysaf.provenio.core.addons.fetchAddonResponseText
import io.github.dimitrysaf.provenio.core.debrid.DebridSettingsRepository
import io.github.dimitrysaf.provenio.core.debrid.DebridStreamPresentation
import io.github.dimitrysaf.provenio.core.debrid.DirectDebridStreamPreparer
import io.github.dimitrysaf.provenio.core.debrid.LocalDebridAvailabilityService
import io.github.dimitrysaf.provenio.core.metadata.MetaDetailsRepository
import io.github.dimitrysaf.provenio.core.streams.AddonStreamGroup
import io.github.dimitrysaf.provenio.core.streams.InstalledStreamAddonTarget
import io.github.dimitrysaf.provenio.core.streams.StreamAutoPlaySelector
import io.github.dimitrysaf.provenio.core.streams.StreamBadgePresentation
import io.github.dimitrysaf.provenio.core.streams.StreamBadgeSettingsRepository
import io.github.dimitrysaf.provenio.core.streams.StreamItem
import io.github.dimitrysaf.provenio.core.streams.StreamLoadCompletion
import io.github.dimitrysaf.provenio.core.streams.StreamParser
import io.github.dimitrysaf.provenio.core.streams.StreamsRepository
import io.github.dimitrysaf.provenio.core.streams.StreamsUiState
import io.github.dimitrysaf.provenio.core.streams.StreamsEmptyStateReason
import io.github.dimitrysaf.provenio.core.streams.withEmbeddedGroups
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import io.github.dimitrysaf.provenio.core.streams.withSpecialStreams
import io.github.dimitrysaf.provenio.core.streams.fetchSpecialStreams
import io.github.dimitrysaf.provenio.core.streams.SpecialSourceFinder
import io.github.dimitrysaf.provenio.core.streams.ExternalSourceFinder
import io.github.dimitrysaf.provenio.core.streams.externalSourceRequest
import io.github.dimitrysaf.provenio.core.streams.EmbeddedSourceState
import io.github.dimitrysaf.provenio.core.streams.runCatchingUnlessCancelled
import io.github.dimitrysaf.provenio.core.streams.streamAddonInstanceId
import io.github.dimitrysaf.provenio.core.streams.toEmptyStateReason
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import provenio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.getString

/**
 * Dedicated stream fetcher for use inside the player (sources & episodes panels).
 * Uses its own state so it doesn't interfere with the main [StreamsRepository].
 */
object PlayerStreamsRepository {
    private val log = Logger.withTag("PlayerStreamsRepo")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // source panel
    private val _sourceState = MutableStateFlow(StreamsUiState())
    val sourceState: StateFlow<StreamsUiState> = _sourceState.asStateFlow()
    private var sourceJob: Job? = null
    private var sourceRequestKey: String? = null

    // episode streams panel
    private val _episodeStreamsState = MutableStateFlow(StreamsUiState())
    val episodeStreamsState: StateFlow<StreamsUiState> = _episodeStreamsState.asStateFlow()
    private var episodeStreamsJob: Job? = null
    private var episodeStreamsRequestKey: String? = null

    fun loadSources(
        type: String,
        videoId: String,
        season: Int? = null,
        episode: Int? = null,
        forceRefresh: Boolean = false,
        parentMetaId: String? = null,
    ) {
        fetchStreams(
            type = type,
            videoId = videoId,
            parentMetaId = parentMetaId,
            season = season,
            episode = episode,
            forceRefresh = forceRefresh,
            stateFlow = _sourceState,
            requestKeyHolder = { sourceRequestKey },
            setRequestKey = { sourceRequestKey = it },
            jobHolder = { sourceJob },
            setJob = { sourceJob = it },
        )
    }

    fun loadEpisodeStreams(
        type: String,
        videoId: String,
        season: Int? = null,
        episode: Int? = null,
        forceRefresh: Boolean = false,
        parentMetaId: String? = null,
    ) {
        fetchStreams(
            type = type,
            videoId = videoId,
            parentMetaId = parentMetaId,
            season = season,
            episode = episode,
            forceRefresh = forceRefresh,
            stateFlow = _episodeStreamsState,
            requestKeyHolder = { episodeStreamsRequestKey },
            setRequestKey = { episodeStreamsRequestKey = it },
            jobHolder = { episodeStreamsJob },
            setJob = { episodeStreamsJob = it },
        )
    }

    fun stopSourcesLoading() {
        cancelSourceJob()
    }

    fun pauseSearchForPlayback() {
        cancelSourceJob()
        cancelEpisodeStreamsJob()
    }

    private fun cancelSourceJob() {
        val job = sourceJob ?: return
        job.cancel()
        sourceJob = null
        sourceRequestKey = null
        _sourceState.update { current ->
            current.copy(
                isAnyLoading = false,
                groups = current.groups.map { group ->
                    if (group.isLoading) group.copy(isLoading = false) else group
                },
            )
        }
    }

    private fun cancelEpisodeStreamsJob() {
        val job = episodeStreamsJob ?: return
        job.cancel()
        episodeStreamsJob = null
        episodeStreamsRequestKey = null
        _episodeStreamsState.update { current ->
            current.copy(
                isAnyLoading = false,
                groups = current.groups.map { group ->
                    if (group.isLoading) group.copy(isLoading = false) else group
                },
            )
        }
    }

    fun selectSourceFilter(addonId: String?) {
        _sourceState.update { it.copy(selectedFilter = addonId) }
    }

    fun selectEpisodeStreamsFilter(addonId: String?) {
        _episodeStreamsState.update { it.copy(selectedFilter = addonId) }
    }

    fun clearEpisodeStreams() {
        episodeStreamsJob?.cancel()
        episodeStreamsRequestKey = null
        _episodeStreamsState.value = StreamsUiState()
    }

    fun clearAll() {
        sourceJob?.cancel()
        sourceRequestKey = null
        _sourceState.value = StreamsUiState()
        clearEpisodeStreams()
    }

    private fun fetchStreams(
        type: String,
        videoId: String,
        parentMetaId: String?,
        season: Int?,
        episode: Int?,
        forceRefresh: Boolean,
        stateFlow: MutableStateFlow<StreamsUiState>,
        requestKeyHolder: () -> String?,
        setRequestKey: (String?) -> Unit,
        jobHolder: () -> Job?,
        setJob: (Job) -> Unit,
    ) {
        val requestKey = "$type::$videoId::$season::$episode"
        val current = stateFlow.value
        if (
            !forceRefresh &&
            requestKeyHolder() == requestKey &&
            (current.groups.isNotEmpty() || current.emptyStateReason != null || current.isAnyLoading)
        ) {
            return
        }

        setRequestKey(requestKey)
        jobHolder()?.cancel()
        val cachedGroups = if (forceRefresh) null else StreamsRepository.cachedGroups(requestKey)
        if (cachedGroups != null) {
            log.d { "Using cached streams for type=$type id=$videoId" }
            stateFlow.value = StreamsUiState(
                groups = cachedGroups,
                activeAddonIds = cachedGroups.map { it.addonId }.toSet(),
                isAnyLoading = false,
                emptyStateReason = cachedGroups.toEmptyStateReason(anyLoading = false),
            )
            return
        }
        stateFlow.value = StreamsUiState()

        val streamBadgeRules = StreamBadgeSettingsRepository.snapshot()
        val metadataEmbeddedStreams = MetaDetailsRepository.findEmbeddedStreams(
            videoId = videoId,
            type = type,
            parentMetaId = parentMetaId,
        )
        val embeddedLookupMetaId = parentMetaId?.takeIf {
            metadataEmbeddedStreams.isEmpty() && !MetaDetailsRepository.isMetaKnown(type, it)
        }
        val specialLookup = if (season == 0 && parentMetaId != null && episode != null) parentMetaId to episode else null
        val externalRequest = externalSourceRequest(
            type = type,
            videoId = videoId,
            parentMetaId = parentMetaId,
            season = season,
            episode = episode,
        )
        val hasEmbeddedSource = metadataEmbeddedStreams.isNotEmpty() ||
            embeddedLookupMetaId != null ||
            specialLookup != null ||
            externalRequest != null

        val installedAddons = AddonRepository.uiState.value.addons.enabledAddons()
        PlayerSettingsRepository.ensureLoaded()
        val playerSettings = PlayerSettingsRepository.uiState.value
        val debridSettings = DebridSettingsRepository.snapshot()

        if (installedAddons.isEmpty() && !hasEmbeddedSource) {
            stateFlow.value = StreamsUiState(
                isAnyLoading = false,
                emptyStateReason = io.github.dimitrysaf.provenio.core.streams.StreamsEmptyStateReason.NoAddonsInstalled,
            )
            return
        }

        val streamAddons = installedAddons
            .mapNotNull { addon ->
                val manifest = addon.manifest ?: return@mapNotNull null
                val supportsRequestedStream = manifest.resources.any { resource ->
                    resource.name == "stream" &&
                        resource.types.contains(type) &&
                        (resource.idPrefixes.isEmpty() ||
                            resource.idPrefixes.any { videoId.startsWith(it) })
                }
                if (!supportsRequestedStream) return@mapNotNull null

                InstalledStreamAddonTarget(
                    addonName = addon.displayTitle.ifBlank { manifest.name },
                    addonId = addon.streamAddonInstanceId(manifest.id),
                    manifest = manifest,
                )
            }

        if (streamAddons.isEmpty() && !hasEmbeddedSource) {
            stateFlow.value = StreamsUiState(
                isAnyLoading = false,
                emptyStateReason = io.github.dimitrysaf.provenio.core.streams.StreamsEmptyStateReason.NoCompatibleAddons,
            )
            return
        }

        val noSourceEmptyReason = if (installedAddons.isEmpty()) {
            StreamsEmptyStateReason.NoAddonsInstalled
        } else {
            StreamsEmptyStateReason.NoCompatibleAddons
        }
        val hasOtherSources = streamAddons.isNotEmpty()
        val embeddedSources = EmbeddedSourceState(
            metadataStreams = metadataEmbeddedStreams,
            metadataPending = embeddedLookupMetaId != null,
            specialPending = specialLookup != null,
            externalPending = externalRequest != null,
            pendingLabel = if (embeddedLookupMetaId != null || specialLookup != null) {
                runBlocking { getString(Res.string.source_embedded) }
            } else {
                ""
            },
            externalPendingLabel = if (externalRequest != null) {
                runBlocking { getString(Res.string.source_where_to_watch) }
            } else {
                ""
            },
            present = { groups -> StreamBadgePresentation.apply(groups = groups, rules = streamBadgeRules) },
        )
        val initialEmbeddedGroups = embeddedSources.initialGroups()
        val installedAddonOrder = streamAddons.map { it.addonName }
        val initialGroups = StreamAutoPlaySelector.orderAddonStreams(initialEmbeddedGroups + streamAddons.map { addon ->
            AddonStreamGroup(
                addonName = addon.addonName,
                addonId = addon.addonId,
                streams = emptyList(),
                isLoading = true,
                addonLogo = addon.manifest.logoUrl,
            )
        }, installedAddonOrder)
        val isInitiallyLoading = initialGroups.any { it.isLoading }
        stateFlow.value = StreamsUiState(
            groups = initialGroups,
            activeAddonIds = initialGroups.map { it.addonId }.toSet(),
            isAnyLoading = isInitiallyLoading,
        )

        val job = scope.launch {
            val installedAddonIds = streamAddons.map { it.addonId }.toSet()
            val installedAddonNames = installedAddonOrder.toSet()
            val totalTasks = streamAddons.size
            val completions = Channel<StreamLoadCompletion>(capacity = Channel.BUFFERED)
            val debridAvailabilityJobs = mutableListOf<Job>()

            fun publishCompletion(completion: StreamLoadCompletion) {
                if (completions.trySend(completion).isFailure) {
                    log.d { "Ignoring late player stream load completion after channel close" }
                }
            }

            fun presentStreamGroup(group: AddonStreamGroup): AddonStreamGroup {
                val badgeGroup = StreamBadgePresentation.apply(
                    groups = listOf(group),
                    rules = streamBadgeRules,
                ).firstOrNull() ?: group
                return DebridStreamPresentation.apply(
                    groups = listOf(badgeGroup),
                    settings = debridSettings,
                ).firstOrNull() ?: badgeGroup
            }

            fun publishStreamGroup(group: AddonStreamGroup) {
                stateFlow.update { current ->
                    val updated = StreamAutoPlaySelector.orderAddonStreams(
                        groups = current.groups.map { currentGroup ->
                            if (currentGroup.addonId == group.addonId) group else currentGroup
                        },
                        installedOrder = installedAddonOrder,
                    )
                    val anyLoading = updated.any { it.isLoading }
                    current.copy(
                        groups = updated,
                        isAnyLoading = anyLoading,
                        emptyStateReason = updated.toEmptyStateReason(anyLoading),
                    )
                }
            }

            fun publishStreamGroupAfterCacheCheck(group: AddonStreamGroup) {
                if (group.addonId !in installedAddonIds || group.streams.isEmpty()) {
                    publishStreamGroup(presentStreamGroup(group))
                    return
                }

                val eligibleGroupIds = setOf(group.addonId)
                val shouldWaitForCacheCheck = LocalDebridAvailabilityService.hasPendingCacheCheck(
                    groups = listOf(group),
                    eligibleGroupIds = eligibleGroupIds,
                )
                if (!shouldWaitForCacheCheck) {
                    publishStreamGroup(presentStreamGroup(group))
                    return
                }

                val checkingGroup = LocalDebridAvailabilityService.markChecking(
                    groups = listOf(group),
                    eligibleGroupIds = eligibleGroupIds,
                ).firstOrNull() ?: group

                val availabilityJob = launch {
                    val availabilityGroup = LocalDebridAvailabilityService.annotateCachedAvailability(
                        groups = listOf(checkingGroup),
                        eligibleGroupIds = eligibleGroupIds,
                    ).firstOrNull() ?: checkingGroup
                    publishStreamGroup(presentStreamGroup(availabilityGroup))
                }
                debridAvailabilityJobs += availabilityJob
            }

            fun publishEmbeddedGroups(groups: List<AddonStreamGroup>) {
                stateFlow.update { current ->
                    val updated = current.groups.withEmbeddedGroups(groups)
                    val anyLoading = updated.any { it.isLoading }
                    current.copy(
                        groups = updated,
                        activeAddonIds = updated.map { it.addonId }.toSet(),
                        isAnyLoading = anyLoading,
                        emptyStateReason = if (!hasOtherSources && !anyLoading && updated.none { it.streams.isNotEmpty() }) {
                            noSourceEmptyReason
                        } else {
                            updated.toEmptyStateReason(anyLoading)
                        },
                    )
                }
            }

            val embeddedLookupJob = embeddedLookupMetaId?.let { metaId ->
                launch {
                    val streams = runCatchingUnlessCancelled {
                        MetaDetailsRepository.fetchEmbeddedStreams(type = type, videoId = videoId, parentMetaId = metaId)
                    }.getOrElse { error ->
                        log.w(error) { "Failed to look up embedded streams for $metaId" }
                        emptyList()
                    }
                    publishEmbeddedGroups(embeddedSources.metadataLoaded(streams))
                }
            }

            val specialContext = specialLookup?.let { (metaId, specialEpisode) ->
                async {
                    runCatchingUnlessCancelled {
                        SpecialSourceFinder.context(type = type, parentMetaId = metaId, episode = specialEpisode)
                    }.getOrElse { error ->
                        log.w(error) { "Failed to look up special $metaId:0:$specialEpisode" }
                        null
                    }
                }
            }
            val specialImdbId = specialContext?.let { context ->
                async {
                    context.await()?.let { found ->
                        runCatchingUnlessCancelled { SpecialSourceFinder.imdbId(found) }.getOrNull()
                    }
                }
            }
            val specialSourcesJob = specialContext?.let { context ->
                launch {
                    val streams = context.await()?.let { found ->
                        runCatchingUnlessCancelled { SpecialSourceFinder.streams(found) }.getOrNull()
                    }.orEmpty()
                    publishEmbeddedGroups(embeddedSources.specialsLoaded(streams))
                }
            }
            val externalSourcesJob = externalRequest?.let { request ->
                launch {
                    val special = specialContext?.await()
                    val streams = runCatchingUnlessCancelled {
                        ExternalSourceFinder.streams(request = request, special = special)
                    }.getOrElse { error ->
                        log.w(error) { "Failed to look up external sources for ${request.metaId}" }
                        emptyList()
                    }
                    publishEmbeddedGroups(embeddedSources.externalLoaded(streams))
                }
            }

            streamAddons.forEach { addon ->
                launch {
                    val url = buildAddonResourceUrl(
                        manifestUrl = addon.manifest.transportUrl,
                        resource = "stream",
                        type = type,
                        id = videoId,
                    )

                    val displayName = addon.addonName
                    val group = runCatchingUnlessCancelled {
                        val payload = fetchAddonResponseText(
                            url = url,
                            forceRefresh = forceRefresh,
                        )
                        StreamParser.parse(
                            payload = payload,
                            addonName = displayName,
                            addonId = addon.addonId,
                            addonLogo = addon.manifest.logoUrl,
                        )
                    }.fold(
                        onSuccess = { streams ->
                            AddonStreamGroup(
                                addonName = displayName,
                                addonId = addon.addonId,
                                streams = streams,
                                isLoading = false,
                                addonLogo = addon.manifest.logoUrl,
                            )
                        },
                        onFailure = { err ->
                            log.w(err) { "Failed: ${displayName}" }
                            AddonStreamGroup(
                                addonName = displayName,
                                addonId = addon.addonId,
                                streams = emptyList(),
                                isLoading = false,
                                error = err.message,
                                addonLogo = addon.manifest.logoUrl,
                            )
                        },
                    )
                    val completedGroup = group.withSpecialStreams(
                        addon.fetchSpecialStreams(specialImdbId = specialImdbId?.await(), forceRefresh = forceRefresh),
                    )
                    publishCompletion(StreamLoadCompletion.Addon(completedGroup))
                }
            }

            repeat(totalTasks) {
                when (val completion = completions.receive()) {
                    is StreamLoadCompletion.Addon -> {
                        publishStreamGroupAfterCacheCheck(completion.group)
                    }
                }
            }

            for (availabilityJob in debridAvailabilityJobs) {
                availabilityJob.join()
            }
            embeddedLookupJob?.join()
            specialSourcesJob?.join()
            externalSourcesJob?.join()
            StreamsRepository.cacheGroups(requestKey, stateFlow.value.groups)
            launch {
                DirectDebridStreamPreparer.prepare(
                    streams = stateFlow.value.groups
                        .filter { it.addonId in installedAddonIds }
                        .flatMap { it.streams },
                    season = season,
                    episode = episode,
                    playerSettings = playerSettings,
                    installedAddonNames = installedAddonNames,
                ) { original, prepared ->
                    stateFlow.update { current ->
                        current.copy(
                            groups = DirectDebridStreamPreparer.replacePreparedStream(
                                groups = current.groups,
                                original = original,
                                prepared = prepared,
                                eligibleGroupIds = installedAddonIds,
                            ),
                        )
                    }
                }
            }
            completions.close()
        }
        setJob(job)
    }
}
private data class PlayerInstalledStreamAddonTarget(
    val addonName: String,
    val addonId: String,
    val manifest: io.github.dimitrysaf.provenio.core.addons.AddonManifest,
)

private fun StreamsUiState.streamDiagnostics(): String {
    val streamCount = groups.sumOf { it.streams.size }
    val loadingCount = groups.count { it.isLoading }
    val errorCount = groups.count { !it.error.isNullOrBlank() }
    val sampleGroups = groups.take(4).joinToString(prefix = "[", postfix = "]") { group ->
        buildString {
            append(group.addonName)
            append(':')
            append(group.streams.size)
            if (group.isLoading) append(":loading")
            if (!group.error.isNullOrBlank()) append(":error")
        }
    }
    val suffix = if (groups.size > 4) "+${groups.size - 4}" else ""
    return "groups=${groups.size} streams=$streamCount isAnyLoading=$isAnyLoading " +
        "loadingGroups=$loadingCount errorGroups=$errorCount empty=${emptyStateReason ?: "none"} " +
        "sample=$sampleGroups$suffix"
}

private fun io.github.dimitrysaf.provenio.core.addons.ManagedAddon.streamAddonInstanceId(manifestId: String): String =
    "addon:$manifestId:$manifestUrl"

