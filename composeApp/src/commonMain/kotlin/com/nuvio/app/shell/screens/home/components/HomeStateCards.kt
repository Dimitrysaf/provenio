package com.nuvio.app.shell.screens.home.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.nuvio.app.shell.components.EmptyState

/**
 * A catalog with nothing in it, or one that failed to load.
 *
 * The app has one empty state, so this is it with a catalog's words in it.
 */
@Composable
fun HomeEmptyStateCard(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.GridView,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    EmptyState(
        icon = icon,
        title = title,
        message = message,
        modifier = modifier,
        actionLabel = actionLabel,
        onActionClick = onActionClick,
    )
}
