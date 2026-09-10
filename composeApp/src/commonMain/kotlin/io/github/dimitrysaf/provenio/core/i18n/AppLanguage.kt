package io.github.dimitrysaf.provenio.core.i18n

/**
 * The languages the picker offers.
 *
 * [tag] is an ISO-639-1 code, and is what selects the `composeResources/values-<tag>`
 * directory; null follows the system. [endonym] is the language's name in itself, which is
 * deliberately not translated — a picker that renders every language in the language you
 * are currently reading is useless to the person trying to leave it. [AppLanguage.System]
 * is the exception and takes its label from the string resources like anything else.
 *
 * Adding a language is two steps: a `composeResources/values-<tag>/strings.xml` translated
 * from `values/strings.xml`, and one entry here.
 */
enum class AppLanguage(val tag: String?, val endonym: String?) {
    System(tag = null, endonym = null),
    English(tag = "en", endonym = "English"),
    Greek(tag = "el", endonym = "Ελληνικά"),
}
