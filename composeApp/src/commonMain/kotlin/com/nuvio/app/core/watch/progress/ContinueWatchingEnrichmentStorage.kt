package com.nuvio.app.core.watch.progress

internal expect object ContinueWatchingEnrichmentStorage {
    fun loadPayload(key: String): String?
    fun savePayload(key: String, payload: String)
    fun removePayload(key: String)
}
