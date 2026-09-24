package com.nuvio.app.shell.screens.profiles

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import com.nuvio.app.core.profiles.PinVerifyResult

/** Every PIN in the app is four digits, so every reading of one is four dots long. */
private const val PIN_LENGTH = 4

/**
 * Entering a PIN: the dots, whatever went wrong, and the keys.
 *
 * The keypad commits on its own, on the fourth digit, so nothing outside it has a button to
 * press: it reports the PIN it was given and says what came back.
 */
@Composable
internal fun PinEntryBody(
    verify: suspend (String) -> PinVerifyResult,
    onVerified: (String) -> Unit,
    onFailed: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pin by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PinDots(entered = pin.length)

        Spacer(modifier = Modifier.height(24.dp))

        PinKeypad(
            enabled = !isVerifying,
            onDigit = { digit ->
                if (pin.length >= PIN_LENGTH || isVerifying) return@PinKeypad
                pin += digit
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                if (pin.length < PIN_LENGTH) return@PinKeypad
                val entered = pin
                isVerifying = true
                scope.launch {
                    val result = verify(entered)
                    if (result.unlocked) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onVerified(entered)
                    } else {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        // What went wrong is worth interrupting for, so the sheet raises it as a
                        // dialog rather than tucking a line of red under the dots.
                        onFailed(
                            result.message
                                ?: if (result.retryAfterSeconds > 0) {
                                    getString(Res.string.pin_locked_try_again, result.retryAfterSeconds)
                                } else {
                                    getString(Res.string.pin_incorrect)
                                },
                        )
                        pin = ""
                    }
                    isVerifying = false
                }
            },
            onBackspace = {
                if (pin.isEmpty() || isVerifying) return@PinKeypad
                pin = pin.dropLast(1)
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            },
        )
    }
}

/**
 * How much of the PIN has been given, without giving away what it is.
 *
 * Material has no component for this, so it is drawn: a filled dot for a digit in hand and an
 * outlined one for a digit still wanted, in the scheme's own roles.
 */
@Composable
private fun PinDots(entered: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(PIN_LENGTH) { index ->
            val filled = index < entered
            val color = if (filled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
            // A digit landing is worth seeing, since the dot is the only thing that confirms it.
            val scale = remember { Animatable(1f) }
            LaunchedEffect(filled) {
                if (filled) {
                    scale.snapTo(1.5f)
                    scale.animateTo(
                        1f,
                        spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessHigh,
                        ),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                    }
                    .size(PinDotSize)
                    .clip(CircleShape)
                    .then(
                        if (filled) {
                            Modifier.background(color)
                        } else {
                            Modifier.border(2.dp, color, CircleShape)
                        },
                    ),
            )
        }
    }
}

/**
 * The keys.
 *
 * Each one is a tonal icon button rather than a drawn circle, so pressing a key has the state
 * layer, the ripple and the disabled treatment every other button in the app has, and the empty
 * corner of the last row is a gap rather than a key that does nothing.
 */
@Composable
private fun PinKeypad(
    enabled: Boolean,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(PinKeySpacing)) {
        PinKeyRows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(PinKeySpacing)) {
                row.forEach { key ->
                    when (key) {
                        "" -> Spacer(modifier = Modifier.size(PinKeySize))
                        PinBackspaceKey -> FilledTonalIconButton(
                            onClick = onBackspace,
                            modifier = Modifier.size(PinKeySize),
                            enabled = enabled,
                            shape = CircleShape,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Backspace,
                                contentDescription = stringResource(Res.string.pin_backspace),
                            )
                        }
                        else -> FilledTonalIconButton(
                            onClick = { onDigit(key) },
                            modifier = Modifier.size(PinKeySize),
                            enabled = enabled,
                            shape = CircleShape,
                        ) {
                            Text(
                                text = key,
                                style = MaterialTheme.typography.headlineSmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val PinBackspaceKey = "backspace"

private val PinKeyRows = listOf(
    listOf("1", "2", "3"),
    listOf("4", "5", "6"),
    listOf("7", "8", "9"),
    listOf("", "0", PinBackspaceKey),
)

private val PinKeySize = 60.dp
private val PinKeySpacing = 12.dp
private val PinDotSize = 16.dp
