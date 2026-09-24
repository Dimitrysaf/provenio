package io.github.dimitrysaf.provenio.shell.components

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

/**
 * Brief messages that go away on their own.
 *
 * These are the platform's own toast rather than a snackbar the app draws: nothing in them is
 * actionable and nothing waits on them, so they need no host placed on a screen, no room reserved
 * in a layout, and they outlive whatever screen raised them.
 */
object ToastController {

    fun show(message: String) = platformShowToast(message)

    fun dismiss() = platformDismissToast()
}
