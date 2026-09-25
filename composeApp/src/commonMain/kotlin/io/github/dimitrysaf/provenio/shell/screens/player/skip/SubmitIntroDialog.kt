package io.github.dimitrysaf.provenio.shell.screens.player.skip

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.action_cancel
import provenio.composeapp.generated.resources.submit_intro_button_submit
import provenio.composeapp.generated.resources.submit_intro_capture_button
import provenio.composeapp.generated.resources.submit_intro_end_time_label
import provenio.composeapp.generated.resources.submit_intro_segment_intro
import provenio.composeapp.generated.resources.submit_intro_segment_outro
import provenio.composeapp.generated.resources.submit_intro_segment_recap
import provenio.composeapp.generated.resources.submit_intro_segment_type_label
import provenio.composeapp.generated.resources.submit_intro_start_time_label
import provenio.composeapp.generated.resources.submit_intro_title
import org.jetbrains.compose.resources.stringResource
import kotlin.math.floor
import io.github.dimitrysaf.provenio.core.playback.skip.SkipIntroRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitIntroDialog(
    imdbId: String,
    season: Int,
    episode: Int,
    currentTimeSec: Double,
    segmentType: String,
    onSegmentTypeChange: (String) -> Unit,
    startTimeStr: String,
    onStartTimeChange: (String) -> Unit,
    endTimeStr: String,
    onEndTimeChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var isSubmitting by remember { mutableStateOf(false) }
    val segments = listOf(
        "intro" to stringResource(Res.string.submit_intro_segment_intro),
        "recap" to stringResource(Res.string.submit_intro_segment_recap),
        "outro" to stringResource(Res.string.submit_intro_segment_outro),
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.submit_intro_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(Res.string.submit_intro_segment_type_label),
                    style = MaterialTheme.typography.labelLarge,
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    segments.forEachIndexed { index, (type, label) ->
                        SegmentedButton(
                            selected = segmentType == type,
                            onClick = { onSegmentTypeChange(type) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = segments.size),
                        ) {
                            Text(label)
                        }
                    }
                }
                TimeInputField(
                    label = stringResource(Res.string.submit_intro_start_time_label),
                    value = startTimeStr,
                    onValueChange = onStartTimeChange,
                    onCapture = { onStartTimeChange(formatSecondsToMMSS(currentTimeSec)) },
                )
                TimeInputField(
                    label = stringResource(Res.string.submit_intro_end_time_label),
                    value = endTimeStr,
                    onValueChange = onEndTimeChange,
                    onCapture = { onEndTimeChange(formatSecondsToMMSS(currentTimeSec)) },
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSubmitting,
                onClick = {
                    val start = parseTimeToSeconds(startTimeStr)
                    val end = parseTimeToSeconds(endTimeStr)
                    if (start != null && end != null && end > start) {
                        isSubmitting = true
                        scope.launch {
                            val result = SkipIntroRepository.submitIntro(
                                imdbId = imdbId,
                                season = season,
                                episode = episode,
                                startSec = start,
                                endSec = end,
                                segmentType = segmentType,
                            )
                            isSubmitting = false
                            if (result) {
                                onSuccess()
                            }
                        }
                    }
                },
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(Res.string.submit_intro_button_submit))
                }
            }
        },
        dismissButton = {
            TextButton(enabled = !isSubmitting, onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    )
}

@Composable
private fun TimeInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onCapture: () -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        trailingIcon = {
            IconButton(onClick = onCapture) {
                Icon(
                    imageVector = Icons.Rounded.GpsFixed,
                    contentDescription = stringResource(Res.string.submit_intro_capture_button),
                )
            }
        },
    )
}

private fun formatSecondsToMMSS(seconds: Double): String {
    val mins = floor(seconds / 60).toInt()
    val secs = floor(seconds % 60).toInt()
    return "${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}"
}

private fun parseTimeToSeconds(input: String): Double? {
    if (input.isBlank()) return null

    // Check for separator (colon or dot)
    val separator = when {
        input.contains(':') -> ":"
        input.contains('.') -> "."
        else -> null
    }

    if (separator != null) {
        val parts = input.split(separator)
        if (parts.size == 2) {
            val mins = parts[0].toIntOrNull() ?: return null
            val secs = parts[1].toIntOrNull() ?: return null
            // If the user uses a dot, we assume they mean MM.SS (e.g. 1.24 = 1m 24s)
            // But we only treat it as minutes if seconds are 0-59.
            if (secs in 0..59) {
                return (mins * 60 + secs).toDouble()
            }
        }
    }

    return input.toDoubleOrNull()
}
