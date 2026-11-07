package io.github.dimitrysaf.provenio.core.watch.watching.application

import io.github.dimitrysaf.provenio.core.home.MetaPreview
import io.github.dimitrysaf.provenio.core.metadata.tmdb.TmdbService
import io.github.dimitrysaf.provenio.core.watch.watched.watchedItemKeys
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * The IMDb id of posters that carry only a TMDB one, as the "tmdb:123" titles of TMDB-built
 * folders do. Watched titles are kept by IMDb id, so without it such a poster never shows as
 * watched. Filled by [resolve]; [withAliases] then lets the TMDB id match too.
 */
object PosterImdbAliases {
    data class Alias(val type: String, val imdbId: String)

    private val _aliases = MutableStateFlow<Map<String, Alias>>(emptyMap())

    /** By the poster's TMDB id. */
    val aliases: StateFlow<Map<String, Alias>> = _aliases.asStateFlow()

    private val requested = mutableSetOf<String>()

    /** [keys], plus the keys of each TMDB-only poster whose IMDb id is among them. */
    fun withAliases(keys: Set<String>, aliases: Map<String, Alias> = _aliases.value): Set<String> {
        if (aliases.isEmpty() || keys.isEmpty()) return keys
        val extra = aliases.flatMap { (tmdbId, alias) ->
            if (watchedItemKeys(alias.type, alias.imdbId).any(keys::contains)) {
                watchedItemKeys(alias.type, tmdbId)
            } else {
                emptySet()
            }
        }
        return if (extra.isEmpty()) keys else keys + extra
    }

    /** Looks up the IMDb id of each TMDB-only poster not looked up yet. Call from the main thread. */
    suspend fun resolve(items: List<MetaPreview>) {
        val known = _aliases.value
        val pending = items.filter { it.id.startsWith(TmdbPrefix) && it.id !in known && requested.add(it.id) }
        if (pending.isEmpty()) return
        val gate = Semaphore(MaxConcurrentLookups)
        coroutineScope {
            pending.forEach { item ->
                launch {
                    val tmdbId = item.id.removePrefix(TmdbPrefix).toIntOrNull()
                    val imdbId = tmdbId?.let { id ->
                        gate.withPermit { runCatching { TmdbService.tmdbToImdb(id, item.type) }.getOrNull() }
                    }
                    // A miss is asked again next time, in case it was only the network.
                    if (imdbId != null) {
                        _aliases.update { it + (item.id to Alias(item.type, imdbId)) }
                    } else {
                        requested.remove(item.id)
                    }
                }
            }
        }
    }

    private const val TmdbPrefix = "tmdb:"
    private const val MaxConcurrentLookups = 4
}
