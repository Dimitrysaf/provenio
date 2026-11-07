package io.github.dimitrysaf.provenio.shell.screens.details.components

import io.github.dimitrysaf.provenio.shell.components.FindableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explicit
import androidx.compose.material.icons.rounded.LocalBar
import androidx.compose.material.icons.rounded.MoodBad
import androidx.compose.material.icons.rounded.NoAdultContent
import androidx.compose.material.icons.rounded.SportsMma
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import io.github.dimitrysaf.provenio.core.playback.ParentalWarning
import io.github.dimitrysaf.provenio.shell.theme.provenio
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.action_ok
import provenio.composeapp.generated.resources.meta_section_parents_guide_title
import provenio.composeapp.generated.resources.parental_alcohol_explanation
import provenio.composeapp.generated.resources.parental_frightening_explanation
import provenio.composeapp.generated.resources.parental_nudity_explanation
import provenio.composeapp.generated.resources.parental_profanity_explanation
import provenio.composeapp.generated.resources.parental_severity_mild_explanation
import provenio.composeapp.generated.resources.parental_severity_moderate_explanation
import provenio.composeapp.generated.resources.parental_severity_severe_explanation
import provenio.composeapp.generated.resources.parental_violence_explanation

/** The parents guide as a segmented list; tapping a row explains what it and its severity mean. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DetailParentsGuideSection(
    warnings: List<ParentalWarning>,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
) {
    if (warnings.isEmpty()) return
    var explained by remember { mutableStateOf<ParentalWarning?>(null) }

    DetailSection(
        title = stringResource(Res.string.meta_section_parents_guide_title),
        modifier = modifier,
        showHeader = showHeader,
    ) {
        DetailSegmentedColumn {
            warnings.forEachIndexed { index, warning ->
                val color = warning.severityColor()
                SegmentedListItem(
                    onClick = { explained = warning },
                    shapes = detailSegmentShapes(index = index, count = warnings.size),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ListItemDefaults.segmentedColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                    leadingContent = {
                        Icon(imageVector = warning.icon(), contentDescription = null, tint = color)
                    },
                    trailingContent = {
                        FindableText(
                            text = warning.severity,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = color,
                        )
                    },
                ) {
                    FindableText(text = warning.label)
                }
            }
        }
    }
    explained?.let { warning ->
        ParentalWarningDialog(warning = warning, onDismiss = { explained = null })
    }
}

@Composable
private fun ParentalWarningDialog(warning: ParentalWarning, onDismiss: () -> Unit) {
    val color = warning.severityColor()
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(imageVector = warning.icon(), contentDescription = null, tint = color) },
        title = { Text(warning.label) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                warning.categoryExplanation()?.let { Text(stringResource(it)) }
                warning.severityExplanation()?.let {
                    Text(text = stringResource(it), color = color, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_ok)) }
        },
    )
}

private fun ParentalWarning.categoryExplanation(): StringResource? = when (category) {
    "nudity" -> Res.string.parental_nudity_explanation
    "violence" -> Res.string.parental_violence_explanation
    "profanity" -> Res.string.parental_profanity_explanation
    "alcohol" -> Res.string.parental_alcohol_explanation
    "frightening" -> Res.string.parental_frightening_explanation
    else -> null
}

private fun ParentalWarning.severityExplanation(): StringResource? = when (level) {
    "severe" -> Res.string.parental_severity_severe_explanation
    "moderate" -> Res.string.parental_severity_moderate_explanation
    "mild" -> Res.string.parental_severity_mild_explanation
    else -> null
}

@Composable
private fun ParentalWarning.severityColor(): Color = when (level) {
    "severe" -> MaterialTheme.provenio.colors.danger
    "moderate" -> MaterialTheme.provenio.colors.warning
    "mild" -> MaterialTheme.provenio.colors.success
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun ParentalWarning.icon(): ImageVector = when (category) {
    "nudity" -> Icons.Rounded.NoAdultContent
    "violence" -> Icons.Rounded.SportsMma
    "profanity" -> Icons.Rounded.Explicit
    "alcohol" -> Icons.Rounded.LocalBar
    "frightening" -> Icons.Rounded.MoodBad
    else -> Icons.Rounded.Warning
}
