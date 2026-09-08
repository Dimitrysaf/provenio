package io.github.dimitrysaf.provenio.data

import app.cash.sqldelight.db.SqlDriver
import io.github.dimitrysaf.provenio.db.ProvenioDatabase
import io.github.dimitrysaf.provenio.player.PlayerBackend

class PlayerStore(driver: SqlDriver) {

    private val queries = ProvenioDatabase(driver).playerSettingsQueries

    fun load(): PlayerBackend? = queries.select().executeAsOneOrNull()?.let { row ->
        // A backend that no longer exists must not fail the load.
        PlayerBackend.entries.firstOrNull { it.name == row.backend }
    }

    fun save(backend: PlayerBackend) = queries.upsert(backend = backend.name)
}
