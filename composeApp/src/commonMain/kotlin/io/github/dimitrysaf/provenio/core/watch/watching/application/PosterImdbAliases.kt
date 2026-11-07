package io.github.dimitrysaf.provenio.core.watch.watching.application

import androidx.compose.runtime.mutableStateMapOf
import io.github.dimitrysaf.provenio.core.home.MetaPreview
import io.github.dimitrysaf.provenio.core.metadata.tmdb.TmdbService
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * The IMDb id of posters that carry only a TMDB one, as the "tmdb:123" titles of TMDB-built
 * folders do. Watched titles are kept by IMDb id, so without it such a poster never shows as
 * watched. Filled by [resolve]; a poster not resolved yet reads as not watched until it is.
 */
object PosterImdbAliases {
    private val imdbIds = mutableStateMapOf<String, String>()
    private val requested = mutableSetOf<String>()

    fun imdbIdOf(item: MetaPreview): String? = imdbIds[item.id]

    /** Looks up the IMDb id of each TMDB-only poster not looked up yet. Call from the main thread. */
    suspend fun resolve(items: List<MetaPreview>) {
        val pending = items.filter { it.id.startsWith(TmdbPrefix) && it.id !in imdbIds && requested.add(it.id) }
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
                    if (imdbId != null) imdbIds[item.id] = imdbId else requested.remove(item.id)
                }
            }
        }
    }

    private const val TmdbPrefix = "tmdb:"
    private const val MaxConcurrentLookups = 4
}
