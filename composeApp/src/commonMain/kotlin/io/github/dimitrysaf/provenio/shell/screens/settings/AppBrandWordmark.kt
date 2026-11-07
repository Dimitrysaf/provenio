package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.min
import org.jetbrains.compose.resources.painterResource
import io.github.dimitrysaf.provenio.core.settings.AppIconOption
import io.github.dimitrysaf.provenio.core.settings.AppIconRepository
import io.github.dimitrysaf.provenio.core.settings.wordmarkResource
import io.github.dimitrysaf.provenio.shell.theme.rememberIconColorScheme
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.app_logo_symbol

@Composable
internal fun AppBrandWordmark(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    icon: AppIconOption? = null,
) {
    val state by remember {
        AppIconRepository.ensureLoaded()
        AppIconRepository.state
    }.collectAsStateWithLifecycle()
    Image(
        painter = painterResource(
            (icon ?: state.selected).wordmarkResource,
        ),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}

/**
 * The app's symbol alone, without the icon's background, in the colour of the app icon chosen
 * in Appearance, so it matches what the launcher shows.
 */
@Composable
internal fun AppBrandSymbol(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val state by remember {
        AppIconRepository.ensureLoaded()
        AppIconRepository.state
    }.collectAsStateWithLifecycle()
    val darkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val tint = rememberIconColorScheme(state.selected, darkTheme)?.primary ?: MaterialTheme.colorScheme.primary
    Icon(
        painter = painterResource(Res.drawable.app_logo_symbol),
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint,
    )
}

/**
 * The wordmark as the app comes up: the "p" drops in from large, then moves left to make room as
 * the rest of the name appears letter by letter. [onFinished] runs once the whole name is in place.
 */
@Composable
internal fun AnimatedAppBrandWordmark(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    onFinished: () -> Unit = {},
) {
    val state by remember {
        AppIconRepository.ensureLoaded()
        AppIconRepository.state
    }.collectAsStateWithLifecycle()
    val painter = painterResource(state.selected.wordmarkResource)
    val drop = remember { Animatable(0f) }
    val slide = remember { Animatable(0f) }
    val letters = remember { Animatable(0f) }
    val currentOnFinished by rememberUpdatedState(onFinished)
    LaunchedEffect(Unit) {
        drop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
        coroutineScope {
            launch { slide.animateTo(1f, tween(WordmarkRevealMillis, easing = FastOutSlowInEasing)) }
            letters.animateTo(WordmarkLetterEdges.size - 2f, tween(WordmarkRevealMillis, easing = LinearEasing))
        }
        currentOnFinished()
    }
    Canvas(
        modifier = modifier.semantics {
            if (contentDescription != null) this.contentDescription = contentDescription
        },
    ) {
        val intrinsic = painter.intrinsicSize
        val aspect = if (intrinsic.isSpecified && intrinsic.height > 0f) intrinsic.width / intrinsic.height else WordmarkAspect
        val width = min(size.width, size.height * aspect)
        val height = width / aspect
        val left = (size.width - width) / 2f
        val top = (size.height - height) / 2f
        val pCenter = Offset(left + WordmarkPCenter * width, top + height / 2f)
        // The "p" starts in the middle of the space; the name ends up centred in it.
        translate(left = (size.width / 2f - pCenter.x) * (1f - slide.value)) {
            scale(scale = WordmarkDropScale + (1f - WordmarkDropScale) * drop.value, pivot = pCenter) {
                for (index in 0 until WordmarkLetterEdges.size - 1) {
                    val alpha = if (index == 0) {
                        (drop.value * 2f).coerceIn(0f, 1f)
                    } else {
                        (letters.value - (index - 1)).coerceIn(0f, 1f)
                    }
                    if (alpha <= 0f) continue
                    clipRect(
                        left = left + WordmarkLetterEdges[index] * width,
                        top = top,
                        right = left + WordmarkLetterEdges[index + 1] * width,
                        bottom = top + height,
                    ) {
                        translate(left = left, top = top) {
                            with(painter) { draw(Size(width, height), alpha = alpha) }
                        }
                    }
                }
            }
        }
    }
}

// Every wordmark colour is the same 1387x344 picture; these are the gaps between its letters.
private const val WordmarkAspect = 1387f / 344f
private val WordmarkLetterEdges = floatArrayOf(0f, 208f, 335f, 524f, 710f, 898f, 1090f, 1178f, 1387f)
    .map { it / 1387f }
private const val WordmarkPCenter = 101f / 1387f
private const val WordmarkDropScale = 3f
private const val WordmarkRevealMillis = 640
