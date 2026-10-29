package io.github.dimitrysaf.provenio.core.party

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class PartyInviteTest {

    private val secret = ByteArray(PARTY_SECRET_SIZE) { it.toByte() }

    @Test
    fun `a built link parses back to the same room`() {
        val link = buildPartyLink("042917", 47_640, listOf("192.168.1.5", "2001:db8::5"), secret)

        val target = assertIs<PartyJoinTarget.Link>(parsePartyJoinTarget(link))

        assertEquals("042917", target.code)
        assertEquals(47_640, target.port)
        assertEquals(listOf("192.168.1.5", "2001:db8::5"), target.addresses)
        assertContentEquals(secret, target.secret)
    }

    @Test
    fun `a code is read with or without spaces`() {
        assertEquals(PartyJoinTarget.Code("482913"), parsePartyJoinTarget("482913"))
        assertEquals(PartyJoinTarget.Code("482913"), parsePartyJoinTarget(" 482 913 "))
        assertEquals(PartyJoinTarget.Code("482913"), parsePartyJoinTarget("482-913"))
    }

    @Test
    fun `a link is picked out of the message it was pasted with`() {
        val link = buildPartyLink("111222", 50_000, listOf("10.0.0.2"), secret)

        val target = assertIs<PartyJoinTarget.Link>(parsePartyJoinTarget("Join us: $link see you there"))

        assertEquals("111222", target.code)
        assertEquals(listOf("10.0.0.2"), target.addresses)
    }

    @Test
    fun `an escaped link still parses`() {
        val link = buildPartyLink("111222", 50_000, listOf("10.0.0.2", "2001:db8::5"), secret)
            .replace(",", "%2C")
            .replace(":", "%3A")
            .replaceFirst("provenio%3A", "provenio:")

        val target = assertIs<PartyJoinTarget.Link>(parsePartyJoinTarget(link))

        assertEquals(listOf("10.0.0.2", "2001:db8::5"), target.addresses)
    }

    @Test
    fun `a link without a secret joins like a code`() {
        val target = assertIs<PartyJoinTarget.Link>(
            parsePartyJoinTarget("provenio://party?v=1&c=123456&p=47640&a=192.168.1.5"),
        )

        assertNull(target.secret)
    }

    @Test
    fun `anything else is not a way in`() {
        assertNull(parsePartyJoinTarget("12345"))
        assertNull(parsePartyJoinTarget("provenio://meta?type=movie&id=tt1"))
        assertNull(parsePartyJoinTarget("provenio://party?v=2&c=123456&p=47640&a=192.168.1.5"))
        assertNull(parsePartyJoinTarget("provenio://party?v=1&c=123456&p=47640"))
        assertNull(parsePartyJoinTarget("provenio://party?v=1&c=123456&p=0&a=192.168.1.5"))
        assertNull(parsePartyJoinTarget("provenio://party?v=1&c=123456&p=47640&a=192.168.1.5&k=short"))
    }
}
