package com.nuvio.app.core.tracking.trakt

internal expect object TraktAuthStorage {
    fun loadPayload(profileId: Int): String?
    fun savePayload(profileId: Int, payload: String)
    fun removeProfile(profileId: Int)
}
