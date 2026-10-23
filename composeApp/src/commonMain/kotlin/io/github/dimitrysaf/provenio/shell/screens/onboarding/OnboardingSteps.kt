@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.github.dimitrysaf.provenio.shell.screens.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.BrightnessLow
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.provenio.core.addons.AddAddonResult
import io.github.dimitrysaf.provenio.core.addons.AddonRepository
import io.github.dimitrysaf.provenio.core.localsync.LocalSyncRepository
import io.github.dimitrysaf.provenio.core.profiles.ProfileRepository
import io.github.dimitrysaf.provenio.core.settings.AppIconOption
import io.github.dimitrysaf.provenio.core.settings.AppLanguage
import io.github.dimitrysaf.provenio.core.settings.ColorPalette
import io.github.dimitrysaf.provenio.core.settings.ThemeMode
import io.github.dimitrysaf.provenio.core.settings.ThemeSettingsRepository
import io.github.dimitrysaf.provenio.core.stremio.StremioImport
import io.github.dimitrysaf.provenio.core.stremio.StremioImportState
import io.github.dimitrysaf.provenio.core.tracking.TrackingProviderRegistry
import io.github.dimitrysaf.provenio.core.tracking.simkl.SimklAuthRepository
import io.github.dimitrysaf.provenio.core.tracking.trakt.TraktAuthRepository
import io.github.dimitrysaf.provenio.shell.components.SingleChoiceBottomSheet
import io.github.dimitrysaf.provenio.shell.components.SingleChoiceOption
import io.github.dimitrysaf.provenio.shell.components.SmallLoadingSpinner
import io.github.dimitrysaf.provenio.shell.components.StatusModal
import io.github.dimitrysaf.provenio.shell.components.SwatchGrid
import io.github.dimitrysaf.provenio.shell.components.arrowKeysLeaveField
import io.github.dimitrysaf.provenio.shell.components.shapedClickable
import io.github.dimitrysaf.provenio.shell.screens.addons.AddonInstallModalState
import io.github.dimitrysaf.provenio.shell.screens.addons.AddonInstallStatusModal
import io.github.dimitrysaf.provenio.shell.screens.addons.CinemetaManifestUrl
import io.github.dimitrysaf.provenio.shell.screens.addons.CinemetaName
import io.github.dimitrysaf.provenio.shell.screens.addons.OpenSubtitlesManifestUrl
import io.github.dimitrysaf.provenio.shell.screens.addons.OpenSubtitlesName
import io.github.dimitrysaf.provenio.shell.screens.addons.manifestKey
import io.github.dimitrysaf.provenio.shell.screens.profiles.CHOSEN_COLOR_AVATAR_ID
import io.github.dimitrysaf.provenio.shell.screens.profiles.ProfileColorPicker
import io.github.dimitrysaf.provenio.shell.screens.settings.ColorPaletteSwatch
import io.github.dimitrysaf.provenio.shell.screens.settings.LocalSettingsRowIcons
import io.github.dimitrysaf.provenio.shell.screens.settings.LocalSyncCodeDialog
import io.github.dimitrysaf.provenio.shell.screens.settings.LocalSyncFeedbackEffect
import io.github.dimitrysaf.provenio.shell.screens.settings.LocalSyncFirewallDialog
import io.github.dimitrysaf.provenio.shell.screens.settings.LocalSyncPairingSheet
import io.github.dimitrysaf.provenio.shell.screens.settings.LocalSyncProgress
import io.github.dimitrysaf.provenio.shell.screens.settings.SettingsList
import io.github.dimitrysaf.provenio.shell.screens.settings.SettingsListScope
import io.github.dimitrysaf.provenio.shell.screens.settings.SimklTrackerCard
import io.github.dimitrysaf.provenio.shell.screens.settings.StremioImportList
import io.github.dimitrysaf.provenio.shell.screens.settings.TraktTrackerCard
import io.github.dimitrysaf.provenio.shell.screens.settings.effective
import io.github.dimitrysaf.provenio.shell.screens.settings.label
import io.github.dimitrysaf.provenio.shell.screens.settings.rememberLocalSyncScanner
import io.github.dimitrysaf.provenio.shell.theme.isDynamicColorAvailable
import io.github.dimitrysaf.provenio.shell.theme.provenio
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.*

@Composable
internal fun stepTitle(step: OnboardingStep, state: OnboardingState): String = when (step) {
    OnboardingStep.Welcome -> stringResource(Res.string.onboarding_welcome_title)
    OnboardingStep.Appearance -> stringResource(Res.string.onboarding_appearance_title)
    OnboardingStep.Profile -> when {
        state.createdProfileName != null ->
            stringResource(Res.string.onboarding_profile_ready_title, state.createdProfileName.orEmpty())
        state.profilePath == OnboardingProfilePath.New -> stringResource(Res.string.onboarding_profile_new_title)
        state.profilePath == OnboardingProfilePath.Bring -> stringResource(Res.string.onboarding_profile_bring_title)
        else -> stringResource(Res.string.onboarding_profile_title)
    }
    OnboardingStep.Stremio -> stringResource(Res.string.onboarding_stremio_title)
    OnboardingStep.Addons -> stringResource(Res.string.onboarding_addons_title)
    OnboardingStep.Tracking -> stringResource(Res.string.onboarding_tracking_title)
    OnboardingStep.Done -> stringResource(Res.string.onboarding_done_title)
}

@Composable
internal fun onboardingActions(state: OnboardingState, onFinish: () -> Unit): OnboardingActions {
    val scope = rememberCoroutineScope()
    val next = stringResource(Res.string.onboarding_next)
    val addons by AddonRepository.uiState.collectAsStateWithLifecycle()
    val stremio by StremioImport.state.collectAsStateWithLifecycle()
    return when (state.step) {
        OnboardingStep.Welcome -> OnboardingActions(
            primaryLabel = stringResource(Res.string.onboarding_get_started),
            primaryEnabled = true,
            onPrimary = state::next,
            secondaryLabel = if (LocalSyncRepository.isSupported) {
                stringResource(Res.string.onboarding_already_use)
            } else {
                null
            },
            onSecondary = {
                state.profilePath = OnboardingProfilePath.Bring
                state.goTo(OnboardingStep.Profile)
            },
        )
        OnboardingStep.Appearance,
        OnboardingStep.Tracking,
        -> OnboardingActions(primaryLabel = next, primaryEnabled = true, onPrimary = state::next)
        // Coming from Stremio is a question, so until something is brought over it can be skipped.
        OnboardingStep.Stremio -> OnboardingActions(
            primaryLabel = if (stremio is StremioImportState.Done) next else stringResource(Res.string.onboarding_skip),
            primaryEnabled = true,
            onPrimary = state::next,
        )
        // Addons can wait: with none added yet, the step says so by offering to skip it.
        OnboardingStep.Addons -> OnboardingActions(
            primaryLabel = if (addons.addons.isEmpty()) stringResource(Res.string.onboarding_skip) else next,
            primaryEnabled = true,
            onPrimary = state::next,
        )
        OnboardingStep.Profile -> when {
            state.createdProfileName != null ->
                OnboardingActions(primaryLabel = next, primaryEnabled = true, onPrimary = state::next)
            state.profilePath == OnboardingProfilePath.New -> OnboardingActions(
                primaryLabel = stringResource(Res.string.onboarding_profile_create),
                primaryEnabled = state.profileName.isNotBlank() && !state.creatingProfile,
                onPrimary = {
                    val name = state.profileName.trim()
                    if (name.isNotEmpty() && !state.creatingProfile) {
                        if (state.preview) {
                            state.createdProfileName = name
                            state.next()
                        } else {
                            state.creatingProfile = true
                            scope.launch {
                                runCatching {
                                    ProfileRepository.createProfile(
                                        name = name,
                                        avatarColorHex = state.profileColorHex,
                                        avatarId = CHOSEN_COLOR_AVATAR_ID,
                                    )
                                }
                                val created = ProfileRepository.state.value.profiles.lastOrNull { it.name == name }
                                if (created != null) {
                                    // Addons and tracking set up next belong to this profile.
                                    if (created.profileIndex != ProfileRepository.activeProfileId) {
                                        ProfileRepository.selectProfile(created.profileIndex)
                                    }
                                    state.createdProfileName = name
                                    state.next()
                                }
                                state.creatingProfile = false
                            }
                        }
                    }
                },
            )
            else -> OnboardingActions(primaryLabel = null, primaryEnabled = false, onPrimary = {})
        }
        OnboardingStep.Done -> OnboardingActions(
            primaryLabel = stringResource(Res.string.onboarding_start),
            primaryEnabled = true,
            onPrimary = onFinish,
        )
    }
}

/** The rows of a step, as Settings draws them, with their icons. */
@Composable
private fun OnboardingList(content: @Composable SettingsListScope.() -> Unit) {
    CompositionLocalProvider(LocalSettingsRowIcons provides true) {
        SettingsList(content = content)
    }
}

@Composable
internal fun AppearanceStep() {
    remember { ThemeSettingsRepository.ensureLoaded() }
    val language by ThemeSettingsRepository.selectedAppLanguage.collectAsStateWithLifecycle()
    val themeMode by ThemeSettingsRepository.themeMode.collectAsStateWithLifecycle()
    val amoled by ThemeSettingsRepository.amoledEnabled.collectAsStateWithLifecycle()
    val palette by ThemeSettingsRepository.colorPalette.collectAsStateWithLifecycle()
    val dynamicColorAvailable = remember { isDynamicColorAvailable() }
    var showLanguages by rememberSaveable { mutableStateOf(false) }

    val languageTitle = stringResource(Res.string.settings_appearance_app_language)
    val languageValue = stringResource(language.labelRes)
    val amoledTitle = stringResource(Res.string.settings_appearance_amoled_black)
    OnboardingList {
        navigationRow(
            title = languageTitle,
            description = languageValue,
            icon = Icons.Rounded.Language,
            onClick = { showLanguages = true },
        )
    }

    SectionLabel(stringResource(Res.string.settings_appearance_theme))
    ThemeButtonGroup(selected = themeMode, onSelected = ThemeSettingsRepository::setThemeMode)
    OnboardingList {
        switchRow(
            title = amoledTitle,
            checked = { amoled },
            enabled = themeMode != ThemeMode.LIGHT,
            onCheckedChange = ThemeSettingsRepository::setAmoled,
        )
    }

    SectionLabel(stringResource(Res.string.settings_appearance_color_palette))
    val palettes = buildList {
        if (dynamicColorAvailable) add(ColorPalette.Dynamic)
        AppIconOption.entries.forEach { add(ColorPalette.Icon(it)) }
    }
    val selectedPalette = palette.effective(dynamicColorAvailable)
    SwatchGrid(items = palettes, cell = SwatchCell) { option ->
        SelectableSwatch(
            selected = option == selectedPalette,
            label = option.label(),
            onClick = { ThemeSettingsRepository.setColorPalette(option) },
        ) { size -> ColorPaletteSwatch(palette = option, size = size) }
    }

    if (showLanguages) {
        SingleChoiceBottomSheet(
            title = languageTitle,
            options = AppLanguage.entries.map { option ->
                SingleChoiceOption(value = option, label = stringResource(option.labelRes))
            },
            isSelected = { it == language },
            onSelected = ThemeSettingsRepository::setAppLanguage,
            onDismiss = { showLanguages = false },
        )
    }
}

/**
 * The theme as one group of connected buttons, as the player's controls are grouped: the chosen
 * one fills in and rounds out fully, the others keep the group's inner corners.
 */
@Composable
private fun ThemeButtonGroup(
    selected: ThemeMode,
    onSelected: (ThemeMode) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupGap),
    ) {
        val modes = ThemeMode.entries
        modes.forEachIndexed { index, mode ->
            val isSelected = mode == selected
            val outer = 28.dp
            val inner = if (isSelected) outer else 8.dp
            val start by animateDpAsState(if (index == 0) outer else inner)
            val end by animateDpAsState(if (index == modes.lastIndex) outer else inner)
            val shape = RoundedCornerShape(topStart = start, bottomStart = start, topEnd = end, bottomEnd = end)
            val container = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
            val content = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(72.dp)
                    .clip(shape)
                    .background(container)
                    .shapedClickable(shape) { onSelected(mode) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            ) {
                Icon(
                    imageVector = when (mode) {
                        ThemeMode.SYSTEM -> Icons.Rounded.BrightnessMedium
                        ThemeMode.LIGHT -> Icons.Rounded.BrightnessHigh
                        ThemeMode.DARK -> Icons.Rounded.BrightnessLow
                    },
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = stringResource(
                        when (mode) {
                            ThemeMode.SYSTEM -> Res.string.settings_appearance_theme_system
                            ThemeMode.LIGHT -> Res.string.settings_appearance_theme_light
                            ThemeMode.DARK -> Res.string.settings_appearance_theme_dark
                        },
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = content,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
internal fun ProfileStep(state: OnboardingState) {
    if (!LocalSyncRepository.isSupported && state.profilePath == null) {
        LaunchedEffect(Unit) { state.profilePath = OnboardingProfilePath.New }
    }
    when {
        state.createdProfileName != null -> Unit
        state.profilePath == OnboardingProfilePath.New -> NewProfile(state)
        state.profilePath == OnboardingProfilePath.Bring -> BringFromDevice()
        else -> {
            val newTitle = stringResource(Res.string.onboarding_profile_new)
            val bringTitle = stringResource(Res.string.onboarding_profile_bring)
            OnboardingList {
                navigationRow(
                    title = newTitle,
                    icon = Icons.Rounded.PersonAdd,
                    opensPage = true,
                    onClick = { state.choosePath(OnboardingProfilePath.New) },
                )
                navigationRow(
                    title = bringTitle,
                    icon = Icons.Rounded.Devices,
                    opensPage = true,
                    onClick = { state.choosePath(OnboardingProfilePath.Bring) },
                )
            }
        }
    }
}

@Composable
private fun NewProfile(state: OnboardingState) {
    OutlinedTextField(
        value = state.profileName,
        onValueChange = { state.profileName = it.take(ProfileNameMaxLength) },
        label = { Text(stringResource(Res.string.profile_name_placeholder)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        modifier = Modifier
            .fillMaxWidth()
            .arrowKeysLeaveField { state.profileName.isEmpty() },
    )
    SectionLabel(stringResource(Res.string.profile_avatar_source_color))
    ProfileColorPicker(
        selectedHex = state.profileColorHex,
        onSelected = { state.profileColorHex = it },
    )
}

/** Pairing with a device already set up, which brings its profiles, library and settings over. */
@Composable
private fun BringFromDevice() {
    val syncState by LocalSyncRepository.uiState.collectAsStateWithLifecycle()
    var showCodeEntry by rememberSaveable { mutableStateOf(false) }
    val scan = rememberLocalSyncScanner { code -> code?.let(LocalSyncRepository::join) }
    LocalSyncFeedbackEffect(syncState.activity)

    val scanTitle = stringResource(Res.string.local_sync_scan_code)
    val showTitle = stringResource(Res.string.local_sync_show_code)
    val enterTitle = stringResource(Res.string.local_sync_enter_code)
    val joining = syncState.joining
    val spinner: @Composable () -> Unit = { LocalSyncProgress(modifier = Modifier.size(24.dp)) }
    OnboardingList {
        if (scan != null) {
            navigationRow(
                title = scanTitle,
                icon = Icons.Rounded.QrCodeScanner,
                enabled = !joining,
                trailingContent = if (joining) spinner else null,
                onClick = scan,
            )
        }
        navigationRow(
            title = showTitle,
            icon = Icons.Rounded.QrCode,
            onClick = LocalSyncRepository::startPairing,
        )
        navigationRow(
            title = enterTitle,
            icon = Icons.Rounded.Keyboard,
            enabled = !joining,
            trailingContent = if (joining && scan == null) spinner else null,
            onClick = { showCodeEntry = true },
        )
    }

    syncState.pairingCode?.let { code ->
        LocalSyncPairingSheet(code = code, qr = syncState.pairingQr, onDismiss = LocalSyncRepository::stopPairing)
    }
    LocalSyncFirewallDialog(prompt = syncState.firewall)
    if (showCodeEntry) {
        LocalSyncCodeDialog(
            onConnect = { code ->
                showCodeEntry = false
                LocalSyncRepository.join(code)
            },
            onDismiss = { showCodeEntry = false },
        )
    }
}

@Composable
internal fun AddonsStep() {
    LaunchedEffect(Unit) { AddonRepository.initialize() }
    val uiState by AddonRepository.uiState.collectAsStateWithLifecycle()
    val installed = remember(uiState.addons) { uiState.addons.map { it.manifestUrl.manifestKey() }.toSet() }
    var installing by remember { mutableStateOf<String?>(null) }
    var pendingInstall by remember { mutableStateOf<RecommendedAddon?>(null) }
    var installModalState by remember { mutableStateOf<AddonInstallModalState?>(null) }
    val scope = rememberCoroutineScope()

    fun requestInstall(addon: RecommendedAddon) {
        if (installing != null || addon.url.manifestKey() in installed) return
        pendingInstall = addon
    }

    fun install(url: String) {
        if (installing != null || url.manifestKey() in installed) return
        installing = url
        installModalState = AddonInstallModalState.Checking
        scope.launch {
            installModalState = when (val result = AddonRepository.addAddon(url)) {
                is AddAddonResult.Success -> AddonInstallModalState.Success(result.manifest.name)
                is AddAddonResult.Error -> AddonInstallModalState.Error(result.message)
            }
            installing = null
        }
    }

    val recommended = listOf(
        RecommendedAddon(CinemetaName, CinemetaManifestUrl, Icons.Rounded.Movie, stringResource(Res.string.addons_guide_cinemeta_body)),
        RecommendedAddon(OpenSubtitlesName, OpenSubtitlesManifestUrl, Icons.Rounded.Subtitles, stringResource(Res.string.addons_guide_opensubtitles_body)),
    )
    val installLabel = stringResource(Res.string.addons_guide_install)
    val installedLabel = stringResource(Res.string.addons_guide_installed)

    WarningNotice(stringResource(Res.string.addons_guide_torrent_warning).withoutGuideTerms())
    OnboardingList {
        recommended.forEach { addon ->
            val isInstalled = addon.url.manifestKey() in installed
            val isInstalling = installing == addon.url
            navigationRow(
                title = addon.name,
                description = addon.body,
                icon = addon.icon,
                enabled = !isInstalled,
                trailingContent = {
                    FilledTonalIconButton(
                        onClick = { requestInstall(addon) },
                        enabled = !isInstalled && !isInstalling,
                    ) {
                        when {
                            isInstalling -> SmallLoadingSpinner(size = 20.dp)
                            isInstalled -> Icon(Icons.Rounded.Check, contentDescription = installedLabel)
                            else -> Icon(Icons.Rounded.Add, contentDescription = installLabel)
                        }
                    }
                },
                onClick = { requestInstall(addon) },
            )
        }
    }

    pendingInstall?.let { addon ->
        StatusModal(
            title = stringResource(Res.string.addons_install_confirm_title, addon.name),
            message = stringResource(Res.string.addons_install_confirm_message),
            isVisible = true,
            confirmText = stringResource(Res.string.action_yes),
            dismissText = stringResource(Res.string.action_no),
            onConfirm = {
                pendingInstall = null
                install(addon.url)
            },
            onDismiss = { pendingInstall = null },
        )
    }

    installModalState?.let { modalState ->
        AddonInstallStatusModal(
            state = modalState,
            onClose = { installModalState = null },
        )
    }
}

private class RecommendedAddon(
    val name: String,
    val url: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val body: String,
)

@Composable
internal fun StremioStep() {
    CompositionLocalProvider(LocalSettingsRowIcons provides true) {
        StremioImportList()
    }
}

@Composable
internal fun TrackingStep() {
    val traktState by TraktAuthRepository.uiState.collectAsStateWithLifecycle()
    val simklState by SimklAuthRepository.uiState.collectAsStateWithLifecycle()
    TraktTrackerCard(uiState = traktState)
    SimklTrackerCard(uiState = simklState)
}

@Composable
internal fun DoneStep(state: OnboardingState) {
    val addons by AddonRepository.uiState.collectAsStateWithLifecycle()
    val connected by TrackingProviderRegistry.connectedProviderIds.collectAsStateWithLifecycle()
    val trackers = connected
        .sortedBy { it.ordinal }
        .mapNotNull { id -> TrackingProviderRegistry.authProvider(id)?.descriptor?.displayName }
    val profileTitle = stringResource(Res.string.onboarding_done_profile)
    val addonsTitle = stringResource(Res.string.onboarding_done_addons)
    val addonsValue = if (addons.addons.isEmpty()) {
        stringResource(Res.string.onboarding_done_addons_none)
    } else {
        addons.addons.mapNotNull { it.manifest?.name }.joinToString(", ").ifBlank { addons.addons.size.toString() }
    }
    val trackingTitle = stringResource(Res.string.onboarding_done_tracking)
    val trackingValue = trackers.joinToString(", ").ifBlank { stringResource(Res.string.onboarding_done_tracking_none) }
    OnboardingList {
        state.createdProfileName?.let { name -> infoRow(title = profileTitle, description = name) }
        infoRow(title = addonsTitle, description = addonsValue)
        infoRow(title = trackingTitle, description = trackingValue)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, start = 4.dp),
    )
}

/** A caution about everything under it, in the theme's warning colour, as Settings shows one. */
@Composable
private fun WarningNotice(text: String) {
    val warning = MaterialTheme.provenio.colors.warning
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(warning.copy(alpha = 0.12f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = Icons.Rounded.Warning, contentDescription = null, tint = warning)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** A round choice, ringed when chosen, with a tick over it. */
@Composable
private fun SelectableSwatch(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    content: @Composable (Dp) -> Unit,
) {
    Box(
        modifier = Modifier
            .size(SwatchCell)
            .aspectRatio(1f)
            .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
            .padding(6.dp)
            .shapedClickable(CircleShape, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content(SwatchCell - 12.dp)
        if (selected) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/** The addons guide marks terms it explains as [[key|text]]; here they read as plain text. */
private fun String.withoutGuideTerms(): String = replace(GuideTermPattern, "$2")

private val GuideTermPattern = Regex("""\[\[([a-z]+)\|([^\]]+)]]""")
private val SwatchCell = 56.dp
private val ButtonGroupGap = 2.dp
private const val ProfileNameMaxLength = 30
