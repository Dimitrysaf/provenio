package io.github.dimitrysaf.provenio.core.diagnostics

import platform.UIKit.UIDevice

internal actual fun deviceDescription(): String =
    "${UIDevice.currentDevice.systemName} ${UIDevice.currentDevice.systemVersion}, ${UIDevice.currentDevice.model}"
