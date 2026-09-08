package io.github.dimitrysaf.provenio.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

/**
 * Material 3 easing and duration tokens.
 *
 * These normally come from `MaterialTheme.motionScheme`, but every API tagged
 * `ExperimentalMaterial3ExpressiveApi` — `MotionScheme` and `MaterialExpressiveTheme`
 * among them — is `internal` in the material3 build that Compose Multiplatform 1.12.0
 * resolves (1.12.0-alpha03), so the scheme cannot be read or installed from here. The
 * spec values are declared directly instead; swap this file for `MaterialTheme.motionScheme`
 * once those APIs are public.
 *
 * https://m3.material.io/styles/motion/easing-and-duration/tokens-specs
 */
object MotionTokens {

    /**
     * M3 publishes "emphasized" as a two-part curve that a single cubic bezier cannot
     * express exactly; (0.2, 0, 0, 1) is the documented single-curve form, shared with
     * "standard".
     */
    val Standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val StandardDecelerate: Easing = CubicBezierEasing(0f, 0f, 0f, 1f)
    val StandardAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 1f, 1f)
    val Emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val EmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    const val DurationShort1 = 50
    const val DurationShort2 = 100
    const val DurationShort3 = 150
    const val DurationShort4 = 200
    const val DurationMedium1 = 250
    const val DurationMedium2 = 300
    const val DurationMedium3 = 350
    const val DurationMedium4 = 400
    const val DurationLong1 = 450
    const val DurationLong2 = 500
}
