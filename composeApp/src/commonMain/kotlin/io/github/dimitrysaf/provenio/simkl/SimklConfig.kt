package io.github.dimitrysaf.provenio.simkl

/**
 * Simkl application identity.
 *
 * [ClientId] is a public client identifier, not a credential. It ships in the APK and
 * belongs in the repository. The PIN flow deliberately has no client secret, which is what
 * makes it usable from an open source app at all.
 *
 * Every Simkl request must carry client_id, app-name and app-version as query parameters
 * and a User-Agent header. [AppName] has to match the name the app was registered under.
 */
object SimklConfig {

    const val ClientId =
        "26bb1aa7a7dfce132f9d2936bce22157293cf1ec886da75a400c19cfb5cc109c"

    const val AppName = "provenio"

    const val AppVersion = "0.0.1"

    val userAgent: String get() = "$AppName/$AppVersion"

    val isConfigured: Boolean get() = !ClientId.startsWith("REPLACE_WITH")
}
