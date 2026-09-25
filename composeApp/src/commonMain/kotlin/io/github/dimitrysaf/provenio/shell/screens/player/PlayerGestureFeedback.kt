package io.github.dimitrysaf.provenio.shell.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeMute
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.BrightnessLow
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/** What the player shows while a gesture changes brightness, volume, seek position or speed. */
@Composable
internal fun BoxScope.PlayerGestureFeedbackOverlay(
    visible: Boolean,
    feedback: GestureFeedbackState?,
) {
    val level = feedback?.level
    val onLeft = feedback?.icon == GestureFeedbackIcon.Brightness
    AnimatedVisibility(
        visible = visible && feedback != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = when {
            level == null -> Modifier
                .align(Alignment.TopCenter)
                .playerFrameInsets(WindowInsetsSides.Top)
                .padding(top = 24.dp)
            onLeft -> Modifier
                .align(Alignment.CenterStart)
                .playerFrameInsets(WindowInsetsSides.Horizontal)
                .padding(start = 32.dp)
            else -> Modifier
                .align(Alignment.CenterEnd)
                .playerFrameInsets(WindowInsetsSides.Horizontal)
                .padding(end = 32.dp)
        },
    ) {
        feedback ?: return@AnimatedVisibility
        if (level != null) {
            GestureLevelSlider(icon = feedback.levelIcon(level), level = level)
        } else {
            GestureMessagePill(feedback)
        }
    }
}

@Composable
private fun GestureLevelSlider(icon: ImageVector, level: Float) {
    val shown by animateFloatAsState(level.coerceIn(0f, 1f))
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(168.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {}
                Surface(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(shown),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                ) {}
            }
            Text(
                text = "${(level.coerceIn(0f, 1f) * 100f).roundToInt()}",
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun GestureMessagePill(feedback: GestureFeedbackState) {
    val message = feedback.message
        ?: feedback.messageRes?.let { stringResource(it, *feedback.messageArgs.toTypedArray()) }
        ?: return
    val secondary = feedback.secondaryMessage
        ?: feedback.secondaryMessageRes?.let { stringResource(it, *feedback.secondaryMessageArgs.toTypedArray()) }
    Surface(
        shape = CircleShape,
        color = if (feedback.isDanger) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        contentColor = if (feedback.isDanger) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(imageVector = feedback.icon.imageVector(), contentDescription = null, modifier = Modifier.size(24.dp))
            Text(text = message, style = MaterialTheme.typography.titleMedium)
            if (secondary != null) {
                Text(
                    text = secondary,
                    style = MaterialTheme.typography.titleMedium,
                    color = feedback.secondaryMessageColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun GestureFeedbackState.levelIcon(level: Float): ImageVector = when (icon) {
    GestureFeedbackIcon.Brightness -> when {
        level < 0.34f -> Icons.Rounded.BrightnessLow
        level < 0.67f -> Icons.Rounded.BrightnessMedium
        else -> Icons.Rounded.BrightnessHigh
    }
    GestureFeedbackIcon.VolumeMuted -> Icons.AutoMirrored.Rounded.VolumeOff
    else -> when {
        level <= 0f -> Icons.AutoMirrored.Rounded.VolumeMute
        level < 0.5f -> Icons.AutoMirrored.Rounded.VolumeDown
        else -> Icons.AutoMirrored.Rounded.VolumeUp
    }
}

private fun GestureFeedbackIcon.imageVector(): ImageVector = when (this) {
    GestureFeedbackIcon.Speed -> Icons.Rounded.Speed
    GestureFeedbackIcon.Brightness -> Icons.Rounded.BrightnessMedium
    GestureFeedbackIcon.Volume -> Icons.AutoMirrored.Rounded.VolumeUp
    GestureFeedbackIcon.VolumeMuted -> Icons.AutoMirrored.Rounded.VolumeOff
    GestureFeedbackIcon.SeekForward -> Icons.Rounded.FastForward
    GestureFeedbackIcon.SeekBackward -> Icons.Rounded.FastRewind
}
