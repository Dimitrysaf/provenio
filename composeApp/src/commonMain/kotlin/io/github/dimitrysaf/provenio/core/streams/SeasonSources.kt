package io.github.dimitrysaf.provenio.core.streams

/** How the Sources sheet lists sources when it is opened for a whole season. */
enum class SeasonSourceMode {
    /** Only torrents that hold the whole season, such as `Show.S01.1080p` or `SHOW_S01.mp4`. */
    Packs,
    /** Every source; downloading the season then finds a matching source for each episode in turn. */
    EpisodeByEpisode,
}

/**
 * True when [stream] names a release of the whole of [season] rather than one episode of it.
 *
 * Add-ons put the release name in the stream's name, title, description or file name, one per
 * line, and a pack often lists the episode file it would play under the pack's own name, so one
 * line that names the season alone is enough.
 */
fun StreamItem.isSeasonPack(season: Int): Boolean =
    listOfNotNull(name, title, description, behaviorHints.filename)
        .flatMap { it.lines() }
        .any { line -> lineNamesWholeSeason(line, season) }

internal fun lineNamesWholeSeason(line: String, season: Int): Boolean {
    if (CompleteSeries.containsMatchIn(line)) return true
    SeasonRange.findAll(line).forEach { match ->
        val from = match.groupValues[1].toInt()
        val to = match.groupValues[2].toInt()
        if (season in from..to) return true
    }
    return SeasonToken.findAll(line).any { it.groupValues[1].toInt() == season } ||
        SeasonWord.findAll(line).any { it.groupValues[1].toInt() == season }
}

/**
 * The source among [candidates], all from the add-on that gave [template], that is the same
 * release as [template] for another episode, or null when none is close enough.
 *
 * The same torrent wins (a season pack, its file for this episode picked by the add-on), then
 * the add-on's own binge group, then the release name with its episode numbers taken out.
 */
fun matchEpisodeStream(template: StreamItem, candidates: List<StreamItem>): StreamItem? {
    val torrent = template.torrentHash()
    if (torrent != null) candidates.firstOrNull { it.torrentHash() == torrent }?.let { return it }
    val bingeGroup = template.behaviorHints.bingeGroup?.takeIf(String::isNotBlank)
    if (bingeGroup != null) candidates.firstOrNull { it.behaviorHints.bingeGroup == bingeGroup }?.let { return it }
    val wanted = template.releaseTokens()
    if (wanted.isEmpty()) return null
    return candidates
        .map { it to similarity(wanted, it.releaseTokens()) }
        .filter { (_, score) -> score >= MinReleaseSimilarity }
        .maxByOrNull { (_, score) -> score }
        ?.first
}

private fun StreamItem.torrentHash(): String? =
    (p2pInfoHash ?: infoHash ?: clientResolve?.infoHash)?.trim()?.lowercase()?.takeIf(String::isNotEmpty)

/** The words of the release name, with episode numbers, sizes and seeders taken out. */
internal fun StreamItem.releaseTokens(): Set<String> =
    listOfNotNull(name, title, description, behaviorHints.filename)
        .flatMap { it.lines() }
        .joinToString(" ")
        .lowercase()
        .replace(EpisodeCode, " ")
        .replace(EpisodeWord, " ")
        .replace(Size, " ")
        .split(NonWord)
        // Lone numbers are seeders, sizes and the like, which differ between episodes of one release.
        .filter { it.length > 1 && !it.all(Char::isDigit) }
        .toSet()

// Shared words over the longer name's words: each episode's own title adds words to both sides.
private fun similarity(a: Set<String>, b: Set<String>): Double {
    if (a.isEmpty() || b.isEmpty()) return 0.0
    return (a intersect b).size.toDouble() / maxOf(a.size, b.size)
}

private const val MinReleaseSimilarity = 0.6

private const val NotAlnumBefore = "(?<![A-Za-z0-9])"

// S01, S1, s01 that is not followed by an episode number (S01E02, S01 E02, S01.E02, S01x02).
private val SeasonToken = Regex("""$NotAlnumBefore[Ss](\d{1,2})(?!\d)(?![\s._-]*(?:[Ee]\d|[Xx]\d))""")

// "Season 1", "Season.01", "Saison 1", not followed by an episode.
private val SeasonWord = Regex(
    """$NotAlnumBefore(?:season|saison|temporada|staffel|stagione|σεζόν)[\s._-]*(\d{1,2})(?!\d)(?![\s._-]*(?:[Ee]\d|episode|ep\b))""",
    RegexOption.IGNORE_CASE,
)

// S01-S05, S01-05, Season 1-5.
private val SeasonRange = Regex("""$NotAlnumBefore(?:[Ss]|season[\s._-]*)(\d{1,2})[\s._]*[-–~][\s._]*[Ss]?(\d{1,2})(?!\d)""", RegexOption.IGNORE_CASE)

private val CompleteSeries = Regex("""$NotAlnumBefore(?:complete[\s._-]*series|all[\s._-]*seasons)""", RegexOption.IGNORE_CASE)

private val EpisodeCode = Regex("""s\d{1,2}[\s._-]*e\d{1,3}|\b\d{1,2}x\d{1,3}\b""")
private val EpisodeWord = Regex("""(?:episode|ep)[\s._-]*\d{1,3}""")
private val Size = Regex("""\d+(?:[.,]\d+)?\s*(?:gb|mb|kb)""")
private val NonWord = Regex("""[^\p{L}\p{N}]+""")
