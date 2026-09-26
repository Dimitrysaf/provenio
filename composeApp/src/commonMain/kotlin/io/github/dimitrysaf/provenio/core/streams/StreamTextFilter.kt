package io.github.dimitrysaf.provenio.core.streams

// Resolution buckets read straight from the text an add-on sent, highest first.
enum class StreamQuality(internal val pattern: Regex?) {
    UHD(Regex("2160p|\\b4k\\b|\\buhd\\b", RegexOption.IGNORE_CASE)),
    FHD(Regex("1080[pi]", RegexOption.IGNORE_CASE)),
    HD(Regex("720p", RegexOption.IGNORE_CASE)),
    SD(Regex("576p|480p|360p|\\bsd\\b", RegexOption.IGNORE_CASE)),
    UNKNOWN(null),
}

fun StreamItem.quality(): StreamQuality {
    val text = searchableText()
    return StreamQuality.entries.firstOrNull { it.pattern?.containsMatchIn(text) == true } ?: StreamQuality.UNKNOWN
}

// The query as a case-insensitive regular expression, or as plain text while it is not a valid one.
fun streamQueryRegex(query: String): Regex? {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return null
    return runCatching { Regex(trimmed, RegexOption.IGNORE_CASE) }
        .getOrElse { Regex(Regex.escape(trimmed), RegexOption.IGNORE_CASE) }
}

// Keeps the streams matching the query and any of the qualities, where none means all; groups still loading stay so their progress shows.
fun List<AddonStreamGroup>.filteredBy(query: Regex?, qualities: Set<StreamQuality>): List<AddonStreamGroup> {
    if (query == null && qualities.isEmpty()) return this
    return mapNotNull { group ->
        val streams = group.streams.filter { stream ->
            (query == null || query.containsMatchIn(stream.searchableText())) &&
                (qualities.isEmpty() || stream.quality() in qualities)
        }
        group.copy(streams = streams).takeIf { streams.isNotEmpty() || group.isLoading }
    }
}
