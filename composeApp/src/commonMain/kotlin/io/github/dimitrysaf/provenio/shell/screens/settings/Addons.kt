package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import io.github.dimitrysaf.provenio.shell.screens.addons.AddonsSettingsPageContent

internal fun LazyListScope.addonsSettingsContent() {
    item {
        AddonsSettingsPageContent(
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
