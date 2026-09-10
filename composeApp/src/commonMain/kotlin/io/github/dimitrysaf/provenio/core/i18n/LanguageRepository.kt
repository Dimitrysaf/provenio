package io.github.dimitrysaf.provenio.core.i18n

import io.github.dimitrysaf.provenio.data.LanguageStore
import io.github.dimitrysaf.provenio.data.createDatabaseDriver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Which language the UI is drawn in, and where that choice is kept between launches. */
object LanguageRepository {

    private val _language = MutableStateFlow(AppLanguage.System)
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private var store: LanguageStore? = null
    private var loaded = false

    fun load() {
        if (loaded) return
        loaded = true
        store = runCatching { LanguageStore(createDatabaseDriver()) }.getOrNull()
        // Applied rather than set: going through setLanguage would write the stored value
        // straight back to the row it was just read from.
        store?.let { existing ->
            runCatching { existing.load() }.getOrNull()?.let { stored ->
                _language.value = stored
                setAppLanguage(stored)
            }
        }
    }

    fun setLanguage(language: AppLanguage) {
        _language.value = language
        setAppLanguage(language)
        store?.let { runCatching { it.save(language) } }
    }
}
