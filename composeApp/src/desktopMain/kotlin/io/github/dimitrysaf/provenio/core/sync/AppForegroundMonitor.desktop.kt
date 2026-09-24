package io.github.dimitrysaf.provenio.core.sync

import io.github.dimitrysaf.provenio.desktop.DesktopWindowState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal actual object AppForegroundMonitor {
    actual fun events(): Flow<AppVisibility> =
        DesktopWindowState.isVisible
            .map { visible -> if (visible) AppVisibility.Foreground else AppVisibility.Background }
            .distinctUntilChanged()
}
