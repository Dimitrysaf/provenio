package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.provenio.shell.screens.updater.AppUpdaterStatus
import provenio.composeapp.generated.resources.updates_downloading_progress
import provenio.composeapp.generated.resources.updates_message_ready
import provenio.composeapp.generated.resources.updates_preparing_download
import provenio.composeapp.generated.resources.updates_title_available
import io.github.dimitrysaf.provenio.core.build.AppVersionConfig
import io.github.dimitrysaf.provenio.core.diagnostics.AppLogs
import io.github.dimitrysaf.provenio.core.updater.AppUpdaterPlatform
import io.github.dimitrysaf.provenio.core.p2p.P2pSettingsRepository
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.compose_about_channel_beta
import provenio.composeapp.generated.resources.compose_about_channel_release
import provenio.composeapp.generated.resources.compose_about_version_format
import provenio.composeapp.generated.resources.settings_category_profile_tracking
import provenio.composeapp.generated.resources.compose_settings_page_licenses_attributions
import provenio.composeapp.generated.resources.compose_settings_page_privacy_policy
import provenio.composeapp.generated.resources.compose_settings_root_check_updates_description
import provenio.composeapp.generated.resources.compose_settings_root_check_updates_title
import provenio.composeapp.generated.resources.compose_settings_root_privacy_policy_description
import provenio.composeapp.generated.resources.compose_settings_root_switch_profile_description
import provenio.composeapp.generated.resources.compose_settings_root_switch_profile_title
import provenio.composeapp.generated.resources.compose_settings_root_tracking_description
import provenio.composeapp.generated.resources.compose_settings_root_about_section
import provenio.composeapp.generated.resources.compose_settings_root_advanced_section
import provenio.composeapp.generated.resources.compose_settings_page_tracking
import provenio.composeapp.generated.resources.about_licenses_attributions_subtitle
import provenio.composeapp.generated.resources.keyboard_shortcuts_entry_description
import provenio.composeapp.generated.resources.keyboard_shortcuts_title
import org.jetbrains.compose.resources.stringResource

private const val PRIVACY_POLICY_URL = "https://github.com/Dimitrysaf/provenio#privacy"

/** The categories this build shows: Plugins only where plugins are on, Server only where P2P streaming exists. */
internal fun visibleSettingsCategories(pluginsEnabled: Boolean): List<SettingsCategory> =
    SettingsCategory.entries.filter { category ->
        when (category) {
            SettingsCategory.Plugins -> pluginsEnabled
            SettingsCategory.Server -> P2pSettingsRepository.isVisible
            else -> true
        }
    }

/** The phone's first Settings page: one row per category, each opening its page or screen. */
internal fun LazyListScope.settingsCategoryListContent(
    categories: List<SettingsCategory>,
    onCategoryClick: (SettingsCategory) -> Unit,
) {
    item {
        SettingsSection(
            title = "",
            isTablet = false,
            showTitle = false,
        ) {
            val rows = categories.map { category ->
                Triple(category, stringResource(category.labelRes), stringResource(category.descriptionRes))
            }
            SettingsList {
                rows.forEach { (category, title, description) ->
                    navigationRow(
                        title = title,
                        description = description,
                        icon = category.icon,
                        onClick = { onCategoryClick(category) },
                    )
                }
            }
        }
    }
}

/** The Accounts page: profiles and tracking, then the services that need a key or a login. */
internal fun LazyListScope.accountsSettingsContent(
    isTablet: Boolean,
    onSwitchProfileClick: (() -> Unit)?,
    onTrackingClick: () -> Unit,
    onTmdbClick: () -> Unit,
    onMdbListClick: () -> Unit,
    onStreamingAvailabilityClick: () -> Unit,
    onDebridClick: () -> Unit,
) {
    item {
        SettingsSection(
            title = stringResource(Res.string.settings_category_profile_tracking),
            isTablet = isTablet,
        ) {
            SettingsList {
                if (onSwitchProfileClick != null) {
                    navigationRow(
                        title = stringResource(Res.string.compose_settings_root_switch_profile_title),
                        description = stringResource(Res.string.compose_settings_root_switch_profile_description),
                        icon = Icons.Rounded.People,
                        onClick = onSwitchProfileClick,
                    )
                }
                navigationRow(
                    title = stringResource(Res.string.compose_settings_page_tracking),
                    description = stringResource(Res.string.compose_settings_root_tracking_description),
                    icon = Icons.Default.Sync,
                    opensPage = true,
                    onClick = onTrackingClick,
                )
            }
        }
    }
    integrationsContent(
        isTablet = isTablet,
        onTmdbClick = onTmdbClick,
        onMdbListClick = onMdbListClick,
        onStreamingAvailabilityClick = onStreamingAvailabilityClick,
        onDebridClick = onDebridClick,
    )
}

/** The rows the Advanced page adds above its own settings: keyboard shortcuts and saving logs. */
internal fun LazyListScope.advancedLinksContent(
    isTablet: Boolean,
    onKeyboardShortcutsClick: () -> Unit,
) {
    item {
        val saveLogs = if (AppLogs.isSupported) rememberSaveLogsRow() else null
        SettingsSection(
            title = stringResource(Res.string.compose_settings_root_advanced_section),
            isTablet = isTablet,
        ) {
            SettingsList {
                navigationRow(
                    title = stringResource(Res.string.keyboard_shortcuts_title),
                    description = stringResource(Res.string.keyboard_shortcuts_entry_description),
                    icon = Icons.Rounded.Keyboard,
                    opensPage = true,
                    onClick = onKeyboardShortcutsClick,
                )
                saveLogs?.let { saveLogsRow(it) }
            }
        }
    }
}

/** The About page: privacy, licenses and updates, with the app's name and version under them. */
internal fun LazyListScope.aboutSettingsContent(
    isTablet: Boolean,
    onLicensesAttributionsClick: () -> Unit,
    onCheckForUpdatesClick: (() -> Unit)? = null,
) {
    item {
        val uriHandler = LocalUriHandler.current
        val updater = AppUpdaterStatus.uiState.collectAsStateWithLifecycle().value
        SettingsSection(
            title = stringResource(Res.string.compose_settings_root_about_section),
            isTablet = isTablet,
            showTitle = false,
        ) {
            SettingsList {
                navigationRow(
                    title = stringResource(Res.string.compose_settings_page_privacy_policy),
                    description = stringResource(Res.string.compose_settings_root_privacy_policy_description),
                    icon = Icons.Rounded.Policy,
                    onClick = { uriHandler.openUri(PRIVACY_POLICY_URL) },
                )
                navigationRow(
                    title = stringResource(Res.string.compose_settings_page_licenses_attributions),
                    description = stringResource(Res.string.about_licenses_attributions_subtitle),
                    icon = Icons.Rounded.Info,
                    opensPage = true,
                    onClick = onLicensesAttributionsClick,
                )
                if (onCheckForUpdatesClick != null) {
                    val downloadPercent = updater.downloadProgress?.let { (it * 100).toInt().coerceIn(0, 100) }
                    navigationRow(
                        title = stringResource(Res.string.compose_settings_root_check_updates_title),
                        description = when {
                            updater.isDownloading && downloadPercent != null ->
                                stringResource(Res.string.updates_downloading_progress, downloadPercent)
                            updater.isDownloading -> stringResource(Res.string.updates_preparing_download)
                            updater.downloadedApkPath != null -> stringResource(Res.string.updates_message_ready)
                            updater.isUpdateAvailable -> stringResource(Res.string.updates_title_available)
                            else -> stringResource(Res.string.compose_settings_root_check_updates_description)
                        },
                        icon = Icons.Rounded.CloudDownload,
                        progress = when {
                            updater.isDownloading -> updater.downloadProgress ?: 0f
                            updater.downloadedApkPath != null -> 1f
                            else -> null
                        },
                        onClick = onCheckForUpdatesClick,
                    )
                }
            }
        }
    }
    item {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = if (isTablet) 20.dp else 16.dp),
        ) {
            run {
                AppBrandWordmark(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .height(if (isTablet) 30.dp else 26.dp),
                )
                androidx.compose.foundation.layout.Spacer(
                    modifier = Modifier.height(if (isTablet) 10.dp else 8.dp),
                )
            }
            val channel = stringResource(
                if (AppUpdaterPlatform.isDebugBuild) Res.string.compose_about_channel_beta else Res.string.compose_about_channel_release,
            )
            Text(
                text = listOf(
                    stringResource(
                        Res.string.compose_about_version_format,
                        AppVersionConfig.VERSION_NAME,
                        AppVersionConfig.VERSION_CODE,
                    ),
                    channel,
                ).filter { it.isNotBlank() }.joinToString(" · "),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
