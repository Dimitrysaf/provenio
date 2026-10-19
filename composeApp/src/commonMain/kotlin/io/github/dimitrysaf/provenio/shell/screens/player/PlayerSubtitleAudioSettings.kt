package io.github.dimitrysaf.provenio.shell.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.dimitrysaf.provenio.shell.components.PlatformBackHandler
import io.github.dimitrysaf.provenio.shell.components.ScreenScaffold
import io.github.dimitrysaf.provenio.shell.screens.settings.SettingsContentMaxWidth
import io.github.dimitrysaf.provenio.shell.screens.settings.audioSettingsContent
import io.github.dimitrysaf.provenio.shell.screens.settings.subtitlesSettingsContent
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.settings_category_subtitles_audio

/**
 * The Subtitles & Audio settings page, opened from the player's sheets over the video. It stays
 * inside the player, so playback goes on underneath and the changes apply as they are made.
 */
@Composable
internal fun PlayerSubtitleAudioSettings(
    visible: Boolean,
    onDismiss: () -> Unit,
) {
    PlatformBackHandler(enabled = visible, onBack = onDismiss)
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally { it / 4 } + fadeIn(),
        exit = slideOutHorizontally { it / 4 } + fadeOut(),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            ScreenScaffold(
                title = stringResource(Res.string.settings_category_subtitles_audio),
                onBack = onDismiss,
                contentMaxWidth = SettingsContentMaxWidth,
            ) {
                subtitlesSettingsContent(isTablet = false)
                audioSettingsContent(isTablet = false)
            }
        }
    }
}
