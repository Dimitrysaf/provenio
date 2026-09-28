package io.github.dimitrysaf.provenio.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.provenio.core.playback.PlayerSettingsRepository
import io.github.dimitrysaf.provenio.shell.nav.Navigator
import io.github.dimitrysaf.provenio.shell.nav.PlayerRoute
import io.github.dimitrysaf.provenio.shell.nav.StreamRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal object DetachedPlayer {
    private val _route = MutableStateFlow<PlayerRoute?>(null)
    val route: StateFlow<PlayerRoute?> = _route.asStateFlow()

    val isEnabled: Boolean
        get() {
            if (!playerWindowSupported) return false
            PlayerSettingsRepository.ensureLoaded()
            return PlayerSettingsRepository.uiState.value.playerInSeparateWindow
        }

    fun open(route: PlayerRoute) {
        _route.value = route
    }

    fun close(route: PlayerRoute) {
        if (_route.value == route) _route.value = null
    }

    fun closeAny() {
        _route.value = null
    }
}

internal fun Navigator.openPlayer(route: PlayerRoute, replaceStreamRoute: Boolean = false) {
    if (!DetachedPlayer.isEnabled) {
        DetachedPlayer.closeAny()
        navigate(route) {
            if (replaceStreamRoute) {
                popUpTo<StreamRoute> { inclusive = true }
            }
        }
        return
    }
    if (replaceStreamRoute && currentRoute is StreamRoute) popBackStack()
    DetachedPlayer.open(route)
}

@Composable
internal fun DetachedPlayerHost(
    navController: Navigator,
    playback: AppPlayback,
    externalPlayerId: String?,
    profileId: Int,
) {
    var lastProfileId by remember { mutableStateOf(profileId) }
    LaunchedEffect(profileId) {
        if (profileId != lastProfileId) {
            lastProfileId = profileId
            DetachedPlayer.closeAny()
        }
    }
    val detachedRoute by DetachedPlayer.route.collectAsStateWithLifecycle()
    val route = detachedRoute ?: return
    key(route.launchId) {
        DetachedPlayerWindow(
            title = route.title,
            onCloseRequest = { DetachedPlayer.close(route) },
        ) {
            DisposableEffect(route) {
                onDispose { disposeRouteResources(route) }
            }
            PlayerDestination(
                route = route,
                navController = navController,
                externalPlayerId = externalPlayerId,
                externalPlayerNotConfiguredText = playback.strings.externalPlayerNotConfigured,
                externalPlayerFailedText = playback.strings.externalPlayerFailed,
                onExternalPlayerLaunch = playback.recordExternalLaunch,
                launchExternalPlayer = playback.launchExternalPlayer,
                openExternalStreamUrl = playback::openExternalStreamUrl,
                onClose = { DetachedPlayer.close(route) },
            )
        }
    }
}

internal expect val playerWindowSupported: Boolean

@Composable
internal expect fun DetachedPlayerWindow(
    title: String,
    onCloseRequest: () -> Unit,
    content: @Composable () -> Unit,
)
