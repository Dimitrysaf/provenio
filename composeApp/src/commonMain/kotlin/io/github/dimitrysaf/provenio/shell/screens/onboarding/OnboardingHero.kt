@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.github.dimitrysaf.provenio.shell.screens.onboarding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import io.github.dimitrysaf.provenio.shell.screens.settings.AppBrandSymbol

/**
 * A step's picture: one of Material's expressive shapes, turning slowly behind what the step is
 * about, in a colour of its own. The shape turns and the picture on it stays upright.
 */
@Composable
internal fun OnboardingHero(
    step: OnboardingStep,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "OnboardingHero")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(HeroTurnMillis, easing = LinearEasing), RepeatMode.Restart),
        label = "OnboardingHeroRotation",
    )
    val shape = when (step) {
        OnboardingStep.Welcome -> MaterialShapes.Cookie12Sided
        OnboardingStep.Appearance -> MaterialShapes.Flower
        OnboardingStep.Profile -> MaterialShapes.Cookie9Sided
        OnboardingStep.Addons -> MaterialShapes.SoftBurst
        OnboardingStep.Tracking -> MaterialShapes.Sunny
        OnboardingStep.Done -> MaterialShapes.Cookie12Sided
    }.toShape()
    val colors = MaterialTheme.colorScheme
    val (container, content) = when (step) {
        OnboardingStep.Welcome, OnboardingStep.Done -> colors.primaryContainer to colors.onPrimaryContainer
        OnboardingStep.Appearance, OnboardingStep.Tracking -> colors.tertiaryContainer to colors.onTertiaryContainer
        OnboardingStep.Profile, OnboardingStep.Addons -> colors.secondaryContainer to colors.onSecondaryContainer
    }
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer { rotationZ = rotation }
                .clip(shape)
                .background(container),
        )
        if (step == OnboardingStep.Welcome) {
            AppBrandSymbol(modifier = Modifier.size(size * 0.46f))
        } else {
            Icon(
                imageVector = when (step) {
                    OnboardingStep.Appearance -> Icons.Rounded.Palette
                    OnboardingStep.Profile -> Icons.Rounded.People
                    OnboardingStep.Addons -> Icons.Rounded.Extension
                    OnboardingStep.Tracking -> Icons.Rounded.Sync
                    else -> Icons.Rounded.DoneAll
                },
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(size * 0.4f),
            )
        }
    }
}

private const val HeroTurnMillis = 30_000
