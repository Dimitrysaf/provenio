package io.github.dimitrysaf.provenio.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.github.dimitrysaf.provenio.db.ProvenioDatabase
import java.io.File

/**
 * The JDBC driver does not create the schema for us the way the Android one does, so a
 * database that did not exist a moment ago has to be built before it is handed back.
 */
actual fun createDatabaseDriver(): SqlDriver {
    val directory = File(System.getProperty("user.home"), ".local/share/provenio")
    directory.mkdirs()
    val file = File(directory, DatabaseFileName)
    val isNew = !file.exists()
    val driver = JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}")
    if (isNew) {
        ProvenioDatabase.Schema.create(driver)
    } else {
        // The Android driver tracks the schema version itself; this one does not. Every
        // migration statement is IF NOT EXISTS, so replaying them all on each open is
        // idempotent and avoids hand rolling version bookkeeping here.
        ProvenioDatabase.Schema.migrate(
            driver = driver,
            oldVersion = 1L,
            newVersion = ProvenioDatabase.Schema.version,
        )
    }
    return driver
}
