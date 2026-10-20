package io.github.dimitrysaf.provenio.core.diagnostics

internal actual fun deviceDescription(): String =
    "${System.getProperty("os.name")} ${System.getProperty("os.version")} (${System.getProperty("os.arch")})"
