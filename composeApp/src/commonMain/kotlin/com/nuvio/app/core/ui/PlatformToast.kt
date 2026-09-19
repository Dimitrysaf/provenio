package com.nuvio.app.core.ui

/**
 * Shows a brief, self-dismissing message over whatever is on screen.
 *
 * These messages are told, not asked: nothing in them is actionable and nothing waits on them, so
 * they are the platform's own transient notice rather than a surface the app draws and has to find
 * room for. Safe to call from any thread; each platform hands the work to its main thread.
 */
internal expect fun platformShowToast(message: String)

/** Clears the message on screen, if any, before it would have gone by itself. */
internal expect fun platformDismissToast()
