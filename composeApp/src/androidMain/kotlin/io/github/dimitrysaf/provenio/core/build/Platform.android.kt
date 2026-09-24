package io.github.dimitrysaf.provenio.core.build

import android.os.Build

internal actual val isIos: Boolean = false

internal actual val supportsPosterNavigationMotion: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
