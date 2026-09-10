package io.github.dimitrysaf.provenio.core.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

/**
 * The language the UI is drawn in, or null to follow the system.
 *
 * Compose Multiplatform resolves `Res.string` against the platform locale, and offers no
 * common API for overriding it, so this is the workaround JetBrains document for an in-app
 * language picker: set the platform default, then re-key the composition so every
 * `stringResource` reads again.
 *
 * https://kotlinlang.org/docs/multiplatform/compose-resource-environment.html
 */
var customAppLocale by mutableStateOf<String?>(null)
    private set

/** Applies [language], and remembers it as the app's choice for the rest of the session. */
fun setAppLanguage(language: AppLanguage) {
    customAppLocale = language.tag
}

expect object LocalAppLocale {
    val current: String
        @Composable get

    @Composable
    infix fun provides(value: String?): ProvidedValue<*>
}

/**
 * Wraps the app so a language change takes effect everywhere at once.
 *
 * The [key] is what does the work: string resources are read during composition, so
 * changing the locale alone would leave every already-composed string stale. Re-keying
 * throws the subtree away and composes it again against the new locale.
 */
@Composable
fun AppEnvironment(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalAppLocale provides customAppLocale,
    ) {
        key(customAppLocale) {
            content()
        }
    }
}
