package io.github.dimitrysaf.provenio.shell.screens.details.components

import io.github.dimitrysaf.provenio.shell.components.FindableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explicit
import androidx.compose.material.icons.rounded.LocalBar
import androidx.compose.material.icons.rounded.MoodBad
import androidx.compose.material.icons.rounded.NoAdultContent
import androidx.compose.material.icons.rounded.SportsMma
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import io.github.dimitrysaf.provenio.core.playback.ParentalWarning
import io.github.dimitrysaf.provenio.shell.theme.provenio
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.meta_section_parents_guide_title

@Composable
fun DetailParentsGuideSection(
    warnings: List<ParentalWarning>,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
) {
    if (warnings.isEmpty()) return

    DetailSection(
        title = stringResource(Res.string.meta_section_parents_guide_title),
        modifier = modifier,
        showHeader = showHeader,
    ) {
        DetailSegmentedColumn {
            warnings.forEachIndexed { index, warning ->
                val color = warning.severityColor()
                DetailStaticSegment(
                    index = index,
                    count = warnings.size,
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
