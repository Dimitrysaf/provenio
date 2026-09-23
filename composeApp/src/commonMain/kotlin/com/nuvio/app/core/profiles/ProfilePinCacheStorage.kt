package com.nuvio.app.core.profiles

internal expect object ProfilePinCacheStorage {
    fun loadPayload(profileIndex: Int): String?
    fun savePayload(profileIndex: Int, payload: String)
    fun removePayload(profileIndex: Int)
}