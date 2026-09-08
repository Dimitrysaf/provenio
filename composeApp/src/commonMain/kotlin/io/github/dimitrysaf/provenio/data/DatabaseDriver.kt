package io.github.dimitrysaf.provenio.data

import app.cash.sqldelight.db.SqlDriver

internal const val DatabaseFileName = "provenio.db"

/** Opens the app's database for the current platform. */
expect fun createDatabaseDriver(): SqlDriver
