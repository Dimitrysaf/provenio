package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.runtime.mutableStateMapOf

/**
 * Which grid shelves the person opened or closed by tapping their title, on any page. It lasts
 * until the app closes; without a tap, a shelf follows its page's Expanded by default setting, and
 * changing that setting clears every tap so all shelves follow it again.
 */
internal object ShelfExpansion {
    private val overrides = mutableStateMapOf<String, Boolean>()

    fun isExpanded(key: String, expandedByDefault: Boolean): Boolean =
        overrides[key] ?: expandedByDefault

    fun toggle(key: String, expandedByDefault: Boolean) {
        overrides[key] = !isExpanded(key, expandedByDefault)
    }

    /** Library's shelves have keys starting with this, so its setting resets only its own taps. */
    const val LibraryKeyPrefix = "library:"

    fun resetLibrary() {
        overrides.keys.filter { it.startsWith(LibraryKeyPrefix) }.forEach(overrides::remove)
    }

    /** Home's setting also governs title, person and company pages, so it resets all but Library's. */
    fun resetHomeAndDetails() {
        overrides.keys.filterNot { it.startsWith(LibraryKeyPrefix) }.forEach(overrides::remove)
    }
}
