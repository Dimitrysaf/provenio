package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.compose_search_recent_searches
import provenio.composeapp.generated.resources.compose_search_remove_recent_search

/**
 * The search box for large screens: one rounded container rather than a field over a divider.
 * Clicking into it opens the recent searches inside the same container, under the field; picking
 * one searches for it, and the box closes again when it loses focus.
 */
@Composable
fun SearchBox(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    recentSearches: List<String>,
    onRecentSearchClick: (String) -> Unit,
    onRemoveRecentSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onSearch: (String) -> Unit = {},
    actions: (@Composable () -> Unit)? = null,
) {
    val focusManager = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }
    val showHistory = focused && query.isBlank() && recentSearches.isNotEmpty()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.hasFocus },
        shape = RoundedCornerShape(SearchBoxCorner),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column {
            SearchField(
                query = query,
                onQueryChange = onQueryChange,
                placeholder = placeholder,
                focusRequester = focusRequester,
                onSearch = { submitted ->
                    onSearch(submitted)
                    focusManager.clearFocus()
                },
                actions = actions,
            )
            AnimatedVisibility(
                visible = showHistory,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    Text(
                        text = stringResource(Res.string.compose_search_recent_searches),
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    recentSearches.forEach { recentQuery ->
                        SearchBoxRecentRow(
                            query = recentQuery,
                            onClick = {
                                onRecentSearchClick(recentQuery)
                                focusManager.clearFocus()
                            },
                            onRemove = { onRemoveRecentSearch(recentQuery) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBoxRecentRow(
    query: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    val removeLabel = stringResource(Res.string.compose_search_remove_recent_search)
    ListItem(
        headlineContent = {
            Text(text = query, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        leadingContent = {
            Icon(imageVector = Icons.Rounded.History, contentDescription = null)
        },
        trailingContent = {
            WithTooltip(removeLabel) {
                IconButton(onClick = onRemove) {
                    Icon(imageVector = Icons.Rounded.Close, contentDescription = removeLabel)
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

private val SearchBoxCorner = 28.dp
