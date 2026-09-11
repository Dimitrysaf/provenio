package io.github.dimitrysaf.provenio.data

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import io.github.dimitrysaf.provenio.db.ProvenioDatabase

private var applicationContext: Context? = null

/**
 * Hands the app an application context. Must run before anything touches the database or
 * the torrent cache — MainActivity calls it during onCreate.
 *
 * Named for the database because that was the first thing to need it; the torrent cache
 * needs exactly the same context, and a second holder for the same object would be one
 * more thing to forget to initialise.
 */
fun initDatabaseContext(context: Context) {
    applicationContext = context.applicationContext
}

/** The stored application context, or an error naming what was never initialised. */
internal fun requireApplicationContext(): Context = requireNotNull(applicationContext) {
    "initDatabaseContext() must be called before the database is used"
}

actual fun createDatabaseDriver(): SqlDriver =
    AndroidSqliteDriver(ProvenioDatabase.Schema, requireApplicationContext(), DatabaseFileName)
