package io.github.dimitrysaf.provenio.shell.components

import io.github.dimitrysaf.provenio.desktop.DesktopToasts

internal actual fun platformShowToast(message: String) {
    DesktopToasts.show(message)
}

internal actual fun platformDismissToast() {
    DesktopToasts.dismiss()
}
