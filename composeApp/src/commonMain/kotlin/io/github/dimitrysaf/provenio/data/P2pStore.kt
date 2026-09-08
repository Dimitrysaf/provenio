package io.github.dimitrysaf.provenio.data

import app.cash.sqldelight.db.SqlDriver
import io.github.dimitrysaf.provenio.db.ProvenioDatabase
import io.github.dimitrysaf.provenio.p2p.CacheSize
import io.github.dimitrysaf.provenio.p2p.P2pSettings
import io.github.dimitrysaf.provenio.p2p.TorrentProfile

/** Persists the single row of peer-to-peer configuration. */
class P2pStore(driver: SqlDriver) {

    private val queries = ProvenioDatabase(driver).p2pSettingsQueries

    fun load(): P2pSettings? = queries.select().executeAsOneOrNull()?.let { row ->
        P2pSettings(
            enabled = row.enabled != 0L,
            consentAccepted = row.consentAccepted != 0L,
            uploadEnabled = row.uploadEnabled != 0L,
            // An enum renamed or removed in a later version must not fail the load.
            profile = TorrentProfile.entries.firstOrNull { it.name == row.profile }
                ?: TorrentProfile.Balanced,
            cacheSize = CacheSize.entries.firstOrNull { it.name == row.cacheSize }
                ?: CacheSize.Gb2,
            listenPort = row.listenPort.toInt(),
            hideStats = row.hideStats != 0L,
        )
    }

    fun save(settings: P2pSettings) {
        queries.upsert(
            enabled = if (settings.enabled) 1L else 0L,
            consentAccepted = if (settings.consentAccepted) 1L else 0L,
            uploadEnabled = if (settings.uploadEnabled) 1L else 0L,
            profile = settings.profile.name,
            cacheSize = settings.cacheSize.name,
            listenPort = settings.listenPort.toLong(),
            hideStats = if (settings.hideStats) 1L else 0L,
        )
    }
}
