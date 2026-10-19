package io.github.dimitrysaf.provenio.core.build

import android.os.Build

internal actual val isIos: Boolean = false

internal actual val supportsPosterNavigationMotion: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

internal actual val isDesktop: Boolean = false

internal actual val isMacOs: Boolean = false

internal actual val supportsVolumeStabilizer: Boolean = true

internal actual val volumeStabilizerNeedsMpv: Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.P

internal actual val supportsSubtitleAutoSync: Boolean = true
