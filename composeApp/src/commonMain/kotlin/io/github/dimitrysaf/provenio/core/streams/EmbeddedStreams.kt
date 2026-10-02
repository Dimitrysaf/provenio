package io.github.dimitrysaf.provenio.core.streams

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

internal fun embeddedLookupGroup(name: String): AddonStreamGroup =
    AddonStreamGroup(
        addonName = name,
        addonId = EmbeddedSourceAddonId,
        streams = emptyList(),
        isLoading = true,
    )

internal fun List<AddonStreamGroup>.withEmbeddedGroups(embedded: List<AddonStreamGroup>): List<AddonStreamGroup> =
    embedded + filterNot { it.isEmbeddedSource }

internal fun List<AddonStreamGroup>.withoutEmbeddedSources(): List<AddonStreamGroup> =
    filterNot { it.isEmbeddedSource }

internal fun List<AddonStreamGroup>.embeddedPlayableStreams(): List<StreamItem> =
    filter { it.isEmbeddedSource }
        .flatMap { it.streams }
        .filter { it.playableDirectUrl != null }

internal fun List<AddonStreamGroup>.embeddedAutoPlayStreams(season: Int?): List<StreamItem> {
    val embedded = embeddedPlayableStreams()
    if (embedded.isEmpty()) return emptyList()
    if (season == 0) return embedded
    val others = withoutEmbeddedSources()
    val othersSettled = others.none { it.isLoading }
    return if (othersSettled && others.none { it.streams.isNotEmpty() }) embedded else emptyList()
}
