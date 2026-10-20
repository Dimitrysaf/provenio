package io.github.dimitrysaf.provenio.shell.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dimitrysaf.provenio.core.playback.SubtitleStyleState
import io.github.dimitrysaf.provenio.core.playback.shadowBlurFraction
import io.github.dimitrysaf.provenio.core.playback.shadowOffsetFraction
import io.github.dimitrysaf.provenio.shell.screens.settings.subtitleTextFontFamily
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * A line of subtitles placed in its frame the way the player places it: centred near the bottom,
 * at the style's size and height, both scaled with the frame. The settings preview draws its
 * sample with this, and the player draws the subtitles it renders itself with it too, so the two
 * always match.
 *
 * Text is set at the style's size in sp over a full-screen landscape frame, so it is scaled by how
 * the frame's height compares with [SubtitleReferenceHeight]. The line's height follows
 * ExoPlayer's: a base bottom padding of two-thirds of its default, plus the offset in thousandths
 * of the frame.
 */
@Composable
internal fun SubtitleFrame(
    text: AnnotatedString,
    style: SubtitleStyleState,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val scale = maxHeight / SubtitleReferenceHeight
        val bottomPadding = maxHeight * (
            SubtitleBaseBottomFraction + (style.bottomOffset / 1000f).coerceIn(0f, 0.2f)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 12.dp, end = 12.dp, bottom = bottomPadding),
        ) {
            StyledSubtitleText(text = text, style = style, fontScale = scale)
        }
    }
}

/**
 * Subtitle text in [style]: its font, weight, colours, outline and background, with its shadow
 * drawn as the text again in the shadow's colour, moved and softened under it, as ExoPlayer's is.
 */
@Composable
private fun StyledSubtitleText(text: AnnotatedString, style: SubtitleStyleState, fontScale: Float) {
    val fontSize = (style.fontSizeSp * fontScale).sp
    val density = LocalDensity.current
    val fontSizePx = with(density) { fontSize.toPx() }
    Box {
        if (style.shadowEnabled) {
            val offset = fontSizePx * style.shadowOffsetFraction
            val radians = style.shadowAngle * PI / 180.0
            SubtitleLayer(
                text = text,
                style = style,
                fontSize = fontSize,
                tint = Color(style.shadowColor),
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (offset * cos(radians)).roundToInt(),
                            (offset * sin(radians)).roundToInt(),
                        )
                    }
                    .blur(
                        radius = with(density) { (fontSizePx * style.shadowBlurFraction).toDp() },
                        edgeTreatment = BlurredEdgeTreatment.Unbounded,
                    ),
            )
        }
        SubtitleLayer(text = text, style = style, fontSize = fontSize, tint = null)
    }
}

/** One layer of the subtitle: in its own colours, or all in [tint] for its shadow. */
@Composable
private fun SubtitleLayer(
    text: AnnotatedString,
    style: SubtitleStyleState,
    fontSize: TextUnit,
    tint: Color?,
    modifier: Modifier = Modifier,
) {
    val baseStyle = TextStyle(
        fontSize = fontSize,
        lineHeight = fontSize * 1.2f,
        fontWeight = if (style.bold) FontWeight.Bold else FontWeight.Normal,
        fontFamily = subtitleTextFontFamily(style.fontFamily),
        textAlign = TextAlign.Center,
    )
    val background = Color(style.backgroundColor)
    Box(
        modifier = modifier
            .background(if (tint != null && background.alpha > 0f) tint else background)
            .padding(horizontal = 4.dp),
    ) {
        if (style.outlineEnabled) {
            // The outline is the text stroked behind the fill, the way an outline edge is drawn.
            val strokeWidth = with(LocalDensity.current) { fontSize.toPx() } * OutlineFraction
            Text(
                text = text,
                style = baseStyle.copy(
                    color = tint ?: Color(style.outlineColor),
                    drawStyle = Stroke(width = strokeWidth, join = StrokeJoin.Round),
                ),
            )
        }
        Text(text = text, style = baseStyle.copy(color = tint ?: Color(style.textColor)))
    }
}

/** A full-screen landscape frame on a phone is about this tall. */
private val SubtitleReferenceHeight: Dp = 412.dp

/** `SubtitleView.DEFAULT_BOTTOM_PADDING_FRACTION` (0.08) times two-thirds, as the player sets it. */
private const val SubtitleBaseBottomFraction = 0.08f * 2f / 3f

/** How thick an outline edge is relative to the text. */
private const val OutlineFraction = 0.12f
