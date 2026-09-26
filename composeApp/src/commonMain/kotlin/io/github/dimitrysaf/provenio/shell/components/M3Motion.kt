package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
    const val PredictiveBackMillis = 350

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

    // Android's predictive back curve, applied to the raw gesture progress so the screen answers the finger at once.
    val PredictiveBack = CubicBezierEasing(0.1f, 0.1f, 0f, 1f)

    // Where the curve reaches 35%, the point at which the exiting screen is gone and the entering one starts to appear.
    private const val PredictiveBackFadeFraction = 0.094f

    // The full screen back pattern: the old screen scales to 90% and fades out, then the previous one fades in from 110%.
    fun predictiveBack(): ContentTransform {
        val fadeMillis = (PredictiveBackMillis * PredictiveBackFadeFraction).toInt()
        val enter = scaleIn(tween(PredictiveBackMillis, easing = PredictiveBack), initialScale = 1.1f) +
            fadeIn(tween(PredictiveBackMillis - fadeMillis, delayMillis = fadeMillis, easing = PredictiveBack))
        val exit = scaleOut(tween(PredictiveBackMillis, easing = PredictiveBack), targetScale = 0.9f) +
            fadeOut(tween(fadeMillis, easing = LinearEasing))
        return enter togetherWith exit
    }
}

