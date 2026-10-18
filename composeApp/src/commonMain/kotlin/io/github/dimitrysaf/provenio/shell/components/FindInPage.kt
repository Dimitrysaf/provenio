package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.action_close
import provenio.composeapp.generated.resources.find_in_page_count
import provenio.composeapp.generated.resources.find_in_page_hint
import provenio.composeapp.generated.resources.find_in_page_next
import provenio.composeapp.generated.resources.find_in_page_previous

/** One place on the page where the searched words show, and how to scroll to it. */
internal class FindMatch(
    val key: Any,
    val position: () -> Offset?,
    val reveal: suspend () -> Unit,
)

private data class FindMatchKey(val owner: Any, val index: Int)

/**
 * Find in page: Ctrl+F opens a bar that highlights the words typed into it wherever the page's
 * text shows them, and steps through each place from top to bottom.
 */
object FindInPage {
    var isOpen by mutableStateOf(false)
        private set

    var query by mutableStateOf("")

    internal var currentKey by mutableStateOf<Any?>(null)
        private set

    internal var focusRequests by mutableIntStateOf(0)
        private set

    private val matches = mutableStateMapOf<Any, FindMatch>()

    val matchCount: Int
        get() = matches.size

    /** Where the highlighted match sits among the matches on screen, from 1, or 0 for none. */
    val currentPosition: Int
        get() = ordered().indexOfFirst { it.key == currentKey } + 1

    /** Opens the bar, or brings the typing back to it when it is open already. */
    fun open() {
        isOpen = true
        focusRequests++
    }

    fun close() {
        isOpen = false
        query = ""
        currentKey = null
    }

    internal fun register(match: FindMatch) {
        matches[match.key] = match
    }

    internal fun unregister(key: Any) {
        matches.remove(key)
        if (currentKey == key) currentKey = null
    }

    /** Moves to the next match, or the previous one, wrapping at the ends; null when there are none. */
    internal fun step(forward: Boolean): FindMatch? {
        val list = ordered()
        if (list.isEmpty()) return null
        val index = list.indexOfFirst { it.key == currentKey }
        val next = when {
            index < 0 -> if (forward) 0 else list.lastIndex
            forward -> (index + 1) % list.size
            else -> (index - 1 + list.size) % list.size
        }
        currentKey = list[next].key
        return list[next]
    }

    /** Keeps a match highlighted: the first on the page when the one before is gone. */
    internal fun ensureCurrent(): FindMatch? {
        val list = ordered()
        if (list.any { it.key == currentKey }) return null
        val first = list.firstOrNull()
        currentKey = first?.key
        return first
    }

    internal fun isCurrent(key: Any): Boolean = currentKey == key

    private fun ordered(): List<FindMatch> = matches.values
        .mapNotNull { match -> match.position()?.let { match to it } }
        .sortedWith(compareBy({ it.second.y }, { it.second.x }))
        .map { it.first }
}

/** Where [query] shows in [text], ignoring case, as index ranges. */
internal fun findRanges(text: String, query: String): List<IntRange> {
    if (query.isBlank() || text.isEmpty()) return emptyList()
    val ranges = mutableListOf<IntRange>()
    var start = text.indexOf(query, ignoreCase = true)
    while (start >= 0) {
        ranges += start until start + query.length
        start = text.indexOf(query, startIndex = start + query.length, ignoreCase = true)
    }
    return ranges
}

/** Whether [text] holds what the find bar is looking for, so a collapsed block can open to show it. */
@Composable
fun rememberFindHit(text: String): Boolean {
    val query = if (FindInPage.isOpen) FindInPage.query.trim() else ""
    return remember(text, query) { findRanges(text, query).isNotEmpty() }
}

/**
 * Text the find bar can search: it draws like [Text], and while the bar is open it highlights
 * each place the searched words show, the current one more strongly, and can be scrolled to.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FindableText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current,
) {
    val query = if (FindInPage.isOpen) FindInPage.query.trim() else ""
    val ranges = remember(text, query) { findRanges(text, query) }
    if (ranges.isEmpty()) {
        Text(
            text = text,
            modifier = modifier,
            color = color,
            fontSize = fontSize,
            fontStyle = fontStyle,
            fontWeight = fontWeight,
            letterSpacing = letterSpacing,
            textDecoration = textDecoration,
            textAlign = textAlign,
            lineHeight = lineHeight,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            minLines = minLines,
            onTextLayout = onTextLayout,
            style = style,
        )
        return
    }

    val owner = remember { Any() }
    val requester = remember { BringIntoViewRequester() }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val keys = remember(ranges) { ranges.indices.map { FindMatchKey(owner, it) } }

    DisposableEffect(owner, ranges) {
        ranges.forEachIndexed { index, range ->
            FindInPage.register(
                FindMatch(
                    key = keys[index],
                    position = position@{
                        val placed = coordinates?.takeIf { it.isAttached } ?: return@position null
                        val box = layout?.boxOf(range) ?: return@position null
                        placed.localToRoot(box.topLeft)
                    },
                    reveal = { requester.bringIntoView(layout?.boxOf(range)?.inflate(48f)) },
                ),
            )
        }
        onDispose { keys.forEach(FindInPage::unregister) }
    }

    val highlight = MaterialTheme.colorScheme.tertiaryContainer
    val onHighlight = MaterialTheme.colorScheme.onTertiaryContainer
    val current = MaterialTheme.colorScheme.primary
    val onCurrent = MaterialTheme.colorScheme.onPrimary
    val annotated = buildAnnotatedString {
        append(text)
        ranges.forEachIndexed { index, range ->
            val isCurrent = FindInPage.isCurrent(keys[index])
            addStyle(
                SpanStyle(
                    background = if (isCurrent) current else highlight,
                    color = if (isCurrent) onCurrent else onHighlight,
                ),
                start = range.first,
                end = range.last + 1,
            )
        }
    }
    Text(
        text = annotated,
        modifier = modifier
            .bringIntoViewRequester(requester)
            .onGloballyPositioned { coordinates = it },
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = { result ->
            layout = result
            onTextLayout?.invoke(result)
        },
        style = style,
    )
}

/** The box of [range]'s first character, or null when it is cut off and not drawn. */
private fun TextLayoutResult.boxOf(range: IntRange): Rect? {
    val offset = range.first
    if (offset >= layoutInput.text.length) return null
    val line = runCatching { getLineForOffset(offset) }.getOrNull() ?: return null
    if (line >= lineCount || isLineEllipsized(line) && offset > getLineEnd(line, visibleEnd = true)) return null
    return runCatching { getBoundingBox(offset) }.getOrNull()
}

/**
 * The find bar, at the top of the window while find in page is open: the words to find, how many
 * places show them, and buttons for the previous and next one. Enter goes to the next place,
 * Shift+Enter to the one before, and Escape closes it.
 */
@Composable
fun FindInPageBar(modifier: Modifier = Modifier) {
    if (!FindInPage.isOpen) return
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val count = FindInPage.matchCount
    val query = FindInPage.query

    LaunchedEffect(FindInPage.focusRequests) {
        runCatching { focusRequester.requestFocus() }
    }
    LaunchedEffect(query, count) {
        delay(FindLayoutSettleMillis)
        FindInPage.ensureCurrent()?.let { it.reveal() }
    }

    fun step(forward: Boolean) {
        FindInPage.step(forward)?.let { match -> scope.launch { match.reveal() } }
    }

    val previousLabel = stringResource(Res.string.find_in_page_previous)
    val nextLabel = stringResource(Res.string.find_in_page_next)
    val closeLabel = stringResource(Res.string.action_close)
    Box(
        modifier = modifier.windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter,
    ) {
        Surface(
            modifier = Modifier
                .padding(top = 12.dp, start = 16.dp, end = 16.dp)
                .widthIn(max = 520.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 3.dp,
            shadowElevation = 6.dp,
        ) {
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.find_in_page_hint),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { FindInPage.query = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { step(forward = true) }),
                        modifier = Modifier
                            .focusRequester(focusRequester)
                            .onPreviewKeyEvent { event ->
                                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                when (event.key) {
                                    Key.Escape -> {
                                        FindInPage.close()
                                        true
                                    }
                                    Key.Enter, Key.NumPadEnter -> {
                                        step(forward = !event.isShiftPressed)
                                        true
                                    }
                                    else -> false
                                }
                            },
                    )
                }
                if (query.isNotBlank()) {
                    Text(
                        text = stringResource(Res.string.find_in_page_count, FindInPage.currentPosition, count),
                        style = MaterialTheme.typography.labelLarge,
                        color = LocalContentColor.current.copy(alpha = 0.8f),
                        maxLines = 1,
                    )
                }
                WithTooltip(previousLabel) {
                    IconButton(onClick = { step(forward = false) }, enabled = count > 0) {
                        Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = previousLabel)
                    }
                }
                WithTooltip(nextLabel) {
                    IconButton(onClick = { step(forward = true) }, enabled = count > 0) {
                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = nextLabel)
                    }
                }
                WithTooltip(closeLabel) {
                    IconButton(onClick = FindInPage::close) {
                        Icon(Icons.Rounded.Close, contentDescription = closeLabel)
                    }
                }
            }
        }
    }
}

private const val FindLayoutSettleMillis = 80L
