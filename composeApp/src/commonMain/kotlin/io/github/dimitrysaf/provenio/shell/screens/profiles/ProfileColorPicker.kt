package io.github.dimitrysaf.provenio.shell.screens.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.shell.components.SwatchGrid
import io.github.dimitrysaf.provenio.shell.components.shapedClickable

/** The colours a profile's letter tile can take, as round swatches, the chosen one ringed and ticked. */
@Composable
internal fun ProfileColorPicker(
    selectedHex: String?,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    SwatchGrid(items = PROFILE_COLORS, cell = SwatchCell, modifier = modifier) { hex ->
        val selected = hex.equals(selectedHex, ignoreCase = true)
        Box(
            modifier = Modifier
                .size(SwatchCell)
                .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
                .padding(6.dp)
                .clip(CircleShape)
                .background(parseHexColor(hex))
                .shapedClickable(CircleShape, onClickLabel = hex) { onSelected(hex) },
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = LetterTile.FontColor,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

private val SwatchCell = 52.dp
