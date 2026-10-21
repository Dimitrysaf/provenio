package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp

/**
 * The large-screen search box while it has focus: the window dims around it, so the box and the
 * recent searches under it stand in front of the page they will change.
 */
internal object SearchFocusScrim {
    private var owner: Any? = null
    var bounds by mutableStateOf<Rect?>(null)
        private set
    var openBelow by mutableStateOf(false)
        private set

    fun show(token: Any, bounds: Rect, openBelow: Boolean) {
        owner = token
        this.bounds = bounds
        this.openBelow = openBelow
    }

    fun hide(token: Any) {
        if (owner !== token) return
        owner = null
        bounds = null
    }
}

/**
 * Draws [SearchFocusScrim] over the window, with the box left clear. It takes no input, so a tap
 * on the page goes through to it as before.
 */
@Composable
fun SearchFocusScrimHost(modifier: Modifier = Modifier) {
    val bounds = SearchFocusScrim.bounds
    val alpha by animateFloatAsState(if (bounds != null) 1f else 0f, label = "search_focus_scrim")
    // The last place the box was, so the scrim fades out around it rather than vanishing.
    val last = remember { LastBounds() }
    if (bounds != null) last.rect = bounds
    val hole = last.rect ?: return
    if (alpha <= 0f) return
    val scrim = MaterialTheme.colorScheme.scrim
    val openBelow = SearchFocusScrim.openBelow
    Canvas(modifier = modifier.fillMaxSize()) {
        val corner = CornerRadius(SearchBoxScrimCorner.toPx())
        val bottomCorner = if (openBelow) CornerRadius.Zero else corner
        val path = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = hole,
                    topLeft = corner,
                    topRight = corner,
                    bottomRight = bottomCorner,
                    bottomLeft = bottomCorner,
                ),
            )
        }
        clipPath(path, clipOp = ClipOp.Difference) {
            drawRect(color = scrim.copy(alpha = ScrimOpacity * alpha))
        }
    }
}

private class LastBounds {
    var rect: Rect? = null
}

/** The search box's own corner. */
private val SearchBoxScrimCorner = 28.dp

/** Material's scrim opacity. */
private const val ScrimOpacity = 0.32f
