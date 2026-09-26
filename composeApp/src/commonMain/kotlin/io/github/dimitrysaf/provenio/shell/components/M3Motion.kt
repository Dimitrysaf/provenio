package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.dp

// Material 3 motion tokens and the transition patterns built from them.
internal object M3Motion {
    val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    val SharedAxisSlide = 30.dp

    private const val SharedAxisMillis = 400
    private const val SharedAxisFadeOutMillis = 140
    private const val FadeThroughOutMillis = 90
    private const val FadeThroughInMillis = 210

    // Shared X axis, for moving forward or back between screens of the same hierarchy.
    fun sharedAxisX(forward: Boolean, slidePx: Int): ContentTransform {
        val direction = if (forward) 1 else -1
        val enter = slideInHorizontally(tween(SharedAxisMillis, easing = Emphasized)) { direction * slidePx } +
            fadeIn(tween(SharedAxisMillis - SharedAxisFadeOutMillis, delayMillis = SharedAxisFadeOutMillis, easing = EmphasizedDecelerate))
        val exit = slideOutHorizontally(tween(SharedAxisMillis, easing = Emphasized)) { -direction * slidePx } +
            fadeOut(tween(SharedAxisFadeOutMillis, easing = EmphasizedAccelerate))
        return (enter togetherWith exit).apply { if (!forward) targetContentZIndex = -1f }
    }

    // Fade through, for switching between destinations that are not related, like top-level tabs.
    fun fadeThrough(): ContentTransform =
        (fadeIn(fadeThroughInSpec()) + scaleIn(fadeThroughInSpec(), initialScale = 0.92f)) togetherWith
            fadeOut(fadeThroughOutSpec())

    fun <T> fadeThroughInSpec() = tween<T>(FadeThroughInMillis, delayMillis = FadeThroughOutMillis, easing = EmphasizedDecelerate)

    fun <T> fadeThroughOutSpec() = tween<T>(FadeThroughOutMillis, easing = EmphasizedAccelerate)

    const val PredictiveBackMillis = 500
    val PredictiveBackCorner = 28.dp
    private val StandardDecelerate = CubicBezierEasing(0f, 0f, 0f, 1f)
    private val StandardAccelerate = CubicBezierEasing(0.3f, 0f, 1f, 1f)

    // The first part of the timeline is the back preview the gesture scrubs, the rest is the commit.
    private const val PreviewFraction = 0.3f

    // How far toward the swiped edge the card moves during the preview, as a share of its width, leaving about an 8dp gap.
    private const val PreviewShift = 0.03f

    // The gesture progress runs through standard decelerate, as the back guidelines ask, and then holds.
    val PredictiveBackPreview = Easing { t -> StandardDecelerate.transform((t / PreviewFraction).coerceAtMost(1f)) }

    private val PredictiveBackSlide = Easing { t ->
        if (t < PreviewFraction) {
            PreviewShift * StandardDecelerate.transform(t / PreviewFraction)
        } else {
            PreviewShift + (1f - PreviewShift) * StandardAccelerate.transform((t - PreviewFraction) / (1f - PreviewFraction))
        }
    }

    // The system back card: the page shrinks to 90% toward the swiped edge over the page beneath, then slides off on commit.
    fun predictiveBackCard(fromRightEdge: Boolean): ContentTransform {
        val direction = if (fromRightEdge) -1 else 1
        val exit = scaleOut(tween(PredictiveBackMillis, easing = PredictiveBackPreview), targetScale = 0.9f) +
            slideOutHorizontally(tween(PredictiveBackMillis, easing = PredictiveBackSlide)) { direction * it }
        return (EnterTransition.None togetherWith exit).apply { targetContentZIndex = -1f }
    }
}

// Rounds a page's corners as it leaves, which is what makes the back preview read as a card.
@Composable
internal fun Transition<EnterExitState>.predictiveBackCorners(): Modifier {
    val corner by animateDp(
        transitionSpec = { tween(M3Motion.PredictiveBackMillis, easing = M3Motion.PredictiveBackPreview) },
        label = "predictiveBackCorner",
    ) { state -> if (state == EnterExitState.PostExit) M3Motion.PredictiveBackCorner else 0.dp }
    return Modifier.graphicsLayer {
        clip = corner > 0.dp
        shape = RoundedCornerShape(corner)
    }
}
