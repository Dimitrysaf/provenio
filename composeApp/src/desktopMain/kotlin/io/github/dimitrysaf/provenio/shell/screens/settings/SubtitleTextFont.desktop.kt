package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.SystemFont
import io.github.dimitrysaf.provenio.core.playback.SubtitleFontFamily
import io.github.dimitrysaf.provenio.core.playback.desktopSubtitleFont

internal actual fun subtitleTextFontFamily(family: SubtitleFontFamily): FontFamily =
    if (family == SubtitleFontFamily.Default) FontFamily.Default else FontFamily(SystemFont(family.desktopSubtitleFont()))
