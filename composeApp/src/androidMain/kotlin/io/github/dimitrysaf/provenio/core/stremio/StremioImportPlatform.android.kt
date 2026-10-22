package io.github.dimitrysaf.provenio.core.stremio

// Stremio's Android app keeps its sign-in private to itself, so it is signed in to here instead.
internal actual object StremioImportPlatform {
    actual fun savedAuthKeys(): List<String> = emptyList()

    actual fun inflate(data: ByteArray): ByteArray? = inflateZlib(data)
}
