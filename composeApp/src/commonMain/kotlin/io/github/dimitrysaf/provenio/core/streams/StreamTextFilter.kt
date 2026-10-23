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

enum class StreamSortOrder {
    DEFAULT,
    LARGEST_FIRST,
    SMALLEST_FIRST,
    MOST_SEEDS,
    FEWEST_SEEDS,
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

private val StreamSeedsPattern = Regex(
    "(?:\uD83D\uDC64|\uD83D\uDC65|\uD83C\uDF31|\\bseed(?:er)?s?\\s*[:=])\\s*(\\d+)",
    RegexOption.IGNORE_CASE,
)

// How many seeds the torrent has, as the add-on wrote it: 👤, 👥 or 🌱 before a number, or a label like "Seeders: 12". The word needs its colon, so "The Bad Seed 2018" is not read as seeds.
fun StreamItem.seeds(): Int? =
    StreamSeedsPattern.find(searchableText())?.groupValues?.get(1)?.toIntOrNull()

// Orders each group's streams by size or seeds, keeping the groups in place; streams without that value go last.
fun List<AddonStreamGroup>.sortedByOrder(order: StreamSortOrder): List<AddonStreamGroup> {
    val value: (StreamItem) -> Long? = when (order) {
        StreamSortOrder.DEFAULT -> return this
        StreamSortOrder.LARGEST_FIRST, StreamSortOrder.SMALLEST_FIRST -> { stream -> stream.sizeBytes() }
        StreamSortOrder.MOST_SEEDS, StreamSortOrder.FEWEST_SEEDS -> { stream -> stream.seeds()?.toLong() }
    }
    val descending = order == StreamSortOrder.LARGEST_FIRST || order == StreamSortOrder.MOST_SEEDS
    return map { group ->
        val valued = group.streams.map { it to value(it) }
        val known = valued.filter { it.second != null }
        val ordered = if (descending) known.sortedByDescending { it.second } else known.sortedBy { it.second }
        group.copy(streams = ordered.map { it.first } + valued.filter { it.second == null }.map { it.first })
    }
}
