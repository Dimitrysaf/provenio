package io.github.dimitrysaf.provenio.core.stremio

// What reading Stremio's data needs from the system: its app's saved sign-in, which only a desktop
// can reach, and zlib, which iOS does not offer here.
internal expect object StremioImportPlatform {
    /** Sign-in keys kept by a Stremio app installed on this device, the most recent first. */
    fun savedAuthKeys(): List<String>

    /** The zlib data [data] inflated, or null where it cannot be. */
    fun inflate(data: ByteArray): ByteArray?
}
