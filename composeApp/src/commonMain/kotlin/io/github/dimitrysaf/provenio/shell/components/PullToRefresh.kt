package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import io.github.dimitrysaf.provenio.core.build.isDesktop
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/**
 * A list that refreshes when pulled down past its top, when [onRefresh] is given, and is only the
 * list otherwise. The indicator comes down from [indicatorTop], under the app bar rather than
 * behind it, and stays while [refreshing].
 *
 * A mouse wheel has no release for a pull to wait for, so on desktop the list refreshes when it is
 * scrolled up again once it already rests at its top.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun PullToRefreshContainer(
    refreshing: Boolean,
    onRefresh: (() -> Unit)?,
    indicatorTop: Dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    if (onRefresh == null) {
        Box(modifier = modifier, content = content)
        return
    }
    val state = rememberPullToRefreshState()
    Box(
        modifier = modifier
            .pullToRefresh(isRefreshing = refreshing, state = state, enabled = !isDesktop, onRefresh = onRefresh)
            .then(if (isDesktop) Modifier.nestedScroll(rememberScrollPastTopRefresh(refreshing, onRefresh)) else Modifier),
    ) {
        content()
        PullToRefreshDefaults.LoadingIndicator(
            state = state,
            isRefreshing = refreshing,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = indicatorTop),
        )
    }
}

/**
 * Calls [onRefresh] for a scroll up that starts while the list is already at its top. The scroll
 * that brings the list there does not count, even as it carries on past the top.
 */
@Composable
private fun rememberScrollPastTopRefresh(refreshing: Boolean, onRefresh: () -> Unit): NestedScrollConnection {
    val isRefreshing by rememberUpdatedState(refreshing)
    val refresh by rememberUpdatedState(onRefresh)
    return remember {
        object : NestedScrollConnection {
            private var lastScroll: TimeSource.Monotonic.ValueTimeMark? = null
            private var gestureMovedList = false
            private var gestureRefreshed = false

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                val startsGesture = lastScroll?.let { it.elapsedNow() > ScrollGestureGap } ?: true
                lastScroll = TimeSource.Monotonic.markNow()
                if (startsGesture) {
                    gestureMovedList = false
                    gestureRefreshed = false
                }
                if (consumed.y != 0f) gestureMovedList = true
                val pastTop = available.y > 0f && consumed.y == 0f
                if (pastTop && !gestureMovedList && !gestureRefreshed && !isRefreshing) {
                    gestureRefreshed = true
                    refresh()
                }
                return Offset.Zero
            }
        }
    }
}

// A pause in the wheel longer than this ends one scroll, so the next one starts afresh.
private val ScrollGestureGap = 300.milliseconds
