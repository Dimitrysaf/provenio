package io.github.dimitrysaf.provenio.shell.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import io.github.dimitrysaf.provenio.desktop.DesktopDbus
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val PortalPollIntervalMs = 3_000L
private const val PortalTimeoutSeconds = 2L

private val isLinux: Boolean = System.getProperty("os.name").orEmpty().lowercase().contains("linux")

@Composable
internal actual fun systemPrefersDarkTheme(): Boolean {
    val fallback = isSystemInDarkTheme()
    if (!isLinux) return fallback
    val portalPreference by produceState<Boolean?>(initialValue = null) {
        while (true) {
            value = withContext(Dispatchers.IO) { readPortalColorScheme() }
            delay(PortalPollIntervalMs)
        }
    }
    return portalPreference ?: fallback
}

private fun readPortalColorScheme(): Boolean? {
    for (method in listOf("ReadOne", "Read")) {
        val reply = DesktopDbus.call(
            busName = "org.freedesktop.portal.Desktop",
            objectPath = "/org/freedesktop/portal/desktop",
            interfaceName = "org.freedesktop.portal.Settings",
            method = method,
            arguments = "('org.freedesktop.appearance', 'color-scheme')",
        ) ?: continue
        return colorSchemeFrom(reply) ?: continue
    }
    val commands = listOf(
        listOf(
            "gdbus", "call", "--session",
            "--dest", "org.freedesktop.portal.Desktop",
            "--object-path", "/org/freedesktop/portal/desktop",
            "--method", "org.freedesktop.portal.Settings.ReadOne",
            "org.freedesktop.appearance", "color-scheme",
        ),
        listOf(
            "gdbus", "call", "--session",
            "--dest", "org.freedesktop.portal.Desktop",
            "--object-path", "/org/freedesktop/portal/desktop",
            "--method", "org.freedesktop.portal.Settings.Read",
            "org.freedesktop.appearance", "color-scheme",
        ),
        listOf(
            "dbus-send", "--session", "--print-reply=literal",
            "--dest=org.freedesktop.portal.Desktop",
            "/org/freedesktop/portal/desktop",
            "org.freedesktop.portal.Settings.ReadOne",
            "string:org.freedesktop.appearance", "string:color-scheme",
        ),
    )
    for (command in commands) {
        val output = runCommand(command) ?: continue
        return colorSchemeFrom(output) ?: continue
    }
    return null
}

private fun colorSchemeFrom(reply: String): Boolean? {
    val value = Regex("""(?:uint32\s+|<+)(\d+)""").find(reply)?.groupValues?.get(1)?.toIntOrNull() ?: return null
    return when (value) {
        1 -> true
        2 -> false
        else -> null
    }
}

private fun runCommand(command: List<String>): String? = runCatching {
    val process = ProcessBuilder(command).redirectErrorStream(true).start()
    if (!process.waitFor(PortalTimeoutSeconds, TimeUnit.SECONDS)) {
        process.destroyForcibly()
        return null
    }
    if (process.exitValue() != 0) return null
    process.inputStream.bufferedReader().readText()
}.getOrNull()

@Composable
internal actual fun SystemBarsAppearance(darkTheme: Boolean) = Unit
