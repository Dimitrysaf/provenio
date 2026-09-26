package io.github.dimitrysaf.provenio.shell.screens.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.github.dimitrysaf.provenio.shell.components.LoadingSpinner
import io.github.dimitrysaf.provenio.shell.theme.typeScale

private const val UnloadedLogoAlpha = 0.3f

// The title's logo as a loading meter: the loaded share from the start edge is solid, the rest see-through.
@Composable
internal fun PlayerBufferingLogo(
    logo: String?,
    title: String?,
    fraction: Float,
    modifier: Modifier = Modifier,
) {
    val shownFraction by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 300),
        label = "bufferingLogoFraction",
    )
    var logoLoadError by remember(logo) { mutableStateOf(false) }
    val logoUrl = logo?.takeIf { it.isNotBlank() && !logoLoadError }

    BoxWithConstraints(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val logoWidth = minOf(320.dp, maxWidth - 48.dp)
        val logoHeight = minOf(180.dp, maxHeight * 0.4f)
        val titleFontSize = if (maxWidth < 600.dp) 30.sp else 42.sp
        val mark: (@Composable (Modifier) -> Unit)? = when {
            logoUrl != null -> { markModifier ->
                AsyncImage(
                    model = logoUrl,
                    contentDescription = null,
                    modifier = markModifier.width(logoWidth).height(logoHeight),
                    contentScale = ContentScale.Fit,
                    onError = { logoLoadError = true },
                )
            }
            !title.isNullOrBlank() -> { markModifier ->
                Text(
                    text = title,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    style = MaterialTheme.typeScale.displayMd.copy(
                        fontSize = titleFontSize,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    modifier = markModifier.padding(horizontal = 24.dp),
                )
            }
            else -> null
        }
        if (mark == null) {
            LoadingSpinner(color = Color.White, modifier = Modifier.size(54.dp))
        } else Box(contentAlignment = Alignment.Center) {
            mark(Modifier.alpha(UnloadedLogoAlpha))
            mark(
                Modifier.drawWithContent {
                    val loadedWidth = size.width * shownFraction
                    val left = if (layoutDirection == LayoutDirection.Ltr) 0f else size.width - loadedWidth
                    clipRect(left = left, right = left + loadedWidth) {
                        this@drawWithContent.drawContent()
                    }
                },
            )
        }
    }
}
