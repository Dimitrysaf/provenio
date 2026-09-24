package io.github.dimitrysaf.provenio.shell.screens.player

import androidx.compose.runtime.Composable
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.lang_afrikaans
import provenio.composeapp.generated.resources.lang_albanian
import provenio.composeapp.generated.resources.lang_amharic
import provenio.composeapp.generated.resources.lang_arabic
import provenio.composeapp.generated.resources.lang_armenian
import provenio.composeapp.generated.resources.lang_azerbaijani
import provenio.composeapp.generated.resources.lang_basque
import provenio.composeapp.generated.resources.lang_belarusian
import provenio.composeapp.generated.resources.lang_bengali
import provenio.composeapp.generated.resources.lang_bosnian
import provenio.composeapp.generated.resources.lang_bulgarian
import provenio.composeapp.generated.resources.lang_burmese
import provenio.composeapp.generated.resources.lang_catalan
import provenio.composeapp.generated.resources.lang_chinese
import provenio.composeapp.generated.resources.lang_chinese_simplified
import provenio.composeapp.generated.resources.lang_chinese_traditional
import provenio.composeapp.generated.resources.lang_croatian
import provenio.composeapp.generated.resources.lang_czech
import provenio.composeapp.generated.resources.lang_danish
import provenio.composeapp.generated.resources.lang_dutch
import provenio.composeapp.generated.resources.lang_english
import provenio.composeapp.generated.resources.lang_estonian
import provenio.composeapp.generated.resources.lang_filipino
import provenio.composeapp.generated.resources.lang_finnish
import provenio.composeapp.generated.resources.lang_french
import provenio.composeapp.generated.resources.lang_galician
import provenio.composeapp.generated.resources.lang_georgian
import provenio.composeapp.generated.resources.lang_german
import provenio.composeapp.generated.resources.lang_greek
import provenio.composeapp.generated.resources.lang_gujarati
import provenio.composeapp.generated.resources.lang_hebrew
import provenio.composeapp.generated.resources.lang_hindi
import provenio.composeapp.generated.resources.lang_hungarian
import provenio.composeapp.generated.resources.lang_icelandic
import provenio.composeapp.generated.resources.lang_indonesian
import provenio.composeapp.generated.resources.lang_irish
import provenio.composeapp.generated.resources.lang_italian
import provenio.composeapp.generated.resources.lang_japanese
import provenio.composeapp.generated.resources.lang_kannada
import provenio.composeapp.generated.resources.lang_kazakh
import provenio.composeapp.generated.resources.lang_khmer
import provenio.composeapp.generated.resources.lang_korean
import provenio.composeapp.generated.resources.lang_lao
import provenio.composeapp.generated.resources.lang_latvian
import provenio.composeapp.generated.resources.lang_lithuanian
import provenio.composeapp.generated.resources.lang_macedonian
import provenio.composeapp.generated.resources.lang_malay
import provenio.composeapp.generated.resources.lang_malayalam
import provenio.composeapp.generated.resources.lang_maltese
import provenio.composeapp.generated.resources.lang_marathi
import provenio.composeapp.generated.resources.lang_mongolian
import provenio.composeapp.generated.resources.lang_nepali
import provenio.composeapp.generated.resources.lang_norwegian
import provenio.composeapp.generated.resources.lang_persian
import provenio.composeapp.generated.resources.lang_polish
import provenio.composeapp.generated.resources.lang_portuguese_brazil
import provenio.composeapp.generated.resources.lang_portuguese_portugal
import provenio.composeapp.generated.resources.lang_punjabi
import provenio.composeapp.generated.resources.lang_romanian
import provenio.composeapp.generated.resources.lang_russian
import provenio.composeapp.generated.resources.lang_serbian
import provenio.composeapp.generated.resources.lang_sinhala
import provenio.composeapp.generated.resources.lang_slovak
import provenio.composeapp.generated.resources.lang_slovenian
import provenio.composeapp.generated.resources.lang_spanish
import provenio.composeapp.generated.resources.lang_spanish_latin_america
import provenio.composeapp.generated.resources.lang_swahili
import provenio.composeapp.generated.resources.lang_swedish
import provenio.composeapp.generated.resources.lang_tamil
import provenio.composeapp.generated.resources.lang_telugu
import provenio.composeapp.generated.resources.lang_thai
import provenio.composeapp.generated.resources.lang_turkish
import provenio.composeapp.generated.resources.lang_ukrainian
import provenio.composeapp.generated.resources.lang_urdu
import provenio.composeapp.generated.resources.lang_uzbek
import provenio.composeapp.generated.resources.lang_vietnamese
import provenio.composeapp.generated.resources.lang_welsh
import provenio.composeapp.generated.resources.lang_zulu
import provenio.composeapp.generated.resources.settings_playback_option_default
import provenio.composeapp.generated.resources.settings_playback_option_device_language
import provenio.composeapp.generated.resources.settings_playback_option_forced
import provenio.composeapp.generated.resources.settings_playback_option_none
import provenio.composeapp.generated.resources.settings_playback_option_original
import provenio.composeapp.generated.resources.subtitle_language_unknown
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import io.github.dimitrysaf.provenio.core.playback.AudioLanguageOption
import io.github.dimitrysaf.provenio.core.playback.AvailableLanguageOptions
import io.github.dimitrysaf.provenio.core.playback.SubtitleLanguageOption
import io.github.dimitrysaf.provenio.core.playback.labelRes
import io.github.dimitrysaf.provenio.core.playback.normalizeLanguageCode
import io.github.dimitrysaf.provenio.core.playback.languageLabelResForCode

@Composable
fun languageLabelForCode(code: String?): String = when {
    code.isNullOrBlank() || code.equals(SubtitleLanguageOption.NONE, ignoreCase = true) ->
        stringResource(Res.string.settings_playback_option_none)
    code.equals(SubtitleLanguageOption.FORCED, ignoreCase = true) ->
        stringResource(Res.string.settings_playback_option_forced)
    code.equals(AudioLanguageOption.DEFAULT, ignoreCase = true) ->
        stringResource(Res.string.settings_playback_option_default)
    code.equals(AudioLanguageOption.DEVICE, ignoreCase = true) ||
        code.equals(SubtitleLanguageOption.DEVICE, ignoreCase = true) ->
        stringResource(Res.string.settings_playback_option_device_language)
    code.equals(AudioLanguageOption.ORIGINAL, ignoreCase = true) ->
        stringResource(Res.string.settings_playback_option_original)
    else -> languageLabelResForCode(code)?.let { stringResource(it) }
        ?: stringResource(Res.string.subtitle_language_unknown)
}

fun resolvePreferredAudioLanguageTargets(
    preferredAudioLanguage: String,
    secondaryPreferredAudioLanguage: String?,
    deviceLanguages: List<String>,
    contentOriginalLanguage: String? = null,
): List<String> {
    fun normalize(language: String?): String? {
        val normalized = normalizeLanguageCode(language)
        return when (normalized) {
            null,
            AudioLanguageOption.DEFAULT,
            AudioLanguageOption.DEVICE,
            SubtitleLanguageOption.NONE,
            SubtitleLanguageOption.FORCED,
            -> null
            AudioLanguageOption.ORIGINAL -> contentOriginalLanguage?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
            else -> normalized
        }
    }

    val primary = normalizeLanguageCode(preferredAudioLanguage) ?: AudioLanguageOption.DEVICE

    return when (primary) {
        AudioLanguageOption.DEFAULT -> listOfNotNull(
            normalize(secondaryPreferredAudioLanguage),
        ).distinct()

        AudioLanguageOption.DEVICE -> (
            deviceLanguages.mapNotNull(::normalize)
                + listOfNotNull(normalize(secondaryPreferredAudioLanguage))
            ).distinct()

        AudioLanguageOption.ORIGINAL -> {
            val originalLang = contentOriginalLanguage?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
            if (originalLang != null) {
                listOfNotNull(
                    originalLang,
                    normalize(secondaryPreferredAudioLanguage),
                ).distinct()
            } else {
                // Fallback to device languages when original language is unknown
                (deviceLanguages.mapNotNull(::normalize)
                    + listOfNotNull(normalize(secondaryPreferredAudioLanguage))
                ).distinct()
            }
        }

        else -> listOfNotNull(
            normalize(preferredAudioLanguage),
            normalize(secondaryPreferredAudioLanguage),
        ).distinct()
    }
}

fun inferForcedSubtitleTrack(
    label: String?,
    language: String?,
    trackId: String?,
    hasForcedSelectionFlag: Boolean = false,
): Boolean {
    if (hasForcedSelectionFlag) return true

    val normalizedLanguage = normalizeLanguageCode(language)
    if (normalizedLanguage == SubtitleLanguageOption.FORCED) return true

    val text = listOfNotNull(label, language, trackId)
        .joinToString(" ")
        .lowercase()

    if ("forced" in text) return true
    return text.contains("songs") && text.contains("sign")
}

/**
 * Best-effort mapping from country name/code to ISO 639-1 primary language.
 * Used as a fallback when [resolveContentLanguage] has no explicit language field.
 */
fun countryToLanguageCode(country: String?): String? {
    val normalized = country?.trim()?.lowercase()?.takeIf { it.isNotBlank() } ?: return null
    return COUNTRY_TO_LANGUAGE_MAP[normalized]
}

/**
 * Resolves the original content language as an ISO 639-1 code.
 * Falls back to country-based inference when the explicit language field is absent.
 */
fun resolveContentLanguage(language: String?, country: String?): String? {
    normalizeLanguageCode(language)?.let { return it }
    countryToLanguageCode(country)?.let { return it }
    return null
}

private val COUNTRY_TO_LANGUAGE_MAP = mapOf(
    // ISO 3166-1 alpha-2
    "jp" to "ja", "kr" to "ko", "cn" to "zh", "tw" to "zh",
    "fr" to "fr", "de" to "de", "it" to "it", "es" to "es",
    "pt" to "pt", "br" to "pt", "ru" to "ru", "in" to "hi",
    "tr" to "tr", "pl" to "pl", "nl" to "nl", "se" to "sv",
    "no" to "no", "dk" to "da", "fi" to "fi", "th" to "th",
    "il" to "he", "cz" to "cs", "ro" to "ro", "hu" to "hu",
    "ua" to "uk", "gr" to "el",
    // ISO 3166-1 alpha-3
    "jpn" to "ja", "kor" to "ko", "chn" to "zh", "twn" to "zh",
    "fra" to "fr", "deu" to "de", "ita" to "it", "esp" to "es",
    "prt" to "pt", "bra" to "pt", "rus" to "ru", "ind" to "hi",
    "tur" to "tr", "pol" to "pl", "nld" to "nl", "swe" to "sv",
    "nor" to "no", "dnk" to "da", "fin" to "fi", "tha" to "th",
    "isr" to "he", "cze" to "cs", "rou" to "ro", "hun" to "hu",
    "ukr" to "uk", "grc" to "el",
    // Common full names
    "japan" to "ja", "south korea" to "ko", "korea" to "ko",
    "china" to "zh", "taiwan" to "zh", "france" to "fr",
    "germany" to "de", "italy" to "it", "spain" to "es",
    "portugal" to "pt", "brazil" to "pt", "russia" to "ru",
    "india" to "hi", "turkey" to "tr", "poland" to "pl",
    "netherlands" to "nl", "sweden" to "sv", "norway" to "no",
    "denmark" to "da", "finland" to "fi", "thailand" to "th",
    "israel" to "he", "romania" to "ro", "hungary" to "hu",
    "ukraine" to "uk", "greece" to "el",
)
