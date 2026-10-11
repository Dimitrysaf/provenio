package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.provenio.core.network.NetworkDiagnosticsRepository
import io.github.dimitrysaf.provenio.core.network.SpeedTestPhase
import io.github.dimitrysaf.provenio.core.network.SpeedTestState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.sin
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.*

/**
 * The speed test, in a dialog: a gauge that follows the speed being measured, download in the
 * primary colour and upload in the tertiary one, the phase it is in, and the four results under
 * it. Closing the dialog while it runs stops the test.
 */
@Composable
internal fun SpeedTestDialog(onDismiss: () -> Unit) {
    val state = NetworkDiagnosticsRepository.speedTest.collectAsStateWithLifecycle().value
    val running = state as? SpeedTestState.Running
    val result = state.result
    val phase = running?.phase
    val measuredMbps = when {
        phase == SpeedTestPhase.Upload -> result.uploadMbps
        phase == SpeedTestPhase.Download -> result.downloadMbps
        phase == SpeedTestPhase.Latency -> null
        else -> result.downloadMbps ?: result.uploadMbps
    }
    val gaugeColor = if (phase == SpeedTestPhase.Upload) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.primary
    }
    val phaseLabel = when {
        phase == SpeedTestPhase.Latency -> stringResource(Res.string.settings_network_speed_running_latency)
        phase == SpeedTestPhase.Download -> stringResource(Res.string.settings_network_download)
        phase == SpeedTestPhase.Upload -> stringResource(Res.string.settings_network_upload)
        state is SpeedTestState.Failed -> state.reason?.let {
            stringResource(Res.string.settings_network_speed_failed_reason, it)
        } ?: stringResource(Res.string.settings_network_speed_failed)
        state is SpeedTestState.Done && result.downloadMbps == null -> stringResource(Res.string.settings_network_upload)
        state is SpeedTestState.Done -> stringResource(Res.string.settings_network_download)
        else -> stringResource(Res.string.settings_network_speed_hint)
    }
    val close = {
        if (running != null) NetworkDiagnosticsRepository.cancelSpeedTest()
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = close,
        title = { Text(stringResource(Res.string.settings_network_section_speed)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SpeedGauge(
                    mbps = measuredMbps ?: 0.0,
                    color = gaugeColor,
                    showValue = measuredMbps != null,
                    label = phaseLabel,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SpeedResult(
                        label = stringResource(Res.string.settings_network_speed_ping),
                        value = result.latencyMs?.let { stringResource(Res.string.settings_network_ms_value, it) },
                        modifier = Modifier.weight(1f),
                    )
                    SpeedResult(
                        label = stringResource(Res.string.settings_network_jitter),
                        value = result.jitterMs?.let { stringResource(Res.string.settings_network_ms_value, it) },
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SpeedResult(
                        label = stringResource(Res.string.settings_network_download),
                        value = result.downloadMbps?.let { stringResource(Res.string.settings_network_mbps_value, formatMbps(it)) },
                        modifier = Modifier.weight(1f),
                    )
                    SpeedResult(
                        label = stringResource(Res.string.settings_network_upload),
                        value = result.uploadMbps?.let { stringResource(Res.string.settings_network_mbps_value, formatMbps(it)) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (running != null) {
                        NetworkDiagnosticsRepository.cancelSpeedTest()
                    } else {
                        NetworkDiagnosticsRepository.runSpeedTest()
                    }
                },
            ) {
                Text(
                    stringResource(
                        when (state) {
                            is SpeedTestState.Running -> Res.string.settings_network_speed_stop
                            SpeedTestState.Idle -> Res.string.settings_network_speed_run
                            is SpeedTestState.Done, is SpeedTestState.Failed -> Res.string.settings_network_speed_again
                        },
                    ),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = close) {
                Text(stringResource(Res.string.action_close))
            }
        },
    )
}

/**
 * A 240° gauge on a logarithmic scale up to 1 Gbit/s, so slow connections still move it, with
 * the speed in the middle and what is being measured under it.
 */
@Composable
private fun SpeedGauge(mbps: Double, color: Color, showValue: Boolean, label: String) {
    val fraction by animateFloatAsState(
        targetValue = gaugeFraction(mbps),
        animationSpec = tween(durationMillis = 250),
        label = "speedGauge",
    )
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val tickColor = MaterialTheme.colorScheme.onSurfaceVariant
    val measurer = rememberTextMeasurer()
    val tickStyle = MaterialTheme.typography.labelSmall.copy(color = tickColor)
    Box(
        modifier = Modifier
            .widthIn(max = 280.dp)
            .fillMaxWidth()
            .aspectRatio(1.15f),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val stroke = 18.dp.toPx()
            val diameter = min(size.width, size.height * 1.15f) - stroke - 28.dp.toPx()
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f + 8.dp.toPx())
            val arcSize = Size(diameter, diameter)
            drawArc(
                color = trackColor,
                startAngle = GaugeStartAngle,
                sweepAngle = GaugeSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            if (fraction > 0f) {
                drawArc(
                    color = color,
                    startAngle = GaugeStartAngle,
                    sweepAngle = GaugeSweep * fraction,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
            val center = Offset(topLeft.x + diameter / 2f, topLeft.y + diameter / 2f)
            val labelRadius = diameter / 2f + stroke / 2f + 12.dp.toPx()
            GaugeTicks.forEach { tick ->
                val angle = (GaugeStartAngle + GaugeSweep * gaugeFraction(tick.toDouble())) * PI / 180.0
                val layout = measurer.measure(tick.toString(), tickStyle)
                val x = center.x + labelRadius * cos(angle).toFloat() - layout.size.width / 2f
                val y = center.y + labelRadius * sin(angle).toFloat() - layout.size.height / 2f
                drawText(layout, topLeft = Offset(x, y))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (showValue) formatMbps(mbps) else "—",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Mbit/s",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = label,
                modifier = Modifier.padding(top = 8.dp, start = 24.dp, end = 24.dp),
                style = MaterialTheme.typography.bodySmall.merge(TextStyle(textAlign = TextAlign.Center)),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
            )
        }
    }
}

@Composable
private fun SpeedResult(label: String, value: String?, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value ?: "—",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** Where [mbps] sits on the gauge, from 0 to 1, on a logarithmic scale that ends at 1000. */
private fun gaugeFraction(mbps: Double): Float =
    (log10(1.0 + mbps.coerceAtLeast(0.0)) / log10(1.0 + GaugeMaxMbps)).toFloat().coerceIn(0f, 1f)

private const val GaugeMaxMbps = 1000.0
private const val GaugeStartAngle = 150f
private const val GaugeSweep = 240f
private val GaugeTicks = listOf(0, 5, 10, 50, 100, 250, 500, 1000)
