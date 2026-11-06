package io.github.dimitrysaf.provenio.shell.screens.details

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ListSearchMatcherTest {

    @Test
    fun anEmptyQueryKeepsEverything() {
        assertNull(listSearchMatcher("   "))
    }

    @Test
    fun theQueryIsACaseInsensitivePattern() {
        val matcher = listSearchMatcher("bry(an|on)")!!
        assertTrue(matcher("Bryan Cranston"))
        assertTrue(matcher("BRYON"))
        assertFalse(matcher("Aaron Paul"))
    }

    @Test
    fun anInvalidPatternIsMatchedAsPlainText() {
        val matcher = listSearchMatcher("(spoiler")!!
        assertTrue(matcher("No (spoiler here"))
        assertFalse(matcher("No spoiler here"))
    }
}
