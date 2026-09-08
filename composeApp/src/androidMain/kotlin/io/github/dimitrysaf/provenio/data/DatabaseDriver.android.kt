package io.github.dimitrysaf.provenio.data

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import io.github.dimitrysaf.provenio.db.ProvenioDatabase

private var applicationContext: Context? = null

/**
 * Hands the database an application context. Must run before anything touches the
 * database — MainActivity calls it during onCreate.
 */
fun initDatabaseContext(context: Context) {
    applicationContext = context.applicationContext
}

actual fun createDatabaseDriver(): SqlDriver {
    val context = requireNotNull(applicationContext) {
        "initDatabaseContext() must be called before the database is used"
    }
    return AndroidSqliteDriver(ProvenioDatabase.Schema, context, DatabaseFileName)
}
