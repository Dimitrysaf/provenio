package io.github.dimitrysaf.provenio.data

import app.cash.sqldelight.db.SqlDriver
import io.github.dimitrysaf.provenio.db.ProvenioDatabase

class SimklStore(driver: SqlDriver) {

    private val queries = ProvenioDatabase(driver).simklAuthQueries

    fun token(): String? = queries.select().executeAsOneOrNull()?.accessToken

    fun save(accessToken: String) = queries.upsert(accessToken)

    fun clear() = queries.clear()
}
