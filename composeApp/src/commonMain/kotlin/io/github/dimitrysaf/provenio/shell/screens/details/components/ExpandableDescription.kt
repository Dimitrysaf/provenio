package io.github.dimitrysaf.provenio.shell.screens.details.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.FilledTonalButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.details_show_less
import provenio.composeapp.generated.resources.details_show_more
import org.jetbrains.compose.resources.stringResource

/**
 * A description that opens, with a button that says so.
 *
 * The whole block used to be the target, which a paragraph of text does not look like. A
 * full-width tonal button under the text is the only thing that moves the state, so nothing is
 * clickable that does not look it.
 *
 * m3.material.io/components/buttons/specs
 */
@Composable
internal fun ExpandableDescription(
    text: String,
    collapsedMaxLines: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    var expanded by remember(text) { mutableStateOf(false) }
    var canExpand by remember(text) { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
    ) {
        Text(
            text = text,
            style = style,
            color = color,
            maxLines = if (expanded) Int.MAX_VALUE else collapsedMaxLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result ->
                if (!expanded) {
                    canExpand = result.hasVisualOverflow
                }
            },
        )
        if (canExpand) {
            FilledTonalButton(
                onClick = { expanded = !expanded },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                Text(
                    text = if (expanded) {
                        stringResource(Res.string.details_show_less)
                    } else {
                        stringResource(Res.string.details_show_more)
                    },
                )
            }
        }
    }
}
