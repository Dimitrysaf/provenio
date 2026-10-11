package io.github.dimitrysaf.provenio.shell.screens.player

import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.StringResource

internal enum class GestureFeedbackIcon {
    Speed,
    Brightness,
    Volume,
    VolumeMuted,
    SeekForward,
    SeekBackward,
}

internal data class GestureFeedbackState(
    val message: String? = null,
    val messageRes: StringResource? = null,
    val messageArgs: List<Any> = emptyList(),
    val icon: GestureFeedbackIcon = GestureFeedbackIcon.Speed,
    val isDanger: Boolean = false,
    val secondaryMessage: String? = null,
    val secondaryMessageRes: StringResource? = null,
    val secondaryMessageArgs: List<Any> = emptyList(),
    val secondaryMessageColor: Color? = null,
    val level: Float? = null,
    /** The top of the level's scale: 1, or 2 while the volume boost is on. */
    val levelMax: Float = 1f,
)
