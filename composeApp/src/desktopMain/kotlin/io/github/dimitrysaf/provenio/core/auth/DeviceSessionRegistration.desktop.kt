package io.github.dimitrysaf.provenio.core.auth

import java.io.File

internal actual fun currentDeviceClientMetadata(): DeviceClientMetadata {
    val hostName = runCatching { File("/etc/hostname").readText().trim() }.getOrNull()
        ?.takeIf { it.isNotBlank() }
        ?: System.getenv("HOSTNAME")?.takeIf { it.isNotBlank() }
    return DeviceClientMetadata(
        deviceName = hostName ?: "Linux computer",
        platform = "Linux ${linuxDistribution() ?: System.getProperty("os.version").orEmpty()}".trim(),
    )
}

/** PRETTY_NAME from os-release, e.g. "Fedora Linux 42". Inside Flatpak this names the host. */
private fun linuxDistribution(): String? =
    listOf("/run/host/os-release", "/etc/os-release").firstNotNullOfOrNull { path ->
        runCatching {
            File(path).readLines()
                .firstOrNull { it.startsWith("PRETTY_NAME=") }
                ?.substringAfter('=')
                ?.trim('"')
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }
