package com.nuvio.app.features.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DetailSection(
    title: String,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (showHeader) {
            DetailSectionTitle(title = title)
        }
        content()
    }
}

/**
 * A section's heading.
 *
 * `titleLarge` as published, rather than the same style with its size and weight overwritten: the
 * type scale already answers "how big is a section heading", and it answers it once for the whole
 * app instead of per breakpoint.
 *
 * m3.material.io/styles/typography/type-scale-tokens
 */
@Composable
fun DetailSectionTitle(
    title: String,
    fullWidth: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        modifier = if (fullWidth) modifier.fillMaxWidth() else modifier,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
    )
}