package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.geometry.Rect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.compose_search_recent_searches
import provenio.composeapp.generated.resources.compose_search_remove_recent_search

/**
 * The search box for large screens: one rounded container rather than a field over a divider.
 * Clicking into it opens the recent searches under the field as an overlay that continues the
 * same container, without moving the page; picking one searches for it, and the box closes again
 * when it loses focus or the page is clicked.
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
    val density = LocalDensity.current
    var focused by remember { mutableStateOf(false) }
    var fieldSize by remember { mutableStateOf(IntSize.Zero) }
    val showHistory = focused && query.isBlank() && recentSearches.isNotEmpty()
    val containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    // The recent searches are a popup that does not take focus, so the arrow keys move a
    // highlight through them while focus stays in the field, as a combo box does.
    var highlighted by remember { mutableIntStateOf(-1) }
    val activeHighlight = if (showHistory && highlighted <= recentSearches.lastIndex) highlighted else -1
    val scrimToken = remember { Any() }
    var bounds by remember { mutableStateOf<Rect?>(null) }
    DisposableEffect(scrimToken) {
        onDispose { SearchFocusScrim.hide(scrimToken) }
    }
    SideEffect {
        val shown = bounds
        if (focused && shown != null) {
            SearchFocusScrim.show(scrimToken, shown, openBelow = showHistory)
        } else {
            SearchFocusScrim.hide(scrimToken)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { fieldSize = it }
            .onGloballyPositioned { bounds = it.boundsInRoot() }
            .onFocusChanged { focused = it.hasFocus }
            .onPreviewKeyEvent { event ->
                if (!showHistory || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionDown -> {
                        highlighted = (activeHighlight + 1).coerceAtMost(recentSearches.lastIndex)
                        true
                    }
                    Key.DirectionUp -> {
                        if (activeHighlight < 0) return@onPreviewKeyEvent false
                        highlighted = activeHighlight - 1
                        true
                    }
                    Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
                        val picked = recentSearches.getOrNull(activeHighlight) ?: return@onPreviewKeyEvent false
                        highlighted = -1
                        onRecentSearchClick(picked)
                        focusManager.clearFocus()
                        true
                    }
                    Key.Delete, Key.Backspace -> {
                        val picked = recentSearches.getOrNull(activeHighlight) ?: return@onPreviewKeyEvent false
                        onRemoveRecentSearch(picked)
                        true
                    }
                    else -> false
                }
            },
    ) {
        Surface(
            shape = if (showHistory) {
                RoundedCornerShape(topStart = SearchBoxCorner, topEnd = SearchBoxCorner)
            } else {
                RoundedCornerShape(SearchBoxCorner)
            },
            color = containerColor,
            shadowElevation = if (showHistory) 6.dp else 0.dp,
        ) {
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
        }
        if (showHistory) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, fieldSize.height),
                onDismissRequest = { focusManager.clearFocus() },
                properties = PopupProperties(focusable = false),
            ) {
                val visible = remember { MutableTransitionState(false).apply { targetState = true } }
                AnimatedVisibility(
                    visibleState = visible,
                    enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                ) {
                    Surface(
                        modifier = Modifier.width(with(density) { fieldSize.width.toDp() }),
                        shape = RoundedCornerShape(bottomStart = SearchBoxCorner, bottomEnd = SearchBoxCorner),
                        color = containerColor,
                        shadowElevation = 6.dp,
                    ) {
                        Column(modifier = Modifier.padding(bottom = 8.dp)) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            Text(
                                text = stringResource(Res.string.compose_search_recent_searches),
                                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            recentSearches.forEachIndexed { index, recentQuery ->
                                SearchBoxRecentRow(
                                    query = recentQuery,
                                    highlighted = index == activeHighlight,
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
    }
}

@Composable
private fun SearchBoxRecentRow(
    query: String,
    highlighted: Boolean,
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
        colors = ListItemDefaults.colors(
            containerColor = if (highlighted) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        ),
    )
}

private val SearchBoxCorner = 28.dp
