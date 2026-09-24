package io.github.dimitrysaf.provenio.core.downloads

internal expect object DownloadsStorage {
    fun loadPayload(): String?
    fun savePayload(payload: String)
}
