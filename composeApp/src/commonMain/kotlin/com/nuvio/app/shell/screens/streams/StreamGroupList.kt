package com.nuvio.app.shell.screens.streams

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nuvio.app.core.build.AppFeaturePolicy
import com.nuvio.app.shell.components.NuvioLoadingIndicator
import com.nuvio.app.shell.components.NuvioToastController
import com.nuvio.app.shell.screens.settings.ListItemBetweenSpace
import com.nuvio.app.shell.screens.settings.segmentShape
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import com.nuvio.app.core.streams.AddonStreamGroup
import com.nuvio.app.core.streams.StreamBadgePlacement
import com.nuvio.app.core.streams.StreamItem
import com.nuvio.app.core.streams.StreamsEmptyStateReason
import com.nuvio.app.core.streams.isSelectableForPlayback

/**
 * Which add-ons are open.
 *
 * Every add-on starts open, so the page reads as one list until someone folds a section away;
 * the set therefore holds what is closed rather than what is shown.
 */
internal class StreamGroupExpansion(initiallyCollapsed: Set<String>) {
    var collapsed by mutableStateOf(initiallyCollapsed)
        private set

    fun isExpanded(addonId: String): Boolean = addonId !in collapsed

    fun toggle(addonId: String) {
        collapsed = if (addonId in collapsed) collapsed - addonId else collapsed + addonId
    }
}

/** Folding is per visit to a title, so a new set of streams opens fully again. */
@Composable
internal fun rememberStreamGroupExpansion(key: Any?): StreamGroupExpansion =
    remember(key) { StreamGroupExpansion(emptySet()) }

/**
 * One row of the sheet, before it knows what shape it will be drawn with.
 *
 * Add-on rows and stream rows are flattened into a single list so that corners can be decided
 * from a row's place in the sheet rather than its place in its own group: an add-on that is open,
 * or that is followed by the next add-on, is touching something and takes the inner corner on that
 * side. The whole sheet is therefore one segmented list rather than a stack of separate ones.
 */
internal sealed interface StreamListEntry {
    val key: String

    data class Addon(
        override val key: String,
        val group: AddonStreamGroup,
        val expanded: Boolean,
    ) : StreamListEntry

    data class Stream(
        override val key: String,
        val stream: StreamItem,
        val sourceName: String?,
    ) : StreamListEntry
}

internal fun buildStreamListEntries(
    groups: List<AddonStreamGroup>,
    expansion: StreamGroupExpansion,
): List<StreamListEntry> = buildList {
    groups.forEachIndexed { groupIndex, group ->
        if (group.streams.isEmpty() && !group.isLoading) return@forEachIndexed

        val sectionKey = streamSectionRenderKey(groupIndex = groupIndex, group = group)
        val expanded = expansion.isExpanded(group.addonId)
        add(
            StreamListEntry.Addon(
                key = "stream_group_$sectionKey",
                group = group,
                expanded = expanded,
            ),
        )

        if (!expanded) return@forEachIndexed

        val distinctSources = group.streams
            .map { it.sourceName?.takeIf { name -> name.isNotBlank() } ?: it.addonName }
            .distinct()
        val showSourceNames = distinctSources.size > 1

        group.streams.forEachIndexed { index, stream ->
            add(
                StreamListEntry.Stream(
                    key = streamCardRenderKey(
                        sectionKey = sectionKey,
                        sourceIndex = 0,
                        itemIndex = index,
                        stream = stream,
                    ),
                    stream = stream,
                    sourceName = if (showSourceNames) {
                        stream.sourceName?.takeIf { it.isNotBlank() } ?: stream.addonName
                    } else {
                        null
                    },
                ),
            )
        }
    }
}

/**
 * The add-ons and their streams, as one segmented list.
 *
 * This replaces the filter pills: instead of choosing one add-on and hiding the rest, every
 * add-on is present as a row of its own and can be folded away, so what is on offer stays visible
 * while it loads. An add-on that is still fetching says so on its own row, which is the only place
 * that knows it.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun LazyListScope.streamGroups(
    groups: List<AddonStreamGroup>,
    expansion: StreamGroupExpansion,
    debridEnabled: Boolean,
    appendInstantServiceToDefaultName: Boolean,
    showFileSizeBadges: Boolean,
    showAddonLogo: Boolean,
    badgePlacement: StreamBadgePlacement,
    isStreamSelected: (StreamItem) -> Boolean,
    torrentNotSupportedText: String,
    onStreamSelected: (StreamItem) -> Unit,
    onStreamLongPress: (StreamItem) -> Unit,
    horizontalPadding: Dp,
) {
    val entries = buildStreamListEntries(groups = groups, expansion = expansion)
    if (entries.isEmpty()) return

    itemsIndexed(
        items = entries,
        key = { _, entry -> entry.key },
    ) { index, entry ->
        val shape = segmentShape(index = index, count = entries.size)
        val shapes = ListItemDefaults.shapes(
            shape = shape,
            selectedShape = shape,
            pressedShape = shape,
            focusedShape = shape,
            hoveredShape = shape,
        )
        val rowModifier = Modifier
            .padding(horizontal = horizontalPadding)
            .padding(bottom = if (index == entries.lastIndex) 0.dp else ListItemBetweenSpace)
            .animateItem()

        when (entry) {
            is StreamListEntry.Addon -> StreamGroupRow(
                group = entry.group,
                expanded = entry.expanded,
                shapes = shapes,
                onClick = { expansion.toggle(entry.group.addonId) },
                modifier = rowModifier,
            )

            is StreamListEntry.Stream -> {
                val stream = entry.stream
                val isSelectable = stream.isSelectableForPlayback(debridEnabled)
                val isUnsupportedTorrentStream = stream.needsLocalDebridResolve &&
                    !AppFeaturePolicy.p2pEnabled &&
                    !(debridEnabled && stream.isAddonDebridCandidate)
                StreamRow(
                    stream = stream,
                    shapes = shapes,
                    enabled = isSelectable || isUnsupportedTorrentStream,
                    appendInstantServiceToDefaultName = appendInstantServiceToDefaultName,
                    showFileSizeBadges = showFileSizeBadges,
                    showAddonLogo = showAddonLogo,
                    badgePlacement = badgePlacement,
                    selected = isStreamSelected(stream),
                    sourceName = entry.sourceName,
                    modifier = rowModifier,
                    onClick = {
                        if (isSelectable) {
                            onStreamSelected(stream)
                        } else if (isUnsupportedTorrentStream) {
                            NuvioToastController.show(torrentNotSupportedText)
                        }
                    },
                    onLongClick = {
                        if (stream.playableDirectUrl != null || stream.shouldOpenExternally || stream.isAddonDebridCandidate) {
                            onStreamLongPress(stream)
                        }
                    },
                )
            }
        }
    }
}

/**
 * An add-on, as the list item that opens and closes its own streams.
 *
 * It is a list row like the ones it heads rather than a heading floating above them, so folding
 * one away leaves the sheet still reading as a single list. It sits one container tone above its
 * streams, which is what separates the two without a divider.
 *
 * m3.material.io/components/lists/specs
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StreamGroupRow(
    group: AddonStreamGroup,
    expanded: Boolean,
    shapes: ListItemShapes,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "stream_group_chevron",
    )

    SegmentedListItem(
        onClick = onClick,
        shapes = shapes,
        modifier = modifier.fillMaxWidth(),
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        leadingContent = { StreamAddonIcon(logo = group.addonLogo, addonName = group.addonName) },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (group.isLoading) {
                    // Indeterminate: an add-on reports no progress, only that it is still asked.
                    CircularProgressIndicator(
                        modifier = Modifier.size(GroupLoadingIndicatorSize),
                        strokeWidth = GroupLoadingIndicatorStroke,
                    )
                } else if (group.streams.isNotEmpty()) {
                    Text(
                        text = group.streams.size.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.rotate(chevronRotation),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    ) {
        Text(
            text = group.addonName,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** The add-on's own icon, or a stand-in for the add-ons that ship without one. */
@Composable
private fun StreamAddonIcon(
    logo: String?,
    addonName: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(AddonIconSize)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        if (!logo.isNullOrBlank()) {
            AsyncImage(
                model = logo,
                contentDescription = addonName,
                modifier = Modifier.size(AddonIconSize),
                contentScale = ContentScale.Fit,
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Extension,
                contentDescription = null,
                modifier = Modifier.size(AddonIconFallbackSize),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * The sheet while it is still working out what to ask for: no add-on has been asked yet, so
 * there is no row to put an indicator on.
 */
@Composable
internal fun StreamsPreparingBlock(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        NuvioLoadingIndicator(modifier = Modifier.size(32.dp))
        Text(
            text = stringResource(Res.string.streams_finding_streams),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun StreamsEmptyBlock(
    reason: StreamsEmptyStateReason?,
    modifier: Modifier = Modifier,
) {
    val title: String
    val message: String

    when (reason) {
        StreamsEmptyStateReason.NoAddonsInstalled,
        StreamsEmptyStateReason.NoCompatibleAddons -> {
            title = stringResource(Res.string.playback_unavailable)
            message = stringResource(Res.string.playback_unavailable_message)
        }

        StreamsEmptyStateReason.StreamFetchFailed -> {
            title = stringResource(Res.string.streams_empty_load_failed_title)
            message = stringResource(Res.string.streams_empty_load_failed_message)
        }

        StreamsEmptyStateReason.NoStreamsFound, null -> {
            title = stringResource(Res.string.compose_player_no_streams_found)
            message = stringResource(Res.string.streams_empty_no_streams_message)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** The add-on icon in an add-on's row, matching the logo a stream row carries. */
private val AddonIconSize = 28.dp

/** The stand-in glyph, inset inside the icon's container. */
private val AddonIconFallbackSize = 18.dp

/** Small enough to sit in a list item's trailing slot without setting the row's height. */
private val GroupLoadingIndicatorSize = 20.dp

/** Scaled down with the indicator, from the 4dp the component draws at its own size. */
private val GroupLoadingIndicatorStroke = 2.dp
