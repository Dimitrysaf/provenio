package io.github.dimitrysaf.provenio.core.localsync

import kotlinx.serialization.json.JsonElement

internal fun SyncRecord.isNewerThan(other: SyncRecord): Boolean =
    clock > other.clock || (clock == other.clock && deviceId > other.deviceId)

private fun SyncRecord.isSameChangeAs(other: SyncRecord): Boolean =
    clock == other.clock && deviceId == other.deviceId

// Stamps whatever changed locally since the last sync: a new or edited value gets the next local counter, and a value that disappeared under one of [ownedPrefixes] becomes a tombstone.
internal fun refreshSyncLedger(
    ledger: SyncLedger,
    current: Map<String, JsonElement>,
    ownedPrefixes: Collection<String>,
    deviceId: String,
): SyncLedger {
    var lamport = ledger.lamport
    val records = ledger.records.toMutableMap()
    current.forEach { (key, value) ->
        if (records[key]?.value != value) {
            lamport += 1
            records[key] = SyncRecord(value = value, clock = lamport, deviceId = deviceId)
        }
    }
    val removedKeys = records.filter { (key, record) ->
        record.value != null && key !in current && ownedPrefixes.any { key.startsWith(it) }
    }.keys
    removedKeys.forEach { key ->
        lamport += 1
        records[key] = SyncRecord(value = null, clock = lamport, deviceId = deviceId)
    }
    return SyncLedger(lamport = lamport, records = records)
}

// Merges the other device's records into [local] by key, both ways: each side ends with the same winners, and [LocalSyncMerge.changes] lists the values this device has to apply.
internal fun mergeSyncLedgers(
    local: SyncLedger,
    remote: SyncLedger,
    policy: SyncConflictPolicy,
): LocalSyncMerge {
    val records = local.records.toMutableMap()
    val changes = linkedMapOf<String, JsonElement?>()
    remote.records.forEach { (key, theirs) ->
        val ours = records[key]
        val takeTheirs = when {
            ours == null -> true
            ours.isSameChangeAs(theirs) -> false
            policy == SyncConflictPolicy.KEEP_LOCAL -> false
            policy == SyncConflictPolicy.TAKE_REMOTE -> true
            else -> theirs.isNewerThan(ours)
        }
        if (takeTheirs) {
            records[key] = theirs
            if (ours?.value != theirs.value) changes[key] = theirs.value
        }
    }
    val remoteClock = remote.records.values.maxOfOrNull { it.clock } ?: 0L
    val lamport = maxOf(local.lamport, remote.lamport, remoteClock)
    return LocalSyncMerge(ledger = SyncLedger(lamport = lamport, records = records), changes = changes)
}

// Keeps the winning stamps for values that were just applied but records them as this device now exports them, so a harmless difference in form is not mistaken for a new local change.
internal fun adoptAppliedValues(
    ledger: SyncLedger,
    current: Map<String, JsonElement>,
    appliedKeys: Collection<String>,
): SyncLedger {
    if (appliedKeys.isEmpty()) return ledger
    val records = ledger.records.toMutableMap()
    appliedKeys.forEach { key ->
        val record = records[key] ?: return@forEach
        val exported = current[key]
        if (record.value != null && exported != null) {
            records[key] = record.copy(value = exported)
        }
    }
    return ledger.copy(records = records)
}
