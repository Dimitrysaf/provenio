package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.provenio.core.home.HomeCatalogSettingsRepository
import io.github.dimitrysaf.provenio.core.home.HomeShelfLayout
import io.github.dimitrysaf.provenio.core.watch.progress.ContinueWatchingPreferencesRepository
import io.github.dimitrysaf.provenio.core.watch.progress.ContinueWatchingSectionStyle
import io.github.dimitrysaf.provenio.shell.components.landscapePosterHeightForWidth
import io.github.dimitrysaf.provenio.shell.components.landscapePosterWidth
import io.github.dimitrysaf.provenio.shell.components.rememberPosterCardStyleUiState
import io.github.dimitrysaf.provenio.shell.screens.home.components.continueWatchingLandscapeCardHeight
import io.github.dimitrysaf.provenio.shell.screens.home.components.continueWatchingLandscapeCardWidth

/** The part of the home screen a settings page is about, drawn in the accent tone. */
internal enum class HomePreviewSection { Hero, ContinueWatching, Catalogs }

/**
 * Half the height of a compact phone's 640dp viewport, the smallest phones in common use, so the
 * preview never takes more than half of any phone screen. It is a fixed size rather than a share
 * of the window: on a taller phone or a tablet it is simply less than half.
 */
internal val HomeLayoutPreviewHeight = 320.dp

/** The preview draws the home screen at half its real size. */
private const val PreviewScale = 0.5f

/**
 * A wireframe of the top of the home screen, in the order the home screen stacks it: the hero
 * carousel, Continue Watching, then the catalog rows. Each section shows only while it is on, and
 * [highlight] is drawn in the accent tone so the page's own section stands out from the rest.
 *
 * Plain blocks only, with no text or artwork, and no shimmer either, since nothing is loading.
 * It reads the settings itself, so every page shows the same, current home screen.
 */
@Composable
internal fun HomeLayoutPreview(highlight: HomePreviewSection) {
    val homeSettings by remember {
        HomeCatalogSettingsRepository.snapshot()
        HomeCatalogSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val continueWatching by remember {
        ContinueWatchingPreferencesRepository.ensureLoaded()
        ContinueWatchingPreferencesRepository.uiState
    }.collectAsStateWithLifecycle()
    val posterStyle = rememberPosterCardStyleUiState()

    val posterWidth = if (posterStyle.catalogLandscapeModeEnabled) {
        landscapePosterWidth(posterStyle.widthDp)
    } else {
        posterStyle.widthDp.dp
    }
    val posterHeight = if (posterStyle.catalogLandscapeModeEnabled) {
        landscapePosterHeightForWidth(posterWidth)
    } else {
        posterStyle.heightDp.dp
    }
    val cornerRadius = (posterStyle.cornerRadiusDp * PreviewScale).dp
    // The cards are the subject on Card Styles, so the view starts at the catalogs, as if scrolled
    // past the sections above: with the hero in it, a single row fits and the bottom edge cuts it
    // off right where its labels are.
    val showSectionsAbove = highlight != HomePreviewSection.Catalogs
    val screenColor = MaterialTheme.colorScheme.surfaceContainer

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(HomeLayoutPreviewHeight)
            .clip(RoundedCornerShape(OuterCorner + 8.dp))
            .background(screenColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp),
        ) {
            AnimatedVisibility(
                visible = showSectionsAbove && homeSettings.heroEnabled,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                PreviewHero(highlighted = highlight == HomePreviewSection.Hero)
            }
            AnimatedVisibility(
                visible = showSectionsAbove && continueWatching.isVisible,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                PreviewContinueWatchingRow(
                    style = continueWatching.style,
                    basePosterWidthDp = posterStyle.widthDp,
                    basePosterHeightDp = posterStyle.heightDp,
                    cornerRadius = cornerRadius,
                    highlighted = highlight == HomePreviewSection.ContinueWatching,
                )
            }
            val catalogHighlighted = highlight == HomePreviewSection.Catalogs
            val posterColor = accentOrBlock(catalogHighlighted, MaterialTheme.colorScheme.secondaryContainer)
            val labelColor = accentOrBlock(catalogHighlighted, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f))
            val showLabels = !posterStyle.hideLabelsEnabled
            val catalogAsGrid = homeSettings.shelfLayout == HomeShelfLayout.Grid
            repeat(PreviewCatalogRowCount) {
                PreviewRow(
                    itemWidth = posterWidth * PreviewScale,
                    itemHeight = posterHeight * PreviewScale,
                    gridRows = if (catalogAsGrid && homeSettings.shelvesExpandedByDefault) 2 else if (catalogAsGrid) 0 else null,
                ) { modifier ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        PreviewBlock(modifier, posterColor, RoundedCornerShape(cornerRadius))
                        // The title under each card, at most the card's width.
                        if (showLabels) {
                            PreviewBlock(
                                Modifier.size(width = posterWidth * PreviewScale * 0.7f, height = 4.dp),
                                labelColor,
                                RoundedCornerShape(2.dp),
                            )
                        }
                    }
                }
            }
        }
        PreviewBottomFade(color = screenColor)
    }
}

/** More rows than ever fit, so the bottom edge always cuts one off, as the real list would. */
private const val PreviewCatalogRowCount = 4

@Composable
private fun accentOrBlock(highlighted: Boolean, accent: Color): Color {
    val color by animateColorAsState(
        targetValue = if (highlighted) accent else MaterialTheme.colorScheme.surfaceContainerHighest,
        label = "homePreviewSectionTone",
    )
    return color
}

/** The focal card with the neighbours peeking in from either side, and the page indicator. */
@Composable
private fun PreviewHero(highlighted: Boolean) {
    val heroColor = accentOrBlock(highlighted, MaterialTheme.colorScheme.secondaryContainer)
    val indicatorColor = accentOrBlock(highlighted, MaterialTheme.colorScheme.secondary)
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(150.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val heroShape = RoundedCornerShape(14.dp)
            PreviewBlock(Modifier.width(12.dp).fillMaxHeight(), heroColor, heroShape)
            PreviewBlock(Modifier.weight(1f).fillMaxHeight(), heroColor, heroShape)
            PreviewBlock(Modifier.width(12.dp).fillMaxHeight(), heroColor, heroShape)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            val indicatorShape = RoundedCornerShape(2.dp)
            PreviewBlock(Modifier.size(width = 14.dp, height = 4.dp), indicatorColor, indicatorShape)
            repeat(2) {
                PreviewBlock(Modifier.size(4.dp), MaterialTheme.colorScheme.outlineVariant, indicatorShape)
            }
        }
    }
}

/**
 * The Continue Watching row in its card style, at the same scale as the rest: landscape cards,
 * wide cards with the poster strip at their start, or posters. Each carries a progress bar, which
 * is what tells this row apart from a catalog.
 */
@Composable
private fun PreviewContinueWatchingRow(
    style: ContinueWatchingSectionStyle,
    basePosterWidthDp: Int,
    basePosterHeightDp: Int,
    cornerRadius: Dp,
    highlighted: Boolean,
) {
    val cardColor = accentOrBlock(highlighted, MaterialTheme.colorScheme.secondaryContainer)
    val progressColor = accentOrBlock(highlighted, MaterialTheme.colorScheme.primary)
    val stripColor = accentOrBlock(highlighted, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f))
    // The real card sizes, from the same formulas the home screen uses.
    val (width, height) = when (style) {
        ContinueWatchingSectionStyle.Card ->
            continueWatchingLandscapeCardWidth(basePosterWidthDp) to
                continueWatchingLandscapeCardHeight(basePosterWidthDp)
        ContinueWatchingSectionStyle.Wide -> {
            val wideWidth = basePosterWidthDp.dp * 2.1f
            wideWidth to wideWidth * 0.4f
        }
        ContinueWatchingSectionStyle.Poster -> basePosterWidthDp.dp to basePosterHeightDp.dp
    }
    val itemHeight = height * PreviewScale
    PreviewRow(itemWidth = width * PreviewScale, itemHeight = itemHeight) { modifier ->
        Box(modifier = modifier.clip(RoundedCornerShape(cornerRadius)).background(cardColor)) {
            if (style == ContinueWatchingSectionStyle.Wide) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(itemHeight * (2f / 3f))
                        .background(stripColor),
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(0.55f)
                    .height(3.dp)
                    .background(progressColor),
            )
        }
    }
}

/**
 * A section's title, then its items running off the right edge, or, with [gridRows], that many
 * rows wrapped to the width as a grid shelf lays them out (none for a collapsed one). [item] draws
 * one item at the size of the modifier it is given.
 */
@Composable
private fun PreviewRow(
    itemWidth: Dp,
    itemHeight: Dp,
    gridRows: Int? = null,
    item: @Composable (Modifier) -> Unit,
) {
    if (gridRows != null) {
        PreviewGridShelf(itemWidth = itemWidth, itemHeight = itemHeight, rows = gridRows, item = item)
        return
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clipToBounds()
            .padding(bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PreviewBlock(
            modifier = Modifier.padding(start = 12.dp).size(width = 64.dp, height = 8.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = RoundedCornerShape(4.dp),
        )
        // Unbounded, so the row lays out every item at full size and the edge crops the last
        // one, rather than squeezing it to whatever width is left.
        Row(
            modifier = Modifier
                .wrapContentWidth(align = Alignment.Start, unbounded = true)
                .padding(start = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            repeat(PreviewItemsPerRow) {
                item(Modifier.size(width = itemWidth, height = itemHeight))
            }
        }
    }
}

@Composable
private fun PreviewGridShelf(
    itemWidth: Dp,
    itemHeight: Dp,
    rows: Int,
    item: @Composable (Modifier) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        val perRow = (((maxWidth - 24.dp + 5.dp) / (itemWidth + 5.dp)).toInt()).coerceAtLeast(1)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.padding(start = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PreviewBlock(
                    modifier = Modifier.size(width = 64.dp, height = 8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = RoundedCornerShape(4.dp),
                )
                // The chevron that opens and closes the shelf.
                PreviewBlock(
                    modifier = Modifier.size(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = RoundedCornerShape(4.dp),
                )
            }
            repeat(rows) {
                Row(
                    modifier = Modifier.padding(start = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    repeat(perRow) {
                        item(Modifier.size(width = itemWidth, height = itemHeight))
                    }
                }
            }
        }
    }
}

/** Enough to run off a tablet's content pane at the smallest item size. */
private const val PreviewItemsPerRow = 16

/** Fades the cut-off rows into the screen, so the edge reads as "continues" rather than "ends". */
@Composable
private fun BoxScope.PreviewBottomFade(color: Color) {
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(48.dp)
            .background(Brush.verticalGradient(listOf(Color.Transparent, color))),
    )
}

@Composable
private fun PreviewBlock(
    modifier: Modifier,
    color: Color,
    shape: RoundedCornerShape,
) {
    Box(modifier = modifier.clip(shape).background(color))
}

/**
 * A page's primary switch: it turns the whole feature on or off, so it stands apart from the
 * rows it governs, larger and on the primary container while on.
 */
@Composable
internal fun SettingsMainSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    val containerColor by animateColorAsState(
        targetValue = if (checked) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        label = "settingsMainSwitchContainer",
    )
    val contentColor = if (checked) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Surface(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        shape = RoundedCornerShape(MainSwitchCorner),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 72.dp)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f).padding(end = 16.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) LocalContentColor.current else LocalContentColor.current.copy(alpha = 0.38f),
            )
            // The surface owns the toggle, so the switch only shows its state.
            Switch(checked = checked, onCheckedChange = null, enabled = enabled)
        }
    }
}

private val MainSwitchCorner = 28.dp
