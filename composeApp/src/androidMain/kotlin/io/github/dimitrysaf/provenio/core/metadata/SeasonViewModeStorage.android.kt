package io.github.dimitrysaf.provenio.core.metadata

import android.content.Context
import android.content.SharedPreferences
import io.github.dimitrysaf.provenio.core.storage.ProfileScopedKey

actual object SeasonViewModeStorage {
    private const val preferencesName = "provenio_season_view_mode"
    private const val key = "season_view_mode"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    actual fun load(): SeasonViewMode? =
        preferences?.getString(ProfileScopedKey.of(key), null)?.let(SeasonViewMode::parse)

    actual fun save(mode: SeasonViewMode) {
        preferences
            ?.edit()
            ?.putString(ProfileScopedKey.of(key), SeasonViewMode.persist(mode))
            ?.apply()
    }
}
