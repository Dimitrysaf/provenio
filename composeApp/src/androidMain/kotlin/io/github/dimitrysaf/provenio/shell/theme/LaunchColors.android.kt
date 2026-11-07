package io.github.dimitrysaf.provenio.shell.theme

import io.github.dimitrysaf.provenio.core.settings.ThemeSettingsStorage

internal actual fun saveLaunchColors(primary: Int, background: Int) {
    ThemeSettingsStorage.saveLaunchColors(primary, background)
}
