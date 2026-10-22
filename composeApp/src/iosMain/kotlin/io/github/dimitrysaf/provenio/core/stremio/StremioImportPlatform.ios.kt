package io.github.dimitrysaf.provenio.core.stremio

// Stremio's iOS app keeps its sign-in private to itself, and watched episodes are not read here.
internal actual object StremioImportPlatform {
    actual fun savedAuthKeys(): List<String> = emptyList()

    actual fun inflate(data: ByteArray): ByteArray? = null
}
