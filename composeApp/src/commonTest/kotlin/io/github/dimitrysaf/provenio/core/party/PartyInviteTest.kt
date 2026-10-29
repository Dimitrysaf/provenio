package io.github.dimitrysaf.provenio.core.party

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PartyInviteTest {

    private val secret = ByteArray(PARTY_SECRET_SIZE) { it.toByte() }

    private val endpoints = listOf(
        PartyEndpoint("203.0.113.7", 51_234),
        PartyEndpoint("2001:db8::5", 47_640),
        PartyEndpoint("192.168.1.5", 47_640),
    )

    @Test
    fun `a built link parses back to the same room`() {
        val invite = assertNotNull(parsePartyInvite(buildPartyLink(endpoints, secret)))

        assertEquals(endpoints, invite.endpoints)
        assertContentEquals(secret, invite.secret)
    }

    @Test
    fun `a link is picked out of the message it was pasted with`() {
        val link = buildPartyLink(endpoints, secret)

        val invite = assertNotNull(parsePartyInvite("Join us: $link see you there"))

        assertEquals(endpoints, invite.endpoints)
    }

    @Test
    fun `an escaped link still parses`() {
        val link = buildPartyLink(endpoints, secret)
            .replace(",", "%2C")
            .replace(":", "%3A")
            .replace("[", "%5B")
            .replace("]", "%5D")
            .replaceFirst("provenio%3A", "provenio:")

        val invite = assertNotNull(parsePartyInvite(link))

        assertEquals(endpoints, invite.endpoints)
    }

    @Test
    fun `anything else is not a way in`() {
        val key = encodePartyBytes(secret)
        assertNull(parsePartyInvite("482913"))
        assertNull(parsePartyInvite("provenio://meta?type=movie&id=tt1"))
        assertNull(parsePartyInvite("provenio://party?v=1&c=123456&p=47640&a=192.168.1.5&k=$key"))
        assertNull(parsePartyInvite("provenio://party?v=2&a=192.168.1.5&k=$key"))
        assertNull(parsePartyInvite("provenio://party?v=2&a=192.168.1.5:0&k=$key"))
        assertNull(parsePartyInvite("provenio://party?v=2&a=192.168.1.5:47640"))
        assertNull(parsePartyInvite("provenio://party?v=2&a=192.168.1.5:47640&k=short"))
    }

    @Test
    fun `only internet addresses count as public`() {
        assertTrue(isPublicIpv4("203.0.113.7"))
        assertTrue(isPublicIpv4("8.8.8.8"))
        assertFalse(isPublicIpv4("192.168.1.1"))
        assertFalse(isPublicIpv4("10.0.0.1"))
        assertFalse(isPublicIpv4("172.20.0.1"))
        assertFalse(isPublicIpv4("100.72.1.1"))
        assertFalse(isPublicIpv4("not an address"))
    }

    @Test
    fun `local files and this device's server are not shared`() {
        assertTrue(isShareablePartySource("torrent://abc?index=1", "abc"))
        assertTrue(isShareablePartySource("https://cdn.example.com/video.mkv", null))
        assertFalse(isShareablePartySource("http://127.0.0.1:8090/stream/1", null))
        assertFalse(isShareablePartySource("file:///storage/movie.mkv", null))
        assertFalse(isShareablePartySource("content://downloads/1", null))
    }
}
