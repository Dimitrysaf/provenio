package io.github.dimitrysaf.provenio.desktop

import io.github.dimitrysaf.provenio.shell.components.AppKeyboardShortcuts
import io.github.dimitrysaf.provenio.shell.components.AppShortcutAction
import java.awt.AWTEvent
import java.awt.Toolkit
import java.awt.event.MouseEvent

/**
 * Goes back and forward with a mouse's side buttons, which Java reports as buttons 4 and 5 on
 * Windows and Linux alike, the same as Alt+Left and Alt+Right.
 */
internal object MouseNavigationButtons {
    private var installed = false

    fun install() {
        if (installed) return
        installed = true
        Toolkit.getDefaultToolkit().addAWTEventListener(
            { event ->
                val mouse = event as? MouseEvent ?: return@addAWTEventListener
                if (mouse.id != MouseEvent.MOUSE_PRESSED) return@addAWTEventListener
                when (mouse.button) {
                    BackButton -> AppKeyboardShortcuts.request(AppShortcutAction.Back)
                    ForwardButton -> AppKeyboardShortcuts.request(AppShortcutAction.Forward)
                }
            },
            AWTEvent.MOUSE_EVENT_MASK,
        )
    }

    private const val BackButton = 4
    private const val ForwardButton = 5
}
