package io.github.dimitrysaf.provenio.core.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class AppIconOptionTest {
    @Test
    fun platformDefaultIconIsArcticBlue() {
        assertEquals(AppIconOption.ARCTIC_BLUE, AppIconOption.DEFAULT)
        assertEquals(AppIconOption.ARCTIC_BLUE, AppIconOption.fromPlatformName(null))
    }

    @Test
    fun alternateIconNamesRoundTrip() {
        AppIconOption.entries.forEach { icon ->
            assertEquals(icon, AppIconOption.fromPlatformName(icon.platformName))
        }
    }

    @Test
    fun shortlistedCatalogueContainsSixIcons() {
        assertEquals(6, AppIconOption.entries.size)
    }

    @Test
    fun unknownIconFallsBackToDefault() {
        assertEquals(AppIconOption.DEFAULT, AppIconOption.fromPlatformName("UnknownIcon"))
    }

    @Test
    fun keysRoundTrip() {
        AppIconOption.entries.forEach { icon ->
            assertEquals(icon, AppIconOption.fromKey(icon.key))
        }
        assertEquals(null, AppIconOption.fromKey("dynamic"))
    }
}
