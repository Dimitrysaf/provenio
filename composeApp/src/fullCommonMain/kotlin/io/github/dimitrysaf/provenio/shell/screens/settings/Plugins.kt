package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import io.github.dimitrysaf.provenio.shell.screens.plugins.PluginsSettingsPageContent

internal actual fun LazyListScope.pluginsSettingsContent() {
    item {
        PluginsSettingsPageContent(
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
