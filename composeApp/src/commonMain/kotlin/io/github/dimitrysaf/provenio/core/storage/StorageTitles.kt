package io.github.dimitrysaf.provenio.core.storage

import kotlinx.serialization.json.Json

/**
 * The titles torrents were last played under, by info hash, so the Storage page can name what the
 * torrent cache holds instead of showing release names. Only the most recent ones are kept.
 */
internal object StorageTitles {
    private var entries: LinkedHashMap<String, String>? = null

    fun titles(): Map<String, String> = load()

    fun record(infoHash: String, title: String) {
        val key = infoHash.trim().lowercase()
        if (key.isEmpty() || title.isBlank()) return
        val current = load()
        if (current[key] == title) return
        current.remove(key)
        current[key] = title
        while (current.size > MaxEntries) current.remove(current.keys.first())
        StoragePlatform.saveTorrentTitles(Json.encodeToString(current.toMap()))
    }

    private fun load(): LinkedHashMap<String, String> {
        entries?.let { return it }
        val stored = StoragePlatform.loadTorrentTitles()
            ?.let { runCatching { Json.decodeFromString<Map<String, String>>(it) }.getOrNull() }
            .orEmpty()
        return LinkedHashMap(stored).also { entries = it }
    }

    private const val MaxEntries = 300
}
