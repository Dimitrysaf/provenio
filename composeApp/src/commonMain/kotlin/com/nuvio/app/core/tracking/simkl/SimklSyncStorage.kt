package com.nuvio.app.core.tracking.simkl

internal expect object SimklSyncStorage {
    fun loadPayload(): String?
    fun savePayload(payload: String)
    fun removeProfile(profileId: Int)
}
