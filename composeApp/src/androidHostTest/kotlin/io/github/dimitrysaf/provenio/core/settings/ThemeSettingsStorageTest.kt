package io.github.dimitrysaf.provenio.core.settings

import android.app.Application
import android.content.Context
import io.github.dimitrysaf.provenio.core.storage.ProfileScopedKey
import io.github.dimitrysaf.provenio.core.sync.decodeSyncBoolean
import kotlinx.serialization.json.buildJsonObject
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ThemeSettingsStorageTest {
    @BeforeTest
    fun initialize() {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("provenio_theme_settings", Context.MODE_PRIVATE).edit().clear().commit()
        ThemeSettingsStorage.initialize(context)
    }

    @Test
    fun glowPreferenceSurvivesReloadAndSync() {
        ThemeSettingsStorage.saveNavBarGlowEnabled(false)
        ThemeSettingsStorage.initialize(RuntimeEnvironment.getApplication())
        assertEquals(false, ThemeSettingsStorage.loadNavBarGlowEnabled())
        val payload = ThemeSettingsStorage.exportToSyncPayload()
        assertEquals(false, payload.decodeSyncBoolean("nav_bar_glow_enabled"))

        ThemeSettingsStorage.saveNavBarGlowEnabled(true)
        ThemeSettingsStorage.replaceFromSyncPayload(payload)
        assertEquals(false, ThemeSettingsStorage.loadNavBarGlowEnabled())
    }

    @Test
    fun olderPayloadClearsOnlyTheCurrentProfilesGlowPreference() {
        val preferences = RuntimeEnvironment.getApplication()
            .getSharedPreferences("provenio_theme_settings", Context.MODE_PRIVATE)
        val otherProfileKey = ProfileScopedKey.of("nav_bar_glow_enabled", 99)
        preferences.edit().putBoolean(otherProfileKey, false).commit()
        ThemeSettingsStorage.saveNavBarGlowEnabled(false)

        ThemeSettingsStorage.replaceFromSyncPayload(buildJsonObject {})

        assertNull(ThemeSettingsStorage.loadNavBarGlowEnabled())
        assertEquals(false, preferences.getBoolean(otherProfileKey, true))
    }
}
