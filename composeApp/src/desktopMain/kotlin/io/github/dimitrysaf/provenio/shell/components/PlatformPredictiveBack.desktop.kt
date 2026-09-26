package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow

@Composable
internal actual fun PlatformPredictiveBackHandler(
    enabled: Boolean,
    onBack: suspend (Flow<BackGestureEvent>) -> Unit,
) = Unit
