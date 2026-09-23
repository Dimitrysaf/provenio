package com.nuvio.app.shell.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_back
import org.jetbrains.compose.resources.stringResource

/**
 * A screen hosting a large flexible top app bar that collapses as its content scrolls.
 *
 * The baseline medium and large app bars are no longer recommended in M3 Expressive; the flexible
 * ones replace them, and they hug their text so a subtitle simply makes the bar taller. The
 * container colour shifts from `surface` to `surface container` on scroll, which
 * `TopAppBarDefaults.topAppBarColors` already handles.
 *
 * m3.material.io/components/app-bars/specs
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NuvioScreen(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: (@Composable () -> Unit)? = null,
    horizontalPadding: Dp = 16.dp,
    topPadding: Dp? = null,
    listState: LazyListState = rememberLazyListState(),
    content: LazyListScope.() -> Unit,
) {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // A null title means the screen supplies its own heading — Home's hero, for one — so no app
    // bar is placed and the content keeps the full height.
    //
    // The scroll behaviour belongs to the app bar, so a screen without one does not get it. Left
    // attached, its connection eats every scroll the content reports: `rememberTopAppBarState`
    // starts with a height-offset limit of -Float.MAX_VALUE, and it is the app bar that narrows
    // that to its own height when it measures. With no app bar to do so, the phantom bar can
    // "collapse" forever, so the connection consumes the whole gesture and the list never moves.
    val scrollBehavior = if (title != null) {
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    } else {
        null
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .then(
                scrollBehavior
                    ?.let { Modifier.nestedScroll(it.nestedScrollConnection) }
                    ?: Modifier,
            ),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            if (title == null) return@Scaffold
            LargeFlexibleTopAppBar(
                title = { Text(title) },
                subtitle = subtitle?.let { { Text(it) } },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = stringResource(Res.string.action_back),
                            )
                        }
                    }
                },
                actions = actions,
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = { bottomBar?.invoke() },
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = horizontalPadding,
                top = topPadding
                    ?: if (title != null) {
                        innerPadding.calculateTopPadding()
                    } else {
                        10.dp + statusBarTop + nuvioPlatformExtraTopPadding
                    },
                end = horizontalPadding,
                // A bottom bar already reserves its own height plus the navigation bar inset in
                // the scaffold's inner padding, so the content only adds the screen's own gap
                // above it.
                bottom = if (bottomBar != null) {
                    innerPadding.calculateBottomPadding() + 18.dp
                } else {
                    nuvioSafeBottomPadding(18.dp)
                },
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

internal fun Modifier.nuvioConsumePointerEvents(): Modifier =
    pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                awaitPointerEvent(PointerEventPass.Final).changes.forEach { change ->
                    change.consume()
                }
            }
        }
    }
