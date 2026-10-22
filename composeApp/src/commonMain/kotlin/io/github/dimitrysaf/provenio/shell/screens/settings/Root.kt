package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilledTonalButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.dimitrysaf.provenio.core.diagnostics.deviceDescription
import io.github.dimitrysaf.provenio.shell.components.SingleChoiceBottomSheet
import io.github.dimitrysaf.provenio.shell.components.SingleChoiceOption
import io.ktor.http.encodeURLParameter
import provenio.composeapp.generated.resources.about_feedback_title
import provenio.composeapp.generated.resources.about_feedback_description
import provenio.composeapp.generated.resources.about_feedback_sheet_description
import provenio.composeapp.generated.resources.about_feedback_bug
import provenio.composeapp.generated.resources.about_feedback_bug_description
import provenio.composeapp.generated.resources.about_feedback_suggestion
import provenio.composeapp.generated.resources.about_feedback_suggestion_description
import provenio.composeapp.generated.resources.about_source_code
import provenio.composeapp.generated.resources.about_contact
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import provenio.composeapp.generated.resources.updates_verifying
import provenio.composeapp.generated.resources.updates_title_available
import io.github.dimitrysaf.provenio.core.build.AppVersionConfig
import io.github.dimitrysaf.provenio.core.diagnostics.AppLogs
import io.github.dimitrysaf.provenio.core.updater.AppUpdaterPlatform
import io.github.dimitrysaf.provenio.core.p2p.P2pSettingsRepository
import io.github.dimitrysaf.provenio.core.tracking.trakt.TraktCommentsSettings
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.settings_imports_section_title
import provenio.composeapp.generated.resources.compose_about_channel_beta
import provenio.composeapp.generated.resources.compose_about_channel_release
import provenio.composeapp.generated.resources.compose_about_version_format
import provenio.composeapp.generated.resources.compose_settings_page_licenses_attributions
import provenio.composeapp.generated.resources.compose_settings_page_privacy_policy
import provenio.composeapp.generated.resources.compose_settings_root_check_updates_description
import provenio.composeapp.generated.resources.compose_settings_root_check_updates_title
import provenio.composeapp.generated.resources.compose_settings_root_privacy_policy_description
import provenio.composeapp.generated.resources.compose_settings_root_switch_profile_description
import provenio.composeapp.generated.resources.compose_settings_root_switch_profile_title
import provenio.composeapp.generated.resources.compose_settings_root_about_section
import provenio.composeapp.generated.resources.compose_settings_root_advanced_section
import provenio.composeapp.generated.resources.compose_settings_page_tracking
import provenio.composeapp.generated.resources.settings_integrations_section_title
import provenio.composeapp.generated.resources.about_licenses_attributions_subtitle
import provenio.composeapp.generated.resources.keyboard_shortcuts_entry_description
import provenio.composeapp.generated.resources.keyboard_shortcuts_title
import org.jetbrains.compose.resources.stringResource

private const val PRIVACY_POLICY_URL = "https://github.com/Dimitrysaf/provenio#privacy"
private const val REPOSITORY_URL = "https://github.com/Dimitrysaf/provenio"
private const val NEW_ISSUE_URL = "$REPOSITORY_URL/issues/new"
// The plus is escaped so every mail app reads it as part of the address.
private const val CONTACT_MAIL_URI = "mailto:demetresmeliates%2Bcontact@gmail.com"

/** The categories this build shows: Plugins only where plugins are on, Server only where P2P streaming exists. */
internal fun visibleSettingsCategories(pluginsEnabled: Boolean): List<SettingsCategory> =
    SettingsCategory.entries.filter { category ->
        when (category) {
            SettingsCategory.Plugins -> pluginsEnabled
            SettingsCategory.Server -> P2pSettingsRepository.isVisible
            else -> true
        }
    }

/**
 * The phone's first Settings page: the categories under Account, User interface, Content and
 * About, one row each, opening its page or screen.
 */
internal fun LazyListScope.settingsCategoryListContent(
    categories: List<SettingsCategory>,
    onCategoryClick: (SettingsCategory) -> Unit,
    onSwitchProfile: (() -> Unit)? = null,
) {
    groupedSettingsCategories(categories).forEach { (section, sectionCategories) ->
        item(key = "settings-root-${section.name}") {
            SettingsSection(
                title = stringResource(section.labelRes),
                isTablet = false,
            ) {
                val rows = sectionCategories.map { category ->
                    Triple(category, stringResource(category.labelRes), stringResource(category.descriptionRes))
                }
                val switchProfileTitle = stringResource(Res.string.compose_settings_root_switch_profile_title)
                val switchProfileDescription = stringResource(Res.string.compose_settings_root_switch_profile_description)
                SettingsList {
                    rows.forEach { (category, title, description) ->
                        navigationRow(
                            title = title,
                            description = description,
                            icon = category.icon,
                            iconHue = category.iconHue,
                            onClick = { onCategoryClick(category) },
                        )
                    }
                    if (section == SettingsRootSection.Account && onSwitchProfile != null) {
                        navigationRow(
                            title = switchProfileTitle,
                            description = switchProfileDescription,
                            icon = Icons.Rounded.People,
                            iconHue = SettingsIconHue.Blue,
                            onClick = onSwitchProfile,
                        )
                    }
                }
            }
        }
    }
}

/**
 * The Accounts page, in two sections: Tracking, with the tracking cards and their choices, and
 * Integrations, with a card for each service that needs a key.
 */
internal fun LazyListScope.accountsSettingsContent(
    isTablet: Boolean,
    data: SettingsData,
    onTmdbClick: () -> Unit,
    onMdbListClick: () -> Unit,
    onStreamingAvailabilityClick: () -> Unit,
    onDebridClick: () -> Unit,
) {
    item(key = "accounts-tracking-heading") {
        AccountsSectionHeading(stringResource(Res.string.compose_settings_page_tracking))
    }
    trackingSettingsContent(
        isTablet = isTablet,
        traktUiState = data.traktAuth,
        simklUiState = data.simklAuth,
        settingsUiState = data.trackingSettings,
        commentsEnabled = data.traktCommentsEnabled,
        onCommentsEnabledChange = TraktCommentsSettings::setEnabled,
    )
    item(key = "accounts-integrations-heading") {
        AccountsSectionHeading(stringResource(Res.string.settings_integrations_section_title))
    }
    integrationCardsContent(
        isTablet = isTablet,
        tmdbSettings = data.tmdbSettings,
        mdbListSettings = data.mdbListSettings,
        streamingAvailabilitySettings = data.streamingAvailabilitySettings,
        debridSettings = data.debridSettings,
        onTmdbClick = onTmdbClick,
        onMdbListClick = onMdbListClick,
        onStreamingAvailabilityClick = onStreamingAvailabilityClick,
        onDebridClick = onDebridClick,
    )
    // Last, so bringing data over from another app, done once, stays out of the way after.
    if (data.showImports) {
        item(key = "accounts-imports-heading") {
            AccountsSectionHeading(stringResource(Res.string.settings_imports_section_title))
        }
        item(key = "accounts-imports-stremio") {
            SettingsSection(title = StremioName, isTablet = isTablet) {
                StremioImportList()
            }
        }
    }
}

/** A heading over one of the Accounts page's sections, a size above the sections inside them. */
@Composable
private fun AccountsSectionHeading(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 12.dp),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
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

/**
 * The About page: privacy, licenses, updates and a way to report a problem, with the app's name,
 * version, its source code and a way to write in under them.
 */
internal fun LazyListScope.aboutSettingsContent(
    isTablet: Boolean,
    onLicensesAttributionsClick: () -> Unit,
    onCheckForUpdatesClick: (() -> Unit)? = null,
) {
    item {
        val uriHandler = LocalUriHandler.current
        val updater = AppUpdaterStatus.uiState.collectAsStateWithLifecycle().value
        var showFeedbackSheet by remember { mutableStateOf(false) }
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
                    trailingContent = {
                        Icon(imageVector = Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null)
                    },
                    onClick = { uriHandler.openUri(PRIVACY_POLICY_URL) },
                )
                navigationRow(
                    title = stringResource(Res.string.about_feedback_title),
                    description = stringResource(Res.string.about_feedback_description),
                    icon = Icons.Rounded.BugReport,
                    onClick = { showFeedbackSheet = true },
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
                            updater.isVerifying -> stringResource(Res.string.updates_verifying)
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
        if (showFeedbackSheet) {
            val bugReportBody = bugReportBody()
            SingleChoiceBottomSheet(
                title = stringResource(Res.string.about_feedback_title),
                description = stringResource(Res.string.about_feedback_sheet_description),
                options = listOf(
                    SingleChoiceOption(
                        value = FeedbackKind.Bug,
                        label = stringResource(Res.string.about_feedback_bug),
                        supportingText = stringResource(Res.string.about_feedback_bug_description),
                    ),
                    SingleChoiceOption(
                        value = FeedbackKind.Suggestion,
                        label = stringResource(Res.string.about_feedback_suggestion),
                        supportingText = stringResource(Res.string.about_feedback_suggestion_description),
                    ),
                ),
                isSelected = { false },
                onSelected = { kind ->
                    uriHandler.openUri(
                        when (kind) {
                            FeedbackKind.Bug ->
                                "$NEW_ISSUE_URL?template=bug_report.md&body=${bugReportBody.encodeURLParameter()}"
                            FeedbackKind.Suggestion -> "$NEW_ISSUE_URL?template=suggestion.md"
                        },
                    )
                },
                onDismiss = { showFeedbackSheet = false },
            )
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
            val uriHandler = LocalUriHandler.current
            Row(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = if (isTablet) 16.dp else 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(onClick = { uriHandler.openUri(REPOSITORY_URL) }) {
                    Text(stringResource(Res.string.about_source_code))
                }
                FilledTonalButton(onClick = { uriHandler.openUri(CONTACT_MAIL_URI) }) {
                    Text(stringResource(Res.string.about_contact))
                }
            }
        }
    }
}

private enum class FeedbackKind { Bug, Suggestion }

/**
 * The bug report template's text with the version and device already filled in. GitHub takes
 * the template's labels either way; whether it shows this or the template's own text, the
 * sections are the same.
 */
@Composable
private fun bugReportBody(): String {
    val version = stringResource(
        Res.string.compose_about_version_format,
        AppVersionConfig.VERSION_NAME,
        AppVersionConfig.VERSION_CODE,
    )
    return "**What happened?**\n\n\n**Steps to reproduce**\n\n\n**App version and device**\n" +
        "Provenio $version, ${deviceDescription()}\n"
}
