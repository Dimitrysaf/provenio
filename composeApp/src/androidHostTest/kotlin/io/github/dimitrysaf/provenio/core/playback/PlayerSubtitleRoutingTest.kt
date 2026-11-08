package io.github.dimitrysaf.provenio.core.playback

import android.net.Uri
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class PlayerSubtitleRoutingTest {
    @Test
    fun markedSubtitleLoadsFromItsOriginalUrl() {
        val url = "https://subs.example.com/file.srt?token=a%2Bb&lang=en"
        assertEquals(url, markedSubtitleUri(url).withoutSubtitleMarker().toString())
        val plain = "https://subs.example.com/file.ass"
        assertEquals(plain, markedSubtitleUri(plain).withoutSubtitleMarker().toString())
    }

    @Test
    fun streamUrlsAndLocalFilesAreNotMarked() {
        assertNull(Uri.parse("https://cdn.example.com/video.mkv?token=x").withoutSubtitleMarker())
        assertEquals("file:///sdcard/sub.srt", markedSubtitleUri("file:///sdcard/sub.srt").toString())
    }

    @Test
    fun eachSubtitleGetsItsOwnStableTrackId() {
        val first = addonSubtitleTrackId("https://subs.example.com/a.ass")
        assertEquals(first, addonSubtitleTrackId("https://subs.example.com/a.ass"))
        assertNotEquals(first, addonSubtitleTrackId("https://subs.example.com/b.ass"))
    }
}
