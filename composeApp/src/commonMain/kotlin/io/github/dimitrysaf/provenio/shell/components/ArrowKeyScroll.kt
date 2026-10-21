package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.core.settings.ThemeSettingsRepository
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * The scrolling content of a page or a sheet, for a remote or a keyboard. Up and down move focus
 * as usual. Where there is nothing to focus, as on a page of only text, focus goes to the
 * content's scrollbar instead, which up and down then scroll, so none of it is out of reach; left
 * leaves it for the content. Past the last item of a page that has some, they scroll the rest
 * of it without moving focus off that item.
 *
 * With the scrollbar setting on, three quick presses of right reach the scrollbar on any page.
 */
@Stable
internal class ArrowKeyScroll {
    internal var state: ScrollableState? = null
    internal var viewportHeight = 0
    internal var running: Job? = null

    internal val scrollbar = FocusRequester()

    // The content takes focus itself, as its scrollbar, only once that is asked for: otherwise
    // the arrow keys would find it rather than the items in it.
    internal var scrollbarFocusable by mutableStateOf(false)
    internal var scrollbarFocused by mutableStateOf(false)
    internal var contentHasFocus = false
    internal var hasContent = false

    private var rightPresses = 0
    private var lastRightAt = TimeSource.Monotonic.markNow()

    internal fun focusScrollbar(): Boolean {
        if (!hasContent) return false
        scrollbarFocusable = true
        if (scrollbar.requestFocus(FocusDirection.Enter)) return true
        scrollbarFocusable = false
        return false
    }

    internal fun scrollBy(scope: CoroutineScope, forward: Boolean): Boolean {
        val state = state ?: return false
        if (if (forward) !state.canScrollForward else !state.canScrollBackward) return false
        val step = viewportHeight * ArrowKeyScrollFraction
        running?.cancel()
        running = scope.launch { state.animateScrollBy(if (forward) step else -step) }
        return true
    }

    /** Whether this press of right is the third in quick succession. */
    internal fun thirdQuickRight(): Boolean {
        rightPresses = if (lastRightAt.elapsedNow() < QuickPressGap) rightPresses + 1 else 1
        lastRightAt = TimeSource.Monotonic.markNow()
        if (rightPresses < 3) return false
        rightPresses = 0
        return true
    }
}

internal val LocalArrowKeyScroll = staticCompositionLocalOf<ArrowKeyScroll?> { null }

@Composable
internal fun rememberArrowKeyScroll(): ArrowKeyScroll = remember { ArrowKeyScroll() }

/**
 * Makes [content] a sheet whose scrolling content, marked with [arrowKeyScrollContent], the arrow
 * keys scroll. It is given the modifier to put on the container holding both that content and
 * anything focus can be on around it, such as the header.
 */
@Composable
internal fun ArrowKeyScrollHost(content: @Composable (Modifier) -> Unit) {
    val scroll = rememberArrowKeyScroll()
    CompositionLocalProvider(LocalArrowKeyScroll provides scroll) {
        content(Modifier.arrowKeyScrollHost(scroll))
    }
}

/**
 * For the container of a page or a dialog: the container, rather than only the content, so the
 * keys count while focus is on the app bar or the buttons around it too.
 */
internal fun Modifier.arrowKeyScrollHost(scroll: ArrowKeyScroll): Modifier = composed {
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    Modifier
        .onPreviewKeyEvent { event ->
            if (event.key != Key.DirectionRight || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            if (!ThemeSettingsRepository.scrollbarFocus.value || scroll.scrollbarFocused) return@onPreviewKeyEvent false
            val state = scroll.state ?: return@onPreviewKeyEvent false
            if (!state.canScrollForward && !state.canScrollBackward) return@onPreviewKeyEvent false
            scroll.thirdQuickRight() && scroll.focusScrollbar()
        }
        .onKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
            val forward = when (event.key) {
                Key.DirectionDown -> true
                Key.DirectionUp -> false
                else -> return@onKeyEvent false
            }
            if (scroll.state == null) return@onKeyEvent false
            if (focusManager.moveFocus(if (forward) FocusDirection.Down else FocusDirection.Up)) {
                return@onKeyEvent true
            }
            // On an item of the content, the content has things to focus, and focus stays on it.
            if (!scroll.contentHasFocus && !scroll.scrollbarFocused && scroll.scrollBy(scope, forward)) {
                scroll.focusScrollbar()
                return@onKeyEvent true
            }
            scroll.scrollBy(scope, forward)
        }
}

/** Marks the scrolling content of the [ArrowKeyScrollHost] it is in. */
internal fun Modifier.arrowKeyScrollContent(state: ScrollableState): Modifier = composed {
    val scroll = LocalArrowKeyScroll.current
    if (scroll == null) Modifier else Modifier.arrowKeyScrollContent(scroll, state)
}

/** Marks the content of a page or a dialog that scrolls with [state], and draws its scrollbar. */
internal fun Modifier.arrowKeyScrollContent(scroll: ArrowKeyScroll, state: ScrollableState): Modifier = composed {
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.24f)
    val thumbColor = MaterialTheme.colorScheme.secondary
    DisposableEffect(scroll, state) {
        scroll.state = state
        scroll.hasContent = true
        onDispose {
            if (scroll.state === state) {
                scroll.state = null
                scroll.hasContent = false
            }
        }
    }
    Modifier
        .onSizeChanged { scroll.viewportHeight = it.height }
        .drawWithContent {
            drawContent()
            if (!scroll.scrollbarFocused) return@drawWithContent
            val (start, length) = state.scrollbarThumb() ?: (0f to 1f)
            val width = ScrollbarWidth.toPx()
            val inset = ScrollbarInset.toPx()
            val ring = ScrollbarRing.toPx()
            val x = size.width - width - inset - ring
            val top = inset + ring
            val height = size.height - 2 * (inset + ring)
            val radius = CornerRadius(width / 2)
            drawRoundRect(trackColor, Offset(x, top), Size(width, height), radius)
            drawRoundRect(thumbColor, Offset(x, top + start * height), Size(width, length * height), radius)
            drawRoundRect(
                color = thumbColor,
                topLeft = Offset(x - ring, top - ring),
                size = Size(width + 2 * ring, height + 2 * ring),
                cornerRadius = CornerRadius(width / 2 + ring),
                style = Stroke(width = ring),
            )
        }
        .focusProperties { canFocus = scroll.scrollbarFocusable }
        .focusRequester(scroll.scrollbar)
        .onFocusChanged {
            scroll.scrollbarFocused = it.isFocused
            scroll.contentHasFocus = it.hasFocus && !it.isFocused
            if (!it.isFocused) scroll.scrollbarFocusable = false
        }
        .onKeyEvent { event ->
            if (!scroll.scrollbarFocused || event.type != KeyEventType.KeyDown) return@onKeyEvent false
            when (event.key) {
                Key.DirectionDown -> scroll.scrollBy(scope, forward = true)
                Key.DirectionUp -> scroll.scrollBy(scope, forward = false)
                // Back to the items shown, if there are any.
                Key.DirectionLeft -> focusManager.moveFocus(FocusDirection.Enter)
                else -> false
            }
        }
        .focusTarget()
}

/** Where the thumb starts and how long it is, as parts of the track; null if it cannot be told. */
private fun ScrollableState.scrollbarThumb(): Pair<Float, Float>? = when (this) {
    is ScrollState -> {
        val total = (maxValue + viewportSize).toFloat()
        if (maxValue == 0 || total <= 0f) null else (value / total) to (viewportSize / total)
    }
    is LazyListState -> {
        val info = layoutInfo
        val visible = info.visibleItemsInfo
        val viewport = (info.viewportEndOffset - info.viewportStartOffset).toFloat()
        if (visible.isEmpty() || viewport <= 0f) {
            null
        } else {
            val itemSize = visible.sumOf { it.size }.toFloat() / visible.size + info.mainAxisItemSpacing
            val total = itemSize * info.totalItemsCount
            if (total <= viewport) {
                null
            } else {
                val offset = firstVisibleItemIndex * itemSize + firstVisibleItemScrollOffset
                val length = (viewport / total).coerceIn(MinThumbFraction, 1f)
                (offset / total).coerceIn(0f, 1f - length) to length
            }
        }
    }
    else -> null
}

/** How much of the visible height one press scrolls. */
private const val ArrowKeyScrollFraction = 0.4f

/** How soon each of the three presses of right has to follow the one before. */
private val QuickPressGap = 300.milliseconds

private const val MinThumbFraction = 0.05f
private val ScrollbarWidth = 6.dp
private val ScrollbarInset = 4.dp
private val ScrollbarRing = 2.dp
