package io.github.dimitrysaf.provenio.data

import app.cash.sqldelight.db.SqlDriver
import io.github.dimitrysaf.provenio.core.i18n.AppLanguage
import io.github.dimitrysaf.provenio.db.ProvenioDatabase

class LanguageStore(driver: SqlDriver) {

    private val queries = ProvenioDatabase(driver).languageSettingsQueries

    fun load(): AppLanguage? = queries.select().executeAsOneOrNull()?.let { row ->
        // An empty tag is the stored form of "follow the system", and a tag that no longer
        // maps to a shipped language must not fail the load.
        if (row.language.isEmpty()) {
            AppLanguage.System
        } else {
            AppLanguage.entries.firstOrNull { it.tag == row.language }
        }
    }

    fun save(language: AppLanguage) = queries.upsert(language = language.tag.orEmpty())
}
