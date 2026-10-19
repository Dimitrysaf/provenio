package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import io.github.dimitrysaf.provenio.core.build.supportsSubtitleAutoSync
import io.github.dimitrysaf.provenio.core.build.supportsVolumeStabilizer
import io.github.dimitrysaf.provenio.core.build.volumeStabilizerNeedsMpv
import io.github.dimitrysaf.provenio.core.playback.AndroidPlaybackEngine
import io.github.dimitrysaf.provenio.core.playback.PlayerSettingsUiState
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.*

/** Why a player feature is off whatever its switch says. */
internal enum class PlayerFeatureBlock {
    /** This device's player cannot do it at all. */
    Device,

    /** Videos open in another app, where the feature does not reach. */
    ExternalPlayer,

    /** The volume stabilizer needs the mpv engine on this Android version. */
    StabilizerEngine,
}

/** What keeps the volume stabilizer off with [settings], or null when it can run. */
internal fun volumeStabilizerBlock(settings: PlayerSettingsUiState): PlayerFeatureBlock? = when {
    !supportsVolumeStabilizer -> PlayerFeatureBlock.Device
    settings.externalPlayerEnabled -> PlayerFeatureBlock.ExternalPlayer
    volumeStabilizerNeedsMpv && settings.androidPlaybackEngine != AndroidPlaybackEngine.Libmpv ->
        PlayerFeatureBlock.StabilizerEngine
    else -> null
}

/** What keeps automatic subtitle sync off with [settings], or null when it can run. */
internal fun subtitleAutoSyncBlock(settings: PlayerSettingsUiState): PlayerFeatureBlock? = when {
    !supportsSubtitleAutoSync -> PlayerFeatureBlock.Device
    settings.externalPlayerEnabled -> PlayerFeatureBlock.ExternalPlayer
    else -> null
}

/** Says why [feature] is off and what would turn it back on. */
@Composable
internal fun PlayerFeatureBlockDialog(
    feature: String,
    block: PlayerFeatureBlock,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settings_unavailable_title, feature)) },
        text = {
            Text(
                when (block) {
                    PlayerFeatureBlock.Device -> stringResource(Res.string.settings_unavailable_device, feature)
                    PlayerFeatureBlock.ExternalPlayer ->
                        stringResource(Res.string.settings_unavailable_external_player, feature)
                    PlayerFeatureBlock.StabilizerEngine -> stringResource(Res.string.settings_unavailable_stabilizer_engine)
                },
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_ok))
            }
        },
    )
}
