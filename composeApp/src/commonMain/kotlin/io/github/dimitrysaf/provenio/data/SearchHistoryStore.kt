package io.github.dimitrysaf.provenio.data

import app.cash.sqldelight.db.SqlDriver
import io.github.dimitrysaf.provenio.db.ProvenioDatabase

class SearchHistoryStore(driver: SqlDriver) {

    private val queries = ProvenioDatabase(driver).searchHistoryQueries

    fun recent(): List<String> = queries.recent().executeAsList()

    /** Delete then insert, so a repeated term moves back to the top of the list. */
    fun record(query: String) {
        queries.transaction {
            queries.remove(query)
            queries.insert(query)
        }
    }

    fun remove(query: String) = queries.remove(query)

    fun clear() = queries.clear()
}
