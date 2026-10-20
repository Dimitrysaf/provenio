package io.github.dimitrysaf.provenio.shell.screens.settings

import android.graphics.Typeface
import androidx.compose.ui.text.font.FontFamily
import io.github.dimitrysaf.provenio.core.playback.SubtitleFontFamily

internal actual fun subtitleTextFontFamily(family: SubtitleFontFamily): FontFamily = when (family) {
    SubtitleFontFamily.Default -> FontFamily.Default
    SubtitleFontFamily.Serif -> FontFamily.Serif
    SubtitleFontFamily.Monospace -> FontFamily.Monospace
    SubtitleFontFamily.Condensed -> FontFamily(Typeface.create("sans-serif-condensed", Typeface.NORMAL))
}
