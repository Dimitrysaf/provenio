package io.github.dimitrysaf.provenio.shell.screens.details.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LogoToneTest {

    // A 10×10 image: transparent border, a black square in the middle.
    private fun blackOnClear(x: Int, y: Int): Pair<Float, Float> =
        if (x in 3..6 && y in 3..6) 1f to 0f else 0f to 0f

    @Test
    fun aSeeThroughLogoReportsTheToneOfWhatItDraws() {
        assertEquals(0f, logoTone(10, 10, ::blackOnClear))
    }

    @Test
    fun aLogoWithItsOwnBackgroundNeedsNoPlate() {
        assertNull(logoTone(10, 10) { _, _ -> 1f to 0.1f })
    }

    @Test
    fun anEmptyLogoHasNoTone() {
        assertNull(logoTone(10, 10) { _, _ -> 0f to 0f })
    }
}
