package com.nuvio.app.shell.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The heading above a list group.
 *
 * Every value is a Material one: the `title small` role for the text and the `on surface variant`
 * colour the spec gives a subheader, so it reads as a label for what follows rather than
 * competing with it. Nothing here transforms the text, so a heading is capitalised the way its
 * translation writes it.
 *
 * m3.material.io/components/lists/specs
 */
@Composable
fun ListSubheader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
