package io.github.dimitrysaf.provenio.core.party

import kotlin.test.Test
import kotlin.test.assertEquals

class PartyProtocolTest {

    private fun roundTrip(message: PartyMessage): PartyMessage =
        partyJson.decodeFromString(PartyMessage.serializer(), partyJson.encodeToString(PartyMessage.serializer(), message))

    @Test
    fun `a guest's play, pause or seek arrives as sent`() {
        val control = PartyMessage.Control(playbackId = 3, action = PartyControlAction.SEEK, positionMs = 61_000)

        assertEquals(control, roundTrip(control))
    }

    @Test
    fun `a guest's loading arrives as sent`() {
        val loading = PartyMessage.Loading(playbackId = 3, loading = true)

        assertEquals(loading, roundTrip(loading))
    }

    @Test
    fun `the host's place carries who the room waits for and who moved it`() {
        val state = PartyMessage.PlaybackState(
            PartyPlaybackState(
                playbackId = 3,
                isPlaying = false,
                positionMs = 12_000,
                isLoading = true,
                seekCount = 2,
                waitingFor = listOf("Maria"),
                changedBy = "Nikos",
            ),
        )

        assertEquals(state, roundTrip(state))
    }

    @Test
    fun `settings without the playback rules keep to the room as it was`() {
        val settings = partyJson.decodeFromString(PartySettings.serializer(), """{"chatEnabled":false}""")

        assertEquals(PartyGuestPause.SNAP_TO_HOST, settings.guestPause)
        assertEquals(PartyGuestSeek.SNAP_BACK, settings.guestSeek)
        assertEquals(PartyLoading.KEEP_UP, settings.loading)
        assertEquals(30, settings.loadingWaitSeconds)
        assertEquals(PartySyncTolerance.NORMAL, settings.syncTolerance)
    }

    @Test
    fun `the room's playback rules arrive as the host set them`() {
        val settings = PartyMessage.Settings(
            PartySettings(
                guestPause = PartyGuestPause.PAUSE_EVERYONE,
                guestSeek = PartyGuestSeek.SEEK_EVERYONE,
                loading = PartyLoading.PAUSE_EVERYONE,
                loadingWaitSeconds = null,
                syncTolerance = PartySyncTolerance.TIGHT,
            ),
        )

        assertEquals(settings, roundTrip(settings))
    }
}
