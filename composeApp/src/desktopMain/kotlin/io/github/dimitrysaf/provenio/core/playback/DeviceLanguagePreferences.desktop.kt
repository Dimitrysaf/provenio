package io.github.dimitrysaf.provenio.core.playback

import java.util.Locale

internal actual object DeviceLanguagePreferences {
    actual fun preferredLanguageCodes(): List<String> {
        val locales = buildList {
            // LANGUAGE lists the user's fallback chain, e.g. "el:en_GB:en".
            System.getenv("LANGUAGE")?.split(':')?.filter { it.isNotBlank() }?.forEach {
                add(Locale.forLanguageTag(it.substringBefore('.').replace('_', '-')))
            }
            add(Locale.getDefault())
            add(Locale.ENGLISH)
        }
        return locales
            .flatMap { listOf(it.toLanguageTag(), it.language) }
            .mapNotNull(::normalizeLanguageCode)
            .distinct()
    }
}
