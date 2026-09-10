package io.github.dimitrysaf.provenio.core.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * Android resolves resources through the [android.content.res.Configuration], so overriding
 * the locale means updating that as well as the JVM default, and handing the amended
 * configuration back down through [LocalConfiguration].
 *
 * Straight from the JetBrains recipe, deprecations included: `updateConfiguration` is what
 * reaches the resources a `stringResource` actually reads, and its replacement does not
 * apply to an existing instance.
 *
 * https://kotlinlang.org/docs/multiplatform/compose-resource-environment.html
 */
@Suppress("DEPRECATION")
actual object LocalAppLocale {
    private var default: Locale? = null

    actual val current: String
        @Composable get() = Locale.getDefault().toString()

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        val configuration = LocalConfiguration.current
        if (default == null) {
            default = Locale.getDefault()
        }
        val new = when (value) {
            null -> default!!
            else -> Locale(value)
        }
        Locale.setDefault(new)
        configuration.setLocale(new)
        val resources = LocalContext.current.resources
        resources.updateConfiguration(configuration, resources.displayMetrics)
        return LocalConfiguration.provides(configuration)
    }
}
