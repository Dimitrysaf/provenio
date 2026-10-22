package io.github.dimitrysaf.provenio.shell.screens.profiles

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.shell.components.shapedClickable

/** The colours a profile's letter tile can take, as round swatches, the chosen one ringed and ticked. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ProfileColorPicker(
    selectedHex: String?,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PROFILE_COLORS.forEach { hex ->
            val selected = hex.equals(selectedHex, ignoreCase = true)
            Box(
                modifier = Modifier
                    .size(SwatchSize + 12.dp)
                    .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
                    .padding(6.dp)
                    .shapedClickable(CircleShape, onClickLabel = hex) { onSelected(hex) },
                contentAlignment = Alignment.Center,
            ) {
                Surface(color = parseHexColor(hex), shape = CircleShape, modifier = Modifier.size(SwatchSize)) {}
                if (selected) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = LetterTile.FontColor,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

private val SwatchSize = 40.dp
