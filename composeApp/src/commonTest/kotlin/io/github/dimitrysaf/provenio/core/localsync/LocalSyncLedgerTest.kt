package io.github.dimitrysaf.provenio.core.localsync

import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalSyncLedgerTest {
    private val prefixes = listOf("library/")

    @Test
    fun localEditsAreStampedAndRemovalsBecomeTombstones() {
        val first = refreshSyncLedger(
            ledger = SyncLedger(),
            current = mapOf("library/a" to JsonPrimitive("A"), "library/b" to JsonPrimitive("B")),
            ownedPrefixes = prefixes,
            deviceId = "phone",
        )
        assertEquals(2L, first.lamport)

        val second = refreshSyncLedger(
            ledger = first,
            current = mapOf("library/a" to JsonPrimitive("A2")),
            ownedPrefixes = prefixes,
            deviceId = "phone",
        )
        assertEquals(JsonPrimitive("A2"), second.records.getValue("library/a").value)
        assertNull(second.records.getValue("library/b").value)
        assertEquals(4L, second.lamport)
    }

    @Test
    fun unchangedValuesKeepTheirStamps() {
        val ledger = refreshSyncLedger(SyncLedger(), mapOf("library/a" to JsonPrimitive("A")), prefixes, "phone")
        assertEquals(ledger, refreshSyncLedger(ledger, mapOf("library/a" to JsonPrimitive("A")), prefixes, "phone"))
    }

    @Test
    fun newestChangeWinsAndBothSidesAgree() {
        val phone = SyncLedger(
            lamport = 5,
            records = mapOf(
                "library/a" to SyncRecord(JsonPrimitive("phone"), clock = 5, deviceId = "phone"),
                "library/only-phone" to SyncRecord(JsonPrimitive("p"), clock = 1, deviceId = "phone"),
            ),
        )
        val laptop = SyncLedger(
            lamport = 7,
            records = mapOf(
                "library/a" to SyncRecord(JsonPrimitive("laptop"), clock = 7, deviceId = "laptop"),
                "library/only-laptop" to SyncRecord(null, clock = 2, deviceId = "laptop"),
            ),
        )
        val onPhone = mergeSyncLedgers(phone, laptop, SyncConflictPolicy.NEWEST)
        val onLaptop = mergeSyncLedgers(laptop, phone, SyncConflictPolicy.NEWEST)

        assertEquals(onPhone.ledger.records, onLaptop.ledger.records)
        assertEquals(JsonPrimitive("laptop"), onPhone.changes["library/a"])
        assertTrue("library/only-laptop" !in onPhone.changes)
        assertEquals(JsonPrimitive("p"), onLaptop.changes["library/only-phone"])
        assertEquals(7L, onPhone.ledger.lamport)
    }

    @Test
    fun hostWinsConflictsOnTheFirstSync() {
        val host = SyncLedger(1, mapOf("settings/x" to SyncRecord(JsonPrimitive("host"), 1, "a")))
        val joiner = SyncLedger(9, mapOf("settings/x" to SyncRecord(JsonPrimitive("joiner"), 9, "b")))

        val onHost = mergeSyncLedgers(host, joiner, SyncConflictPolicy.KEEP_LOCAL)
        val onJoiner = mergeSyncLedgers(joiner, onHost.ledger, SyncConflictPolicy.TAKE_REMOTE)

        assertEquals(JsonPrimitive("host"), onHost.ledger.records.getValue("settings/x").value)
        assertEquals(JsonPrimitive("host"), onJoiner.changes["settings/x"])
        assertEquals(onHost.ledger.records, onJoiner.ledger.records)
    }
}
