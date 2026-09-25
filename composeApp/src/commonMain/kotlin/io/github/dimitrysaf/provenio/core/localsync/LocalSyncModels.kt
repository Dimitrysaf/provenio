package io.github.dimitrysaf.provenio.core.localsync

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// One synced value. [clock] is the counter of the device that last changed it, so the newest change wins; a null [value] is a tombstone that carries a deletion to the other device.
@Serializable
internal data class SyncRecord(
    val value: JsonElement? = null,
    val clock: Long,
    val deviceId: String,
)

/** Everything this device syncs for one profile, with the counter its own changes are stamped with. */
@Serializable
internal data class SyncLedger(
    val lamport: Long = 0L,
    val records: Map<String, SyncRecord> = emptyMap(),
)

/** A device this one has paired with, and where it was last reached. */
@Serializable
data class LocalSyncPeer(
    val deviceId: String,
    val name: String,
    val secret: String,
    val host: String,
    val port: Int,
    val lastSyncedAtEpochMs: Long? = null,
)

/** How a record both devices changed is settled. */
internal enum class SyncConflictPolicy {
    NEWEST,
    KEEP_LOCAL,
    TAKE_REMOTE,
}

internal data class LocalSyncMerge(
    val ledger: SyncLedger,
    val changes: Map<String, JsonElement?>,
)
