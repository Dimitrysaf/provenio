package io.github.dimitrysaf.provenio.designsystem.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale

/**
 * An image, actually blurred — not a stand-in for it, like a heavy downscale or a scrim.
 *
 * Compose's own [androidx.compose.ui.draw.blur] only exists through RenderEffect, an
 * Android 12+ (API 31) capability with no fallback and, from common code, no way to even
 * ask whether it is there. Below that this decodes the same bytes and blurs the actual
 * pixels through a platform API that predates RenderEffect, so something that has to stay
 * hidden — a spoiler still, not just to a good approximation — stays hidden the same way
 * on every device rather than only the newest ones.
 */
@Composable
expect fun BlurredAsyncImage(
    url: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
)
