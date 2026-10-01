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

enum class StreamSizeOrder {
    DEFAULT,
    LARGEST_FIRST,
    SMALLEST_FIRST,
}

private val StreamSizePattern = Regex(
    "(\\d+(?:[.,]\\d+)?)\\s*(TB|TiB|GB|GiB|MB|MiB|KB|KiB)\\b",
    RegexOption.IGNORE_CASE,
)

// The file's size in bytes: the add-on's hint, or else the size written in its name or description.
fun StreamItem.sizeBytes(): Long? {
    behaviorHints.videoSize?.takeIf { it > 0L }?.let { return it }
    val match = StreamSizePattern.find(searchableText()) ?: return null
    val value = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
    val unit = when (match.groupValues[2].uppercase().first()) {
        'T' -> 1024.0 * 1024.0 * 1024.0 * 1024.0
        'G' -> 1024.0 * 1024.0 * 1024.0
        'M' -> 1024.0 * 1024.0
        else -> 1024.0
    }
    return (value * unit).toLong().takeIf { it > 0L }
}

// Orders each group's streams by size, keeping the groups in place; streams without a size go last.
fun List<AddonStreamGroup>.sortedBySize(order: StreamSizeOrder): List<AddonStreamGroup> {
    if (order == StreamSizeOrder.DEFAULT) return this
    return map { group ->
        val sized = group.streams.map { it to it.sizeBytes() }
        val known = sized.filter { it.second != null }
        val ordered = when (order) {
            StreamSizeOrder.LARGEST_FIRST -> known.sortedByDescending { it.second }
            else -> known.sortedBy { it.second }
        }
        group.copy(streams = ordered.map { it.first } + sized.filter { it.second == null }.map { it.first })
    }
}
