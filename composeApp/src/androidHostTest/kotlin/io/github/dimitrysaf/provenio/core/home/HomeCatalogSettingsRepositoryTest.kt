package io.github.dimitrysaf.provenio.core.home

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import io.github.dimitrysaf.provenio.core.addons.AddonCatalog
import io.github.dimitrysaf.provenio.core.addons.AddonManifest
import io.github.dimitrysaf.provenio.core.addons.ManagedAddon
import io.github.dimitrysaf.provenio.core.collection.CollectionRepository
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class HomeCatalogSettingsRepositoryTest {
    private lateinit var preferences: RecordingPreferences
    private lateinit var originalLocale: Locale

    @BeforeTest
    fun initialize() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.ENGLISH)
        val context = RuntimeEnvironment.getApplication()
        val stored = context.getSharedPreferences("provenio_home_catalog_settings", Context.MODE_PRIVATE)
        stored.edit().clear().commit()
        preferences = RecordingPreferences(stored)
        HomeCatalogSettingsStorage.initialize(object : ContextWrapper(context) {
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences = preferences
        })
        CollectionRepository.clearLocalState()
        HomeRepository.clear()
        HomeCatalogSettingsRepository.clearLocalState()
        HomeCatalogSettingsRepository.setHeroEnabled(false)
    }

    @AfterTest
    fun clearState() {
        Locale.setDefault(originalLocale)
        HomeCatalogSettingsRepository.clearLocalState()
        CollectionRepository.clearLocalState()
        HomeRepository.clear()
    }

    @Test
    fun unchangedCatalogsDoNotRewriteSettings() {
        val addons = listOf(addon())
        HomeCatalogSettingsRepository.syncCatalogs(addons)
        val editCount = preferences.editCount

        HomeCatalogSettingsRepository.syncCatalogs(addons.map { it.copy() })

        assertEquals(editCount, preferences.editCount)
        assertEquals("test:movie:popular", HomeCatalogSettingsRepository.uiState.value.items.single().key)
    }

    @Test
    fun localeChangesRebuildCatalogLabels() {
        val addons = listOf(addon())
        HomeCatalogSettingsRepository.syncCatalogs(addons)
        val editCount = preferences.editCount

        Locale.setDefault(Locale.FRENCH)
        HomeCatalogSettingsRepository.syncCatalogs(addons)

        assertEquals(editCount + 1, preferences.editCount)
    }

    @Test
    fun profileChangesRebuildTheSameCatalogInputs() {
        val addons = listOf(addon())
        HomeCatalogSettingsRepository.syncCatalogs(addons)
        val settings = HomeCatalogSettingsRepository.uiState.value

        HomeCatalogSettingsRepository.onProfileChanged()
        HomeCatalogSettingsRepository.syncCatalogs(addons)

        assertEquals(settings, HomeCatalogSettingsRepository.uiState.value)
    }

    private fun addon() = ManagedAddon(
        manifestUrl = "https://example.com/manifest.json",
        manifest = AddonManifest(
            id = "test",
            name = "Catalogs",
            description = "",
            version = "1",
            resources = emptyList(),
            types = listOf("movie"),
            catalogs = listOf(AddonCatalog(type = "movie", id = "popular", name = "Popular")),
            transportUrl = "https://example.com",
        ),
    )

    private class RecordingPreferences(
        private val delegate: SharedPreferences,
    ) : SharedPreferences by delegate {
        var editCount = 0

        override fun edit(): SharedPreferences.Editor {
            editCount += 1
            return delegate.edit()
        }
    }
}
