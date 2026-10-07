package io.github.dimitrysaf.provenio.shell.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ShapedArtworkTest {
    private val wide = 16f / 9f
    private val tall = 2f / 3f

    @Test
    fun `wide slot skips a poster for the backdrop after it`() {
        val ratios = mapOf("poster.jpg" to tall, "backdrop.jpg" to wide)
        val choice = chooseArtwork(listOf("poster.jpg", "backdrop.jpg"), wide, ratios::get)
        assertEquals(ArtworkChoice("backdrop.jpg", letterbox = false, pending = false), choice)
    }

    @Test
    fun `tall slot skips a backdrop for the poster after it`() {
        val ratios = mapOf("poster.jpg" to tall, "backdrop.jpg" to wide)
        val choice = chooseArtwork(listOf("backdrop.jpg", "poster.jpg"), tall, ratios::get)
        assertEquals(ArtworkChoice("poster.jpg", letterbox = false, pending = false), choice)
    }

    @Test
    fun `measures an unloaded candidate before trying later ones`() {
        val ratios = mapOf("backdrop.jpg" to wide)
        val choice = chooseArtwork(listOf("unknown.jpg", "backdrop.jpg"), wide, ratios::get)
        assertEquals(ArtworkChoice("unknown.jpg", letterbox = false, pending = true), choice)
    }

    @Test
    fun `fits the closest whole when nothing has the right shape`() {
        val ratios = mapOf("strip.jpg" to 0.4f, "poster.jpg" to tall)
        val choice = chooseArtwork(listOf("strip.jpg", "poster.jpg"), wide, ratios::get)
        assertEquals(ArtworkChoice("poster.jpg", letterbox = true, pending = false), choice)
    }

    @Test
    fun `skips images that failed to load`() {
        val ratios = mapOf("broken.jpg" to ArtworkRatios.Failed, "backdrop.jpg" to wide)
        val choice = chooseArtwork(listOf("broken.jpg", "backdrop.jpg"), wide, ratios::get)
        assertEquals("backdrop.jpg", choice?.url)
    }

    @Test
    fun `has nothing to show without candidates`() {
        assertNull(chooseArtwork(emptyList(), wide) { null })
    }
}
