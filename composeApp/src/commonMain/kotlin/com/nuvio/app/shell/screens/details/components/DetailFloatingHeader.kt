package com.nuvio.app.shell.screens.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nuvio.app.shell.components.NuvioBackButton
import com.nuvio.app.shell.components.platformPhysicalTopInset
import com.nuvio.app.core.metadata.MetaDetails
import com.nuvio.app.core.build.isIos
import com.nuvio.app.shell.nav.LocalUseNativeNavigation
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/** The page's top app bar: always there for the back button, its surface and title tied to the hero's retreat. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailFloatingHeader(
    meta: MetaDetails,
    progress: Float,
    backgroundColor: Color? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val useNativeNavigation = LocalUseNativeNavigation.current
    val safeAreaTop = if (useNativeNavigation) {
        platformPhysicalTopInset()
    } else {
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    }
    val headerTopPadding = (safeAreaTop - 6.dp).coerceAtLeast(safeAreaTop * 0.8f)
    val surfaceColor = backgroundColor ?: if (isIos) {
        MaterialTheme.colorScheme.surface.copy(alpha = 1.0f)
    } else {
        MaterialTheme.colorScheme.background
    }
    var logoLoadError by remember(meta.id, meta.logo) {
        mutableStateOf(false)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                shadowElevation = 4.dp.toPx() * progress
                shape = RectangleShape
            },
    ) {
        val logoWidth = (maxWidth * 0.6f).coerceAtMost(LogoMaxWidth)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clipToBounds()
                .background(surfaceColor.copy(alpha = progress)),
        ) {
            CenterAlignedTopAppBar(
                modifier = Modifier.padding(top = headerTopPadding),
                // The status bar is already paid for above, by an inset the bar cannot see on
                // iOS, where native navigation owns the real one.
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
                title = {
                    Box(modifier = Modifier.graphicsLayer { alpha = progress }) {
                        if (!meta.logo.isNullOrBlank() && !logoLoadError) {
                            AsyncImage(
                                model = meta.logo,
                                contentDescription = stringResource(
                                    Res.string.detail_logo_content_description,
                                    meta.name,
                                ),
                                modifier = Modifier
                                    .width(logoWidth)
                                    .widthIn(max = LogoMaxWidth)
                                    .height(LogoHeight),
                                onError = { logoLoadError = true },
                            )
                        } else {
                            Text(
                                text = meta.name,
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (!useNativeNavigation) {
                        // Over artwork the arrow needs its own scrim; the bar's surface takes
                        // that job over as it fades in behind it.
                        NuvioBackButton(
                            onClick = onBack,
                            containerColor = MaterialTheme.colorScheme.scrim
                                .copy(alpha = 0.40f * (1f - progress)),
                            contentColor = lerp(
                                Color.White,
                                MaterialTheme.colorScheme.onBackground,
                                progress,
                            ),
                        )
                    } else {
                        // Native iOS navigation owns the back button, but retaining this slot
                        // keeps the logo centred as the bar replaces the hero.
                        Box(modifier = Modifier.size(NavigationSlotSize))
                    }
                },
                actions = {
                    // Balances the navigation slot so the title stays centred.
                    Box(modifier = Modifier.size(NavigationSlotSize))
                },
            )

            if (isIos) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(Color.White.copy(alpha = 0.15f * progress)),
                )
            }
        }
    }
}

/** Wide enough to read, narrow enough to leave the two action slots alone. */
private val LogoMaxWidth = 240.dp

private val LogoHeight = 42.dp

/** Matches the navigation icon a top app bar would have placed here. */
private val NavigationSlotSize = 48.dp
