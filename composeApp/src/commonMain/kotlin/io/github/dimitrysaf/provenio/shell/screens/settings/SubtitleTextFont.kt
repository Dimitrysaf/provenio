package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.ui.text.font.FontFamily
import io.github.dimitrysaf.provenio.core.playback.SubtitleFontFamily

/** The font subtitles are drawn in for [family], wherever Provenio draws them itself. */
internal expect fun subtitleTextFontFamily(family: SubtitleFontFamily): FontFamily
