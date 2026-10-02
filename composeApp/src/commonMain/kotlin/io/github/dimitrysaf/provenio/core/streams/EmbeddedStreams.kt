package io.github.dimitrysaf.provenio.core.streams

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal const val EmbeddedSourceAddonId = "embedded"

private val KnownEmbeddedSources = listOf(
    "youtube.com" to "YouTube",
    "youtu.be" to "YouTube",
    "youtube-nocookie.com" to "YouTube",
    "vimeo.com" to "Vimeo",
    "dailymotion.com" to "Dailymotion",
    "dai.ly" to "Dailymotion",
    "archive.org" to "Internet Archive",
    "twitch.tv" to "Twitch",
)

internal val AddonStreamGroup.isEmbeddedSource: Boolean
    get() = addonId == EmbeddedSourceAddonId || addonId.startsWith("$EmbeddedSourceAddonId:")

internal fun embeddedSourceName(url: String?): String? {
    val trimmed = url?.trim().orEmpty()
    val scheme = trimmed.substringBefore("://", missingDelimiterValue = "").lowercase()
    if (scheme != "http" && scheme != "https") return null
    val host = trimmed
        .substringAfter("://")
        .substringBefore('/')
        .substringBefore('?')
        .substringBefore('#')
        .substringAfterLast('@')
        .substringBefore(':')
        .lowercase()
        .removePrefix("www.")
        .removePrefix("m.")
        .takeIf { it.isNotBlank() }
        ?: return null
    return KnownEmbeddedSources
        .firstOrNull { (domain, _) -> host == domain || host.endsWith(".$domain") }
        ?.second
        ?: host
}

internal fun embeddedStreamGroups(streams: List<StreamItem>): List<AddonStreamGroup> =
    streams
        .groupBy { it.addonName }
        .map { (sourceName, sourceStreams) ->
            val groupId = "$EmbeddedSourceAddonId:${sourceName.lowercase()}"
            AddonStreamGroup(
                addonName = sourceName,
                addonId = groupId,
                streams = sourceStreams.map { it.copy(addonId = groupId) },
                isLoading = false,
                addonLogo = sourceStreams.firstNotNullOfOrNull { it.addonLogo },
            )
        }

internal const val ExternalLookupGroupId = "$EmbeddedSourceAddonId::external"

internal fun embeddedLookupGroup(name: String, addonId: String = EmbeddedSourceAddonId): AddonStreamGroup =
    AddonStreamGroup(
        addonName = name,
        addonId = addonId,
        streams = emptyList(),
        isLoading = true,
    )

internal val AddonStreamGroup.isPendingEmbeddedLookup: Boolean
    get() = addonId == EmbeddedSourceAddonId && isLoading

internal fun List<AddonStreamGroup>.withEmbeddedGroups(embedded: List<AddonStreamGroup>): List<AddonStreamGroup> =
    embedded + filterNot { it.isEmbeddedSource }

internal fun List<AddonStreamGroup>.withoutEmbeddedSources(): List<AddonStreamGroup> =
    filterNot { it.isEmbeddedSource }

internal fun List<AddonStreamGroup>.embeddedPlayableStreams(): List<StreamItem> =
    filter { it.isEmbeddedSource }
        .flatMap { it.streams }
        .filter { it.playableDirectUrl != null && !it.discovered }

internal fun List<AddonStreamGroup>.embeddedAutoPlayStreams(season: Int?): List<StreamItem> {
    val embedded = embeddedPlayableStreams()
    if (embedded.isEmpty()) return emptyList()
    if (season == 0) return embedded
    val others = withoutEmbeddedSources()
    val othersSettled = others.none { it.isLoading }
    return if (othersSettled && others.none { it.streams.isNotEmpty() }) embedded else emptyList()
}

internal class EmbeddedSourceState(
    private var metadataStreams: List<StreamItem>,
    private var metadataPending: Boolean,
    private var specialPending: Boolean,
    private var externalPending: Boolean,
    private val pendingLabel: String,
    private val externalPendingLabel: String,
    private val present: (List<AddonStreamGroup>) -> List<AddonStreamGroup>,
) {
    private var specialStreams: List<StreamItem> = emptyList()
    private var externalStreams: List<StreamItem> = emptyList()
    private val mutex = Mutex()

    fun initialGroups(): List<AddonStreamGroup> = groups()

    suspend fun metadataLoaded(streams: List<StreamItem>): List<AddonStreamGroup> = mutex.withLock {
        metadataStreams = streams
        metadataPending = false
        groups()
    }

    suspend fun specialsLoaded(streams: List<StreamItem>): List<AddonStreamGroup> = mutex.withLock {
        specialStreams = streams
        specialPending = false
        groups()
    }

    suspend fun externalLoaded(streams: List<StreamItem>): List<AddonStreamGroup> = mutex.withLock {
        externalStreams = streams
        externalPending = false
        groups()
    }

    private fun groups(): List<AddonStreamGroup> {
        val found = present(
            embeddedStreamGroups((metadataStreams + specialStreams + externalStreams).distinctBy { it.embeddedSourceKey() }),
        )
        return when {
            metadataPending || specialPending -> found + embeddedLookupGroup(pendingLabel)
            externalPending -> found + embeddedLookupGroup(externalPendingLabel, ExternalLookupGroupId)
            else -> found
        }
    }
}

private fun StreamItem.embeddedSourceKey(): String =
    if (url == null && infoHash == null && externalUrl != null) "$addonName:$name:$externalUrl" else sourceKey()
