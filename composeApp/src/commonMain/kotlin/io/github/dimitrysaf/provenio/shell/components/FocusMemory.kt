package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.currentCompositeKeyHashCode
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * Where focus has been, for a remote or a keyboard. When focus is lost, because the menu, sheet
 * or page it was on has closed, or because the item it was on went away, it goes back to the
 * item focused most recently of those still shown: the one the menu was opened from, or the one
 * the page was left from. It does so even when something else took focus in the meantime, as the
 * rail does whenever the focused item goes. With touch nothing is focused, so nothing goes back.
 *
 * An item is known by where it is composed, which stays the same when its page is composed again
 * on the way back to it. After a page, only the page come back to counts, the items shown again
 * about when focus was lost: going on to a new page loses focus too, and that is not for an item
 * left behind beside it to take. While a sheet is open, focus is its own, and goes back once it
 * closes.
 */
internal object FocusMemory {
    /** The items focused, the most recent last. */
    private val history = LinkedHashSet<Long>()

    /** The items shown now that focus can go back to. */
    private val shown = mutableMapOf<Long, Shown>()

    /** The item focused now, if it is one of [shown]. */
    private var focusedKey: Long? = null

    /** When focus was lost, if it has not gone back yet. */
    private var lostAt: TimeSource.Monotonic.ValueTimeMark? = null

    private var sheetClosedAt: TimeSource.Monotonic.ValueTimeMark? = null

    internal var appHasFocus = false

    /** Counts the times focus was lost or a sheet closed, which the app's root answers. */
    internal var restoreRequests by mutableIntStateOf(0)

    /** A menu or a sheet closed: focus goes back to what it was opened from. */
    internal fun sheetClosed() {
        sheetClosedAt = TimeSource.Monotonic.markNow()
        restoreRequests++
    }

    internal fun lost() {
        if (lostAt == null) lostAt = TimeSource.Monotonic.markNow()
        restoreRequests++
    }

    internal fun focused(key: Long) {
        focusedKey = key
        history.remove(key)
        history.add(key)
        if (history.size > HistoryLimit) history.remove(history.first())
    }

    internal fun unfocused(key: Long) {
        if (focusedKey == key) focusedKey = null
    }

    internal fun shown(key: Long, requester: FocusRequester) {
        shown[key] = Shown(requester, TimeSource.Monotonic.markNow())
    }

    internal fun hidden(key: Long, requester: FocusRequester) {
        if (shown[key]?.requester !== requester) return
        shown.remove(key)
        if (focusedKey == key) {
            focusedKey = null
            lost()
        }
    }

    /** Whether focus has somewhere to go back to: it was lost, or a sheet closed with it nowhere. */
    internal fun wantsRestore(): Boolean = lostAt != null || (!appHasFocus && sheetClosedRecently())

    /** Focuses the item focused most recently of those shown; false if none can be. */
    internal fun restore(): Boolean {
        val lost = lostAt
        val afterSheet = sheetClosedRecently()
        for (key in history.reversed()) {
            val item = shown[key] ?: continue
            // Shown again around when focus was lost, as a page come back to is.
            val shownAgain = lost != null && item.since.elapsedNow() < lost.elapsedNow() + RecentWindow
            if (!afterSheet && !shownAgain) continue
            // An item can be gone from the screen before it is told; asking it for focus then throws.
            val focused = key == focusedKey ||
                runCatching { item.requester.requestFocus(FocusDirection.Enter) }.getOrDefault(false)
            if (focused) {
                lostAt = null
                return true
            }
        }
        return false
    }

    /** Stops trying: nothing shown is to have focus back. */
    internal fun giveUp() {
        lostAt = null
    }

    private fun sheetClosedRecently(): Boolean = sheetClosedAt?.let { it.elapsedNow() < RecentWindow } == true

    private class Shown(val requester: FocusRequester, val since: TimeSource.Monotonic.ValueTimeMark)
}

/** For an item focus can go back to; put it before the item's clickable. */
fun Modifier.restoresFocus(): Modifier = composed {
    val key = currentCompositeKeyHashCode
    val requester = remember { FocusRequester() }
    DisposableEffect(key, requester) {
        FocusMemory.shown(key, requester)
        onDispose { FocusMemory.hidden(key, requester) }
    }
    Modifier
        .focusRequester(requester)
        .onFocusChanged { if (it.isFocused) FocusMemory.focused(key) else FocusMemory.unfocused(key) }
}

/**
 * For the app's root: notices focus being lost and sends it back, keeps a focused item in the
 * middle of what scrolls rather than at its edge, and makes a held key one press, see
 * [KeyPresses].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FocusMemoryRoot(content: @Composable (Modifier) -> Unit) {
    val inputModeManager = LocalInputModeManager.current
    val defaultSpec = LocalBringIntoViewSpec.current
    val centredSpec = remember(defaultSpec, inputModeManager) { CentredBringIntoViewSpec(defaultSpec, inputModeManager) }
    val rootModifier = remember {
        Modifier.singleKeyPresses().onFocusChanged {
            val hadFocus = FocusMemory.appHasFocus
            FocusMemory.appHasFocus = it.hasFocus
            if (hadFocus && !it.hasFocus) FocusMemory.lost()
        }
    }
    FocusRestoreEffect(inputModeManager)
    CompositionLocalProvider(LocalBringIntoViewSpec provides centredSpec) {
        content(rootModifier)
    }
}

/** Its own scope, so a request recomposes only this and not the app under it. */
@Composable
private fun FocusRestoreEffect(inputModeManager: InputModeManager) {
    val requests = FocusMemory.restoreRequests
    LaunchedEffect(requests) {
        if (requests == 0) return@LaunchedEffect
        // The page being gone back to can take a few frames to be composed and placed.
        repeat(RestoreFrames) {
            withFrameNanos {}
            if (inputModeManager.inputMode != InputMode.Keyboard) {
                FocusMemory.giveUp()
                return@LaunchedEffect
            }
            // A sheet open now has focus; its closing asks again.
            if (OpenSheets.count > 0 || !FocusMemory.wantsRestore()) return@LaunchedEffect
            if (FocusMemory.restore()) return@LaunchedEffect
        }
        FocusMemory.giveUp()
    }
}

/** With keys, scrolls a focused item to the middle; with touch, as [default] does. */
@OptIn(ExperimentalFoundationApi::class)
private class CentredBringIntoViewSpec(
    private val default: BringIntoViewSpec,
    private val inputModeManager: InputModeManager,
) : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        if (inputModeManager.inputMode != InputMode.Keyboard) {
            return default.calculateScrollDistance(offset, size, containerSize)
        }
        // Too big to be centred, it starts at the edge instead.
        if (size >= containerSize) return offset
        return offset + size / 2f - containerSize / 2f
    }
}

private const val HistoryLimit = 128

/** Long enough for a page's way back in, or a sheet's way out, to have finished. */
private val RecentWindow = 2.seconds

/** About half a second at 60 frames a second. */
private const val RestoreFrames = 30
