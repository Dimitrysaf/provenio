package io.github.dimitrysaf.provenio.shell.screens.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explicit
import androidx.compose.material.icons.rounded.LocalBar
import androidx.compose.material.icons.rounded.MoodBad
import androidx.compose.material.icons.rounded.NoAdultContent
import androidx.compose.material.icons.rounded.SportsMma
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.core.playback.ParentalWarning
import io.github.dimitrysaf.provenio.shell.theme.provenio
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.meta_section_parents_guide_title

@OptIn(ExperimentalLayoutApi::class)
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
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            warnings.forEach { warning -> ParentalWarningChip(warning) }
        }
    }
}

@Composable
private fun ParentalWarningChip(warning: ParentalWarning) {
    val color = warning.severityColor()
    Row(
        modifier = Modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = warning.icon(),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = warning.label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = warning.severity,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
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
