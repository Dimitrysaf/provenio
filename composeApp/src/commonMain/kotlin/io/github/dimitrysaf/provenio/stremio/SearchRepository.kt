package io.github.dimitrysaf.provenio.stremio

import io.github.dimitrysaf.provenio.data.SearchHistoryStore
import io.github.dimitrysaf.provenio.data.createDatabaseDriver
import io.github.dimitrysaf.provenio.navigation.SearchFilter
import io.github.dimitrysaf.provenio.stremio.model.MetaPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Search history, and the last search itself.
 *
 * The results are held here rather than in the screen because opening a title disposes the
 * search screen's composition. Keeping them outside it means coming back shows what was
 * already found instead of an empty field and a second round of requests.
 */
object SearchRepository {

    private val _history = MutableStateFlow<List<String>>(emptyList())
    val history: StateFlow<List<String>> = _history.asStateFlow()

    var lastQuery: String = ""
        private set
    var lastFilter: SearchFilter = SearchFilter.All
        private set
    var lastResults: List<MetaPreview> = emptyList()
        private set

    private var store: SearchHistoryStore? = null
    private var loaded = false

    fun load() {
        if (loaded) return
        loaded = true
        store = runCatching { SearchHistoryStore(createDatabaseDriver()) }.getOrNull()
        refresh()
    }

    fun remember(query: String, filter: SearchFilter, results: List<MetaPreview>) {
        lastQuery = query
        lastFilter = filter
        lastResults = results
    }

    /** Only worth keeping a term that actually found something. */
    fun record(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        store?.let { runCatching { it.record(trimmed) } }
        refresh()
    }

    fun remove(query: String) {
        store?.let { runCatching { it.remove(query) } }
        refresh()
    }

    fun clear() {
        store?.let { runCatching { it.clear() } }
        refresh()
    }

    private fun refresh() {
        _history.value = store?.let { runCatching { it.recent() }.getOrNull() }.orEmpty()
    }
}
