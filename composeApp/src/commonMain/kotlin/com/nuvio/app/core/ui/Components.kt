package com.nuvio.app.core.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_back
import nuvio.composeapp.generated.resources.action_ok
import org.jetbrains.compose.resources.stringResource
import com.nuvio.app.navigation.LocalNativeNavigationBarHidden
import com.nuvio.app.navigation.LocalUseNativeNavigation

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
    horizontalPadding: Dp = MaterialTheme.nuvio.spacing.screenHorizontal,
    topPadding: Dp? = null,
    listState: LazyListState = rememberLazyListState(),
    content: LazyListScope.() -> Unit,
) {
    val tokens = MaterialTheme.nuvio
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
                        tokens.spacing.screenTop + statusBarTop + nuvioPlatformExtraTopPadding
                    },
                end = horizontalPadding,
                // A bottom bar already reserves its own height plus the navigation bar inset in
                // the scaffold's inner padding, so the content only adds the screen's own gap
                // above it.
                bottom = if (bottomBar != null) {
                    innerPadding.calculateBottomPadding() + tokens.spacing.screenBottom
                } else {
                    nuvioSafeBottomPadding(tokens.spacing.screenBottom)
                },
            ),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.listGap),
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

@Composable
fun NuvioSurfaceCard(
    modifier: Modifier = Modifier,
    tonalElevation: Int = 0,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = tokens.colors.surface,
        shape = tokens.shapes.card,
        tonalElevation = tonalElevation.dp,
        shadowElevation = tokens.elevation.flat,
    ) {
        Column(
            modifier = Modifier.padding(tokens.spacing.cardPadding),
            content = content,
        )
    }
}

@Composable
fun NuvioScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    includeStatusBarPadding: Boolean = true,
    topPadding: Dp? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val tokens = MaterialTheme.nuvio
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val nativeDetailNavigation = LocalUseNativeNavigation.current &&
        !LocalNativeNavigationBarHidden.current &&
        onBack != null
    if (nativeDetailNavigation) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = NuvioTokens.Space.s4),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            content = actions,
        )
        return
    }
    val resolvedTopPadding = topPadding ?: if (includeStatusBarPadding) statusBarTop else NuvioTokens.Space.none
    Box(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .matchParentSize()
                .background(tokens.colors.background)
                .nuvioConsumePointerEvents(),
        ) {}
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = resolvedTopPadding, bottom = NuvioTokens.Space.s4),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.controlGap),
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(Res.string.action_back),
                            tint = tokens.colors.textPrimary,
                        )
                    }
                }
                AnimatedContent(
                    targetState = title,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "screen_header_title",
                ) { currentTitle ->
                    Text(
                        text = currentTitle,
                        style = MaterialTheme.typography.displayLarge,
                        color = tokens.colors.textPrimary,
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s2),
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
    }
}



@Composable
fun NuvioBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.nuvio.shapes.avatar,
    containerColor: Color = MaterialTheme.nuvio.colors.surface,
    contentColor: Color = MaterialTheme.nuvio.colors.textPrimary,
    buttonSize: Dp = NuvioTokens.Space.s40,
    iconSize: Dp = NuvioTokens.Icon.md,
    contentDescription: String = stringResource(Res.string.action_back),
) {
    if (LocalUseNativeNavigation.current && !LocalNativeNavigationBarHidden.current) return

    Box(
        modifier = modifier
            .size(buttonSize)
            .clip(shape)
            .background(containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(iconSize),
        )
    }
}




/**
 * The app's confirmation and status dialog.
 *
 * Built on the dialog component rather than on a bare surface with the parts placed by hand: the
 * headline, the supporting text and the actions are the dialog's own slots, so they take its
 * spec's placement, tone, shape and text button treatment. A busy dialog shows the indicator as
 * the dialog's icon and withholds its actions, since neither is answerable yet.
 *
 * m3.material.io/components/dialogs/specs
 */
@Composable
fun NuvioStatusModal(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    isBusy: Boolean = false,
    confirmText: String = stringResource(Res.string.action_ok),
    dismissText: String? = null,
    onConfirm: () -> Unit,
    onDismiss: (() -> Unit)? = null,
) {
    if (!isVisible) return

    AlertDialog(
        modifier = modifier,
        onDismissRequest = {
            if (!isBusy) {
                onDismiss?.invoke() ?: onConfirm()
            }
        },
        icon = if (isBusy) {
            { NuvioLoadingIndicator() }
        } else {
            null
        },
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isBusy) {
                Text(confirmText)
            }
        },
        dismissButton = if (!isBusy && dismissText != null && onDismiss != null) {
            {
                TextButton(onClick = onDismiss) {
                    Text(dismissText)
                }
            }
        } else {
            null
        },
    )
}


/**
 * Brief messages that go away on their own.
 *
 * These are the platform's own toast rather than a snackbar the app draws: nothing in them is
 * actionable and nothing waits on them, so they need no host placed on a screen, no room reserved
 * in a layout, and they outlive whatever screen raised them.
 */
object NuvioToastController {

    fun show(message: String) = platformShowToast(message)

    fun dismiss() = platformDismissToast()
}
