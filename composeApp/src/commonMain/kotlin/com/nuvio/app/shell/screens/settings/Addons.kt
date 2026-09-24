package com.nuvio.app.shell.screens.settings

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import com.nuvio.app.shell.screens.addons.AddonsSettingsPageContent

internal fun LazyListScope.addonsSettingsContent() {
    item {
        AddonsSettingsPageContent(
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
