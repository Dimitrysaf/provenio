package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import io.github.dimitrysaf.provenio.core.build.supportsSubtitleAutoSync
import io.github.dimitrysaf.provenio.core.build.supportsSubtitleFonts
import io.github.dimitrysaf.provenio.core.build.supportsSubtitleShadow
import io.github.dimitrysaf.provenio.core.build.supportsSubtitleShadowAngle
import io.github.dimitrysaf.provenio.core.build.supportsSubtitleShadowSoftness
import io.github.dimitrysaf.provenio.core.build.supportsVolumeStabilizer
import io.github.dimitrysaf.provenio.core.build.volumeStabilizerNeedsMpv
import io.github.dimitrysaf.provenio.core.playback.AndroidPlaybackEngine
import io.github.dimitrysaf.provenio.core.playback.PlayerSettingsUiState
import io.github.dimitrysaf.provenio.core.playback.SubtitleFontFamily
import io.github.dimitrysaf.provenio.core.playback.SubtitleStyleState
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

/** What keeps the subtitle font choice off with [settings], or null when it can apply. */
internal fun subtitleFontBlock(settings: PlayerSettingsUiState): PlayerFeatureBlock? = when {
    !supportsSubtitleFonts -> PlayerFeatureBlock.Device
    settings.externalPlayerEnabled -> PlayerFeatureBlock.ExternalPlayer
    else -> null
}

/** What keeps the subtitle shadow off with [settings], or null when it can be drawn. */
internal fun subtitleShadowBlock(settings: PlayerSettingsUiState): PlayerFeatureBlock? = when {
    !supportsSubtitleShadow -> PlayerFeatureBlock.Device
    settings.externalPlayerEnabled -> PlayerFeatureBlock.ExternalPlayer
    else -> null
}

/** What keeps the shadow falling only down and to the right with [settings], or null when it can turn. */
internal fun subtitleShadowAngleBlock(settings: PlayerSettingsUiState): PlayerFeatureBlock? =
    subtitleShadowBlock(settings) ?: PlayerFeatureBlock.Device.takeIf { !supportsSubtitleShadowAngle }

/** What keeps the shadow's edge sharp with [settings], or null when it can be softened. */
internal fun subtitleShadowSoftnessBlock(settings: PlayerSettingsUiState): PlayerFeatureBlock? =
    subtitleShadowBlock(settings) ?: PlayerFeatureBlock.Device.takeIf { !supportsSubtitleShadowSoftness }

/**
 * The subtitle style as the player will draw it here: options it cannot apply fall back to what it
 * draws instead, so the settings preview never shows a shadow turned a way the player won't turn it.
 */
internal fun PlayerSettingsUiState.drawnSubtitleStyle(): SubtitleStyleState {
    val style = subtitleStyle
    return style.copy(
        fontFamily = if (subtitleFontBlock(this) == null) style.fontFamily else SubtitleFontFamily.Default,
        shadowEnabled = style.shadowEnabled && subtitleShadowBlock(this) == null,
        shadowAngle = if (subtitleShadowAngleBlock(this) == null) style.shadowAngle else SubtitleStyleState.DEFAULT.shadowAngle,
        shadowSoftness = if (subtitleShadowSoftnessBlock(this) == null) style.shadowSoftness else 0,
    )
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
