package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import io.github.dimitrysaf.provenio.shell.screens.player.FullscreenPlayerDialog
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.action_close
import provenio.composeapp.generated.resources.image_viewer_download

internal class ViewerImage(
    val url: String,
    val previewUrl: String? = null,
)

internal class ImageViewerRequest(
    val images: List<ViewerImage>,
    val title: String,
    val initialIndex: Int = 0,
    val colorFilter: ColorFilter? = null,
)

internal val LocalImageViewerLauncher = staticCompositionLocalOf<((ImageViewerRequest) -> Unit)?> { null }

internal fun viewerImageOf(url: String): ViewerImage =
    ViewerImage(url = fullSizeImageUrl(url), previewUrl = url)

internal fun fullSizeImageUrl(url: String): String =
    url.replace(Regex("(image\\.tmdb\\.org/t/p/)[^/]+/"), "\$1original/")

@Composable
internal fun ImageViewerHost(
    request: ImageViewerRequest?,
    onDismiss: () -> Unit,
) {
    if (request == null) return
    ImageViewer(
        images = request.images,
        initialIndex = request.initialIndex,
        title = request.title,
        colorFilter = request.colorFilter,
        onDismiss = onDismiss,
    )
}

@Composable
internal fun ImageViewer(
    images: List<ViewerImage>,
    initialIndex: Int,
    title: String,
    onDismiss: () -> Unit,
    colorFilter: ColorFilter? = null,
) {
    if (images.isEmpty()) return
    val saveImage = rememberImageSaver()
    val pagerState = rememberPagerState(initialPage = initialIndex.coerceIn(0, images.lastIndex)) { images.size }
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val platformContext = LocalPlatformContext.current

    FullscreenPlayerDialog(onDismiss = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.Escape -> {
                            onDismiss()
                            true
                        }
                        Key.DirectionLeft -> {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0))
                            }
                            true
                        }
                        Key.DirectionRight -> {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage((pagerState.currentPage + 1).coerceAtMost(images.lastIndex))
                            }
                            true
                        }
                        else -> false
                    }
                },
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { page -> "$page:${images[page].url}" },
            ) { page ->
                val image = images[page]
                val request = remember(platformContext, image.url, image.previewUrl) {
                    ImageRequest.Builder(platformContext)
                        .data(image.url)
                        .placeholderMemoryCacheKey(image.previewUrl)
                        .build()
                }
                AsyncImage(
                    model = request,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    colorFilter = colorFilter,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                ImageViewerButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = stringResource(Res.string.action_close),
                    onClick = onDismiss,
                )
                ImageViewerButton(
                    icon = Icons.Rounded.Download,
                    contentDescription = stringResource(Res.string.image_viewer_download),
                    onClick = {
                        val page = pagerState.currentPage
                        val image = images[page]
                        saveImage(
                            ImageSaveRequest(
                                url = image.url,
                                fileName = imageFileName(title, page, image.url),
                            ),
                        )
                    },
                )
            }
        }

        LaunchedEffect(Unit) {
            runCatching { focusRequester.requestFocus() }
        }
    }
}

@Composable
private fun ImageViewerButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    WithTooltip(contentDescription) {
        FilledTonalIconButton(
            onClick = onClick,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = Color.Black.copy(alpha = 0.55f),
                contentColor = Color.White,
            ),
        ) {
            Icon(imageVector = icon, contentDescription = contentDescription)
        }
    }
}

private fun imageFileName(title: String, page: Int, url: String): String {
    val base = title
        .trim()
        .replace(Regex("[^\\p{L}\\p{N}]+"), "-")
        .trim('-')
        .ifBlank { "image" }
    val extension = url
        .substringBefore('?')
        .substringAfterLast('/')
        .substringAfterLast('.', "")
        .lowercase()
        .takeIf { it in setOf("jpg", "jpeg", "png", "webp") }
        ?: "jpg"
    return "$base-${page + 1}.$extension"
}
