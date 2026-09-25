package io.github.dimitrysaf.provenio.core.settings

import android.app.Application
import android.content.Context
import io.github.dimitrysaf.provenio.core.sync.decodeSyncString
import io.github.dimitrysaf.provenio.core.membership.MemberAccessRepository
import io.github.dimitrysaf.provenio.core.membership.MemberAssetStorage
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ThemeSettingsRepositoryTest {
    @BeforeTest
    fun initialize() {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("provenio_theme_settings", Context.MODE_PRIVATE).edit().clear().commit()
        ThemeSettingsStorage.initialize(context)
        MemberAssetStorage.initialize(context)
        MemberAccessRepository.clearLocalState()
        ThemeSettingsRepository.clearLocalState()
    }

    @AfterTest
    fun clearState() {
        ThemeSettingsRepository.clearLocalState()
    }

    @Test
    fun nonMembersCanSaveAndReloadASolidCustomTheme() {
        val colors = CustomThemeColors.solid(0x2255AA)

        ThemeSettingsRepository.setCustomTheme(colors)
        ThemeSettingsRepository.onProfileChanged()

        assertEquals(AppTheme.CUSTOM, ThemeSettingsRepository.selectedTheme.value)
        assertEquals(colors, ThemeSettingsRepository.customThemeColors.value)
        assertEquals(colors, ThemeSettingsRepository.customThemePreference.value)
        assertEquals(colors.encode(), ThemeSettingsStorage.loadCustomThemeColors())
    }

    @Test
    fun savingAGradientKeepsAllItsStops() {
        val expected = CustomThemeColors(0x112233, 0x445566, 0x778899)
        ThemeSettingsRepository.setCustomTheme(expected)

        assertEquals(AppTheme.CUSTOM, ThemeSettingsRepository.selectedTheme.value)
        assertEquals(expected, ThemeSettingsRepository.customThemeColors.value)
        assertEquals(expected, ThemeSettingsRepository.customThemePreference.value)
        assertEquals(expected.encode(), ThemeSettingsStorage.loadCustomThemeColors())
    }

    @Test
    fun loadingASavedGradientShowsAllItsStops() {
        val saved = CustomThemeColors(0x112233, 0x445566, 0x778899)
        ThemeSettingsStorage.saveCustomThemeColors(saved.encode())
        ThemeSettingsStorage.saveSelectedTheme(AppTheme.CUSTOM.name)

        ThemeSettingsRepository.ensureLoaded()
        ThemeSettingsRepository.setTheme(AppTheme.WHITE)
        ThemeSettingsRepository.setTheme(AppTheme.CUSTOM)

        assertEquals(AppTheme.CUSTOM, ThemeSettingsRepository.selectedTheme.value)
        assertEquals(saved, ThemeSettingsRepository.customThemeColors.value)
        assertEquals(saved, ThemeSettingsRepository.customThemePreference.value)
        assertEquals(saved.encode(), ThemeSettingsStorage.exportToSyncPayload().decodeSyncString("custom_theme_colors"))
    }
}
