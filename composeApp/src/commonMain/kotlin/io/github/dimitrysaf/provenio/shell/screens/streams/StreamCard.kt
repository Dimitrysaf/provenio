package io.github.dimitrysaf.provenio.shell.screens.streams

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.dimitrysaf.provenio.core.debrid.DebridProviders
import io.github.dimitrysaf.provenio.core.streams.StreamBadge
import io.github.dimitrysaf.provenio.core.streams.StreamBadgePlacement
import io.github.dimitrysaf.provenio.core.streams.StreamDebridCacheState
import io.github.dimitrysaf.provenio.core.streams.StreamItem

/**
 * One stream, as a row in its add-on's segmented list.
 *
 * The row playback is currently using is the list's selected item, and says so the way a
 * selected list item says it: the container and the shape, with nothing added on top.
 *
 * m3.material.io/components/lists/specs
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun StreamRow(
    stream: StreamItem,
    shapes: ListItemShapes,
    enabled: Boolean,
    appendInstantServiceToDefaultName: Boolean,
    showFileSizeBadges: Boolean,
    showAddonLogo: Boolean,
    badgePlacement: StreamBadgePlacement,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    selected: Boolean = false,
    sourceName: String? = null,
) {
    val badgeImages = stream.badges.filter { it.imageURL.isNotBlank() }
    val hasBadges = badgeImages.isNotEmpty() || (showFileSizeBadges && stream.behaviorHints.videoSize != null)
    val subtitle = stream.streamSubtitle?.takeIf { it.isNotBlank() }
    val overline = sourceName?.takeIf { it.isNotBlank() }
    val instantLabel = if (appendInstantServiceToDefaultName) stream.instantServiceLabel() else null

    SegmentedListItem(
        selected = selected,
        onClick = onClick,
        shapes = shapes,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        onLongClick = onLongClick,
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        overlineContent = overline?.let {
            {
                Text(text = it, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        supportingContent = if (subtitle != null || hasBadges) {
            {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (hasBadges && badgePlacement == StreamBadgePlacement.TOP) {
                        StreamRowBadges(
                            badgeImages = badgeImages,
                            stream = stream,
                            showFileSizeBadges = showFileSizeBadges,
                        )
                    }
                    if (subtitle != null) {
                        Text(text = subtitle)
                    }
                    if (hasBadges && badgePlacement == StreamBadgePlacement.BOTTOM) {
                        StreamRowBadges(
                            badgeImages = badgeImages,
                            stream = stream,
                            showFileSizeBadges = showFileSizeBadges,
                        )
                    }
                }
            }
        } else {
            null
        },
        leadingContent = if (showAddonLogo && !stream.addonLogo.isNullOrBlank()) {
            {
                AsyncImage(
                    model = stream.addonLogo,
                    contentDescription = stream.addonName,
                    modifier = Modifier
                        .size(AddonLogoSize)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit,
                )
            }
        } else {
            null
        },
    ) {
        Text(
            text = instantLabel?.let { "${stream.streamLabel} $it" } ?: stream.streamLabel,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StreamRowBadges(
    badgeImages: List<StreamBadge>,
    stream: StreamItem,
    showFileSizeBadges: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        badgeImages.forEach { badge ->
            StreamBadgeImage(badge = badge)
        }
        if (showFileSizeBadges) {
            StreamFileSizeBadge(stream = stream)
        }
    }
}

private val AddonLogoSize = 28.dp

private fun StreamItem.instantServiceLabel(): String? {
    val status = debridCacheStatus ?: return null
    if (status.state != StreamDebridCacheState.CACHED) return null
    val providerLabel = DebridProviders.shortName(status.providerId)
        .ifBlank { status.providerName.trim() }
        .ifBlank { DebridProviders.displayName(status.providerId) }
    return "- $providerLabel Instant"
}
