package io.github.dimitrysaf.provenio.shell.screens.details.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
 * The whole block used to be the target, which a paragraph of text does not look like. The text
 * button is the component for an action of this weight, and it is the only thing that moves the
 * state, so nothing is clickable that does not look it.
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
            TextButton(
                onClick = { expanded = !expanded },
                // A text button pads itself out to its own touch target. Shifting it back by
                // that padding keeps the label aligned with the paragraph above it; an offset
                // rather than padding, which cannot be negative.
                modifier = Modifier.offset(x = -TextButtonStartPadding),
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

/** `ButtonDefaults.TextButtonContentPadding` on the start edge. */
private val TextButtonStartPadding = 12.dp
