package io.github.dimitrysaf.provenio.shell.screens.details.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import io.github.dimitrysaf.provenio.core.metadata.CastResolver
import io.github.dimitrysaf.provenio.core.metadata.MetaPerson
import io.github.dimitrysaf.provenio.core.metadata.castAvatarSharedTransitionKey
import io.github.dimitrysaf.provenio.shell.components.DeceasedPhotoFilter
import io.github.dimitrysaf.provenio.shell.components.FindableText
import io.github.dimitrysaf.provenio.shell.components.ImageViewerRequest
import io.github.dimitrysaf.provenio.shell.components.LocalImageViewerLauncher
import io.github.dimitrysaf.provenio.shell.components.onSecondaryClick
import io.github.dimitrysaf.provenio.shell.components.viewerImageOf
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.settings_meta_cast

/**
 * The cast as a segmented list: the first few, then View all for the cast page when [onViewAll]
 * is given; every member when it is not, as in the player's info sheet.
 */
@Composable
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
fun DetailCastSection(
    cast: List<MetaPerson>,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
    onCastClick: ((MetaPerson, String?) -> Unit)? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    metaId: String? = null,
    metaType: String? = null,
    onViewAll: (() -> Unit)? = null,
) {
    if (cast.isEmpty()) return
    val resolvedCast = rememberResolvedCast(cast, metaId, metaType)

    DetailSection(
        title = stringResource(Res.string.settings_meta_cast),
        modifier = modifier,
        showHeader = showHeader,
    ) {
        DetailSegmentedList(
            items = resolvedCast.withIndex().toList(),
            previewCount = DetailListPreviewCount,
            showingAll = false,
            onViewAll = onViewAll,
        ) { (index, person), shapes ->
            CastRow(
                index = index,
                person = person,
                shapes = shapes,
                onCastClick = onCastClick,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
            )
        }
    }
}

/** The cast with their photos and IDs filled in where the resolver finds them; the raw cast until then. */
@Composable
internal fun rememberResolvedCast(cast: List<MetaPerson>, metaId: String?, metaType: String?): List<MetaPerson> {
    val resolvedCast by produceState(initialValue = cast, cast, metaId, metaType) {
        value = CastResolver.resolve(cast, metaId, metaType)
    }
    return resolvedCast
}

/** One cast member as a list row: opens their page on click and their photo on long press. */
@Composable
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
internal fun CastRow(
    index: Int,
    person: MetaPerson,
    shapes: ListItemShapes,
    onCastClick: ((MetaPerson, String?) -> Unit)?,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
) {
    val launchImageViewer = LocalImageViewerLauncher.current
    val sharedTransitionKey = person.tmdbId
        ?.takeIf { it > 0 }
        ?.let { castAvatarSharedTransitionKey(it, occurrenceIndex = index) }
    val onClick = if (onCastClick != null && person.tmdbId != null && person.tmdbId > 0) {
        { onCastClick(person, sharedTransitionKey) }
    } else {
        null
    }
    val onLongClick = person.photo
        ?.takeIf(String::isNotBlank)
        ?.let { photo ->
            launchImageViewer?.let { openViewer ->
                {
                    openViewer(
                        ImageViewerRequest(
                            images = listOf(viewerImageOf(photo)),
                            title = person.name,
                            colorFilter = if (person.deceased) DeceasedPhotoFilter else null,
                        ),
                    )
                }
            }
        }
    SegmentedListItem(
        onClick = { onClick?.invoke() },
        onLongClick = onLongClick,
        shapes = shapes,
        modifier = Modifier.fillMaxWidth().onSecondaryClick(onLongClick),
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        leadingContent = {
            CastPhoto(
                person = person,
                sharedTransitionKey = sharedTransitionKey,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
            )
        },
        supportingContent = person.role?.takeIf(String::isNotBlank)?.let { role ->
            { FindableText(text = role, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        },
    ) {
        FindableText(text = person.name, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
private fun CastPhoto(
    person: MetaPerson,
    sharedTransitionKey: String?,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
) {
    val platformContext = LocalPlatformContext.current
    val avatarRequest = if (!person.photo.isNullOrBlank() && !sharedTransitionKey.isNullOrBlank()) {
        remember(platformContext, person.photo, sharedTransitionKey) {
            ImageRequest.Builder(platformContext)
                .data(person.photo)
                .memoryCacheKey(sharedTransitionKey)
                .placeholderMemoryCacheKey(sharedTransitionKey)
                .diskCacheKey(person.photo)
                .build()
        }
    } else {
        null
    }
    val sharedElement = if (
        sharedTransitionScope != null &&
        animatedVisibilityScope != null &&
        !sharedTransitionKey.isNullOrBlank()
    ) {
        with(sharedTransitionScope) {
            Modifier.sharedElement(
                sharedContentState = rememberSharedContentState(key = sharedTransitionKey),
                animatedVisibilityScope = animatedVisibilityScope,
            )
        }
    } else {
        Modifier
    }
    // Profile photos are 2:3 portraits, so a frame of that shape shows the whole face rather than a crop of it.
    Box(
        modifier = Modifier
            .then(sharedElement)
            .width(CastPhotoWidth)
            .aspectRatio(2f / 3f)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (person.photo != null) {
            AsyncImage(
                model = avatarRequest ?: person.photo,
                contentDescription = person.name,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                colorFilter = if (person.deceased) DeceasedPhotoFilter else null,
            )
        } else {
            FindableText(
                text = person.name.initials(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private val CastPhotoWidth = 48.dp

private fun String.initials(): String {
    val parts = trim().split(" ").filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> "${parts.first().first().uppercaseChar()}${parts.last().first().uppercaseChar()}"
        parts.size == 1 -> parts.first().take(2).uppercase()
        else -> ""
    }
}
