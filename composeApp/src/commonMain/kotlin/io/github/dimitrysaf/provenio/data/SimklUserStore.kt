package io.github.dimitrysaf.provenio.data

import app.cash.sqldelight.db.SqlDriver
import io.github.dimitrysaf.provenio.db.ProvenioDatabase
import io.github.dimitrysaf.provenio.db.SimklUser

class SimklUserStore(driver: SqlDriver) {

    private val queries = ProvenioDatabase(driver).simklUserQueries

    fun load(): SimklUser? = queries.select().executeAsOneOrNull()

    fun save(name: String, avatarUrl: String?) = queries.upsert(name, avatarUrl)

    fun clear() = queries.clear()
}
