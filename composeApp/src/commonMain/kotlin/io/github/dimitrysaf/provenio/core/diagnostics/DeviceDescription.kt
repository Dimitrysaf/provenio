package io.github.dimitrysaf.provenio.core.diagnostics

/** The system and device the app runs on, in one line, for a bug report. */
internal expect fun deviceDescription(): String
