package io.github.dimitrysaf.provenio.shell.screens.player

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.dimitrysaf.provenio.shell.screens.settings.drawnSubtitleStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/**
 * The subtitles Provenio draws itself, for an engine that hands it each line instead of drawing
 * plain-text subtitles in its own limited way. They sit over the video and under everything else,
 * drawn as the settings preview draws them.
 */
@Composable
internal fun PlayerScreenRuntime.RenderAppDrawnSubtitles() {
    val controller = playerController ?: return
    var line by remember(controller) { mutableStateOf<String?>(null) }
    LaunchedEffect(controller) {
        while (isActive) {
            line = withContext(Dispatchers.Default) { controller.appDrawnSubtitleText() }
            delay(AppDrawnSubtitlePollMs)
        }
    }
    val raw = line?.takeIf { it.isNotBlank() } ?: return
    val text = remember(raw) { assSubtitleText(raw) }
    SubtitleFrame(
        text = text,
        style = playerSettingsUiState.drawnSubtitleStyle(),
        modifier = Modifier.fillMaxSize(),
    )
}

/** Often enough that a line shows within a frame or two of its time. */
private const val AppDrawnSubtitlePollMs = 50L
