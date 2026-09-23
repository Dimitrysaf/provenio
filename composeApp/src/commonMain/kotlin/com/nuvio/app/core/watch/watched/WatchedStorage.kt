package com.nuvio.app.core.watch.watched

expect object WatchedStorage {
    fun loadPayload(profileId: Int): String?
    fun savePayload(profileId: Int, payload: String)
}

