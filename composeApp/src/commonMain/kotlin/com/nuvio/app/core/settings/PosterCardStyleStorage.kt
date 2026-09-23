package com.nuvio.app.core.settings

internal expect object PosterCardStyleStorage {
    fun loadPayload(): String?
    fun savePayload(payload: String)
}
