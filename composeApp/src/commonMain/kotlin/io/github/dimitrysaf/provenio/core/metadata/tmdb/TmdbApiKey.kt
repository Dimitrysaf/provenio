package io.github.dimitrysaf.provenio.core.metadata.tmdb

object TmdbApiKey {
    val hasBuiltInKey: Boolean
        get() = TmdbConfig.API_KEY.isNotBlank()

    fun current(): String =
        TmdbSettingsRepository.snapshot().apiKey.ifBlank { TmdbConfig.API_KEY }
}
