@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)

package io.github.dimitrysaf.provenio.shell.screens.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.BrightnessLow
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
import io.github.dimitrysaf.provenio.core.tracking.TrackingProviderRegistry
import io.github.dimitrysaf.provenio.core.tracking.simkl.SimklAuthRepository
import io.github.dimitrysaf.provenio.core.tracking.trakt.TraktAuthRepository
import io.github.dimitrysaf.provenio.shell.components.SingleChoiceBottomSheet
import io.github.dimitrysaf.provenio.shell.components.SingleChoiceOption
import io.github.dimitrysaf.provenio.shell.components.SmallLoadingSpinner
import io.github.dimitrysaf.provenio.shell.components.ToastController
import io.github.dimitrysaf.provenio.shell.components.shapedClickable
import io.github.dimitrysaf.provenio.shell.screens.addons.CinemetaManifestUrl
import io.github.dimitrysaf.provenio.shell.screens.addons.CinemetaName
import io.github.dimitrysaf.provenio.shell.screens.addons.OpenSubtitlesManifestUrl
import io.github.dimitrysaf.provenio.shell.screens.addons.OpenSubtitlesName
import io.github.dimitrysaf.provenio.shell.screens.addons.TorrentioManifestUrl
import io.github.dimitrysaf.provenio.shell.screens.addons.TorrentioName
import io.github.dimitrysaf.provenio.shell.screens.addons.manifestKey
import io.github.dimitrysaf.provenio.shell.screens.profiles.PROFILE_COLORS
import io.github.dimitrysaf.provenio.shell.screens.profiles.ProfileAvatar
import io.github.dimitrysaf.provenio.shell.screens.profiles.parseHexColor
import io.github.dimitrysaf.provenio.shell.screens.settings.ColorPaletteSwatch
import io.github.dimitrysaf.provenio.shell.screens.settings.LocalSyncCodeDialog
import io.github.dimitrysaf.provenio.shell.screens.settings.LocalSyncFeedbackEffect
import io.github.dimitrysaf.provenio.shell.screens.settings.LocalSyncFirewallDialog
import io.github.dimitrysaf.provenio.shell.screens.settings.LocalSyncPairingDialog
import io.github.dimitrysaf.provenio.shell.screens.settings.LocalSyncProgress
import io.github.dimitrysaf.provenio.shell.screens.settings.SimklTrackerCard
import io.github.dimitrysaf.provenio.shell.screens.settings.TraktTrackerCard
import io.github.dimitrysaf.provenio.shell.screens.settings.effective
import io.github.dimitrysaf.provenio.shell.screens.settings.label
import io.github.dimitrysaf.provenio.shell.screens.settings.rememberLocalSyncScanner
import io.github.dimitrysaf.provenio.shell.theme.isDynamicColorAvailable
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
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
    OnboardingStep.Addons -> stringResource(Res.string.onboarding_addons_title)
    OnboardingStep.Tracking -> stringResource(Res.string.onboarding_tracking_title)
    OnboardingStep.Done -> stringResource(Res.string.onboarding_done_title)
}

@Composable
internal fun stepBody(step: OnboardingStep, state: OnboardingState): String = when (step) {
    OnboardingStep.Welcome -> stringResource(Res.string.onboarding_welcome_body)
    OnboardingStep.Appearance -> stringResource(Res.string.onboarding_appearance_body)
    OnboardingStep.Profile -> when {
        state.createdProfileName != null -> stringResource(Res.string.onboarding_profile_ready_body)
        state.profilePath == OnboardingProfilePath.New -> stringResource(Res.string.onboarding_profile_new_body)
        state.profilePath == OnboardingProfilePath.Bring -> stringResource(Res.string.onboarding_profile_bring_body)
        else -> stringResource(Res.string.onboarding_profile_body)
    }
    OnboardingStep.Addons -> stringResource(Res.string.onboarding_addons_body)
    OnboardingStep.Tracking -> stringResource(Res.string.onboarding_tracking_body)
    OnboardingStep.Done -> stringResource(Res.string.onboarding_done_body)
}

@Composable
internal fun onboardingActions(state: OnboardingState, onFinish: () -> Unit): OnboardingActions {
    val scope = rememberCoroutineScope()
    val next = stringResource(Res.string.onboarding_next)
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
        OnboardingStep.Addons,
        OnboardingStep.Tracking,
        -> OnboardingActions(primaryLabel = next, primaryEnabled = true, onPrimary = state::next)
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
                                    ProfileRepository.createProfile(name = name, avatarColorHex = state.profileColorHex)
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

@Composable
internal fun WelcomeStep() {
    OnboardingGroup {
        FeatureRow(
            icon = Icons.Rounded.Extension,
            title = stringResource(Res.string.onboarding_feature_addons_title),
            body = stringResource(Res.string.onboarding_feature_addons_body),
        )
        FeatureRow(
            icon = Icons.Rounded.Devices,
            title = stringResource(Res.string.onboarding_feature_devices_title),
            body = stringResource(Res.string.onboarding_feature_devices_body),
        )
        FeatureRow(
            icon = Icons.Rounded.Sync,
            title = stringResource(Res.string.onboarding_feature_tracking_title),
            body = stringResource(Res.string.onboarding_feature_tracking_body),
        )
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

    SectionLabel(stringResource(Res.string.onboarding_language))
    OnboardingGroup {
        ChoiceRow(
            icon = Icons.Rounded.Language,
            title = stringResource(language.labelRes),
            onClick = { showLanguages = true },
        )
    }

    SectionLabel(stringResource(Res.string.onboarding_theme))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        ThemeMode.entries.forEach { mode ->
            ThemeChoice(
                mode = mode,
                selected = mode == themeMode,
                onClick = { ThemeSettingsRepository.setThemeMode(mode) },
                modifier = Modifier.weight(1f),
            )
        }
    }
    OnboardingGroup {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.settings_appearance_amoled_black),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(Res.string.settings_appearance_amoled_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Switch(
                checked = amoled,
                enabled = themeMode != ThemeMode.LIGHT,
                onCheckedChange = ThemeSettingsRepository::setAmoled,
            )
        }
    }

    SectionLabel(stringResource(Res.string.onboarding_colors))
    val palettes = buildList {
        if (dynamicColorAvailable) add(ColorPalette.Dynamic)
        AppIconOption.entries.forEach { add(ColorPalette.Icon(it)) }
    }
    val selectedPalette = palette.effective(dynamicColorAvailable)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        palettes.forEach { option ->
            val label = option.label()
            SelectableCircle(
                selected = option == selectedPalette,
                label = label,
                onClick = { ThemeSettingsRepository.setColorPalette(option) },
            ) {
                ColorPaletteSwatch(palette = option, size = SwatchSize)
            }
        }
    }

    if (showLanguages) {
        SingleChoiceBottomSheet(
            title = stringResource(Res.string.settings_appearance_app_language_sheet_title),
            options = AppLanguage.entries.map { option ->
                SingleChoiceOption(value = option, label = stringResource(option.labelRes))
            },
            isSelected = { it == language },
            onSelected = ThemeSettingsRepository::setAppLanguage,
            onDismiss = { showLanguages = false },
        )
    }
}

@Composable
private fun ThemeChoice(
    mode: ThemeMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The chosen card rounds out, as expressive selection does, rather than only changing colour.
    val corner by animateDpAsState(if (selected) 28.dp else 12.dp)
    val shape = RoundedCornerShape(corner)
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        shape = shape,
        modifier = modifier
            .heightIn(min = 96.dp)
            .shapedClickable(shape, onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {
            Icon(
                imageVector = when (mode) {
                    ThemeMode.SYSTEM -> Icons.Rounded.BrightnessMedium
                    ThemeMode.LIGHT -> Icons.Rounded.BrightnessHigh
                    ThemeMode.DARK -> Icons.Rounded.BrightnessLow
                },
                contentDescription = null,
                modifier = Modifier.size(28.dp),
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
            )
        }
    }
}

@Composable
internal fun ProfileStep(state: OnboardingState) {
    if (!LocalSyncRepository.isSupported && state.profilePath == null) {
        LaunchedEffect(Unit) { state.profilePath = OnboardingProfilePath.New }
    }
    when {
        state.createdProfileName != null -> ProfileReady(state)
        state.profilePath == OnboardingProfilePath.New -> NewProfile(state)
        state.profilePath == OnboardingProfilePath.Bring -> BringFromDevice()
        else -> {
            BigChoice(
                icon = Icons.Rounded.PersonAdd,
                title = stringResource(Res.string.onboarding_profile_new),
                body = stringResource(Res.string.onboarding_profile_new_choice_body),
                onClick = { state.profilePath = OnboardingProfilePath.New },
            )
            BigChoice(
                icon = Icons.Rounded.Devices,
                title = stringResource(Res.string.onboarding_profile_bring),
                body = stringResource(Res.string.onboarding_profile_bring_choice_body),
                onClick = { state.profilePath = OnboardingProfilePath.Bring },
            )
        }
    }
}

@Composable
private fun NewProfile(state: OnboardingState) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        ProfileAvatar(
            size = 112.dp,
            imageUrl = null,
            monogram = state.profileName.ifBlank { null },
            identifier = state.profileName,
            contentDescription = null,
            containerColor = parseHexColor(state.profileColorHex),
        )
    }
    OutlinedTextField(
        value = state.profileName,
        onValueChange = { state.profileName = it.take(ProfileNameMaxLength) },
        label = { Text(stringResource(Res.string.onboarding_profile_name)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth(),
    )
    SectionLabel(stringResource(Res.string.onboarding_profile_color))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PROFILE_COLORS.forEach { hex ->
            SelectableCircle(
                selected = hex == state.profileColorHex,
                label = hex,
                onClick = { state.profileColorHex = hex },
            ) {
                Surface(
                    color = parseHexColor(hex),
                    shape = CircleShape,
                    modifier = Modifier.size(SwatchSize),
                ) {}
            }
        }
    }
    if (state.preview) {
        Text(
            text = stringResource(Res.string.onboarding_preview_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProfileReady(state: OnboardingState) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        ProfileAvatar(
            size = 112.dp,
            imageUrl = null,
            monogram = state.createdProfileName,
            identifier = state.createdProfileName.orEmpty(),
            contentDescription = null,
            containerColor = parseHexColor(state.profileColorHex),
        )
    }
}

/** Pairing with a device already set up, which brings its profiles, library and settings over. */
@Composable
private fun BringFromDevice() {
    val syncState by LocalSyncRepository.uiState.collectAsStateWithLifecycle()
    var showCodeEntry by rememberSaveable { mutableStateOf(false) }
    val scan = rememberLocalSyncScanner { code -> code?.let(LocalSyncRepository::join) }
    LocalSyncFeedbackEffect(syncState.activity)

    Text(
        text = stringResource(Res.string.welcome_pairing_hint),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OnboardingGroup {
        if (scan != null) {
            ChoiceRow(
                icon = Icons.Rounded.QrCodeScanner,
                title = stringResource(Res.string.welcome_scan_code),
                enabled = !syncState.joining,
                loading = syncState.joining,
                onClick = scan,
            )
        }
        ChoiceRow(
            icon = Icons.Rounded.QrCode,
            title = stringResource(Res.string.welcome_show_code),
            onClick = LocalSyncRepository::startPairing,
        )
        ChoiceRow(
            icon = Icons.Rounded.Keyboard,
            title = stringResource(Res.string.welcome_enter_code),
            enabled = !syncState.joining,
            loading = syncState.joining && scan == null,
            onClick = { showCodeEntry = true },
        )
    }

    syncState.pairingCode?.let { code ->
        LocalSyncPairingDialog(code = code, qr = syncState.pairingQr, onDismiss = LocalSyncRepository::stopPairing)
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
    var link by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun install(url: String) {
        if (installing != null || url.isBlank() || url.manifestKey() in installed) return
        installing = url
        scope.launch {
            when (val result = AddonRepository.addAddon(url)) {
                is AddAddonResult.Success -> {
                    ToastController.show(getString(Res.string.addons_modal_success_message, result.manifest.name))
                    if (url == link) link = ""
                }
                is AddAddonResult.Error -> ToastController.show(result.message)
            }
            installing = null
        }
    }

    OnboardingGroup {
        RecommendedAddon(
            icon = Icons.Rounded.Movie,
            name = CinemetaName,
            body = stringResource(Res.string.addons_guide_cinemeta_body),
            installed = CinemetaManifestUrl.manifestKey() in installed,
            installing = installing == CinemetaManifestUrl,
            onInstall = { install(CinemetaManifestUrl) },
        )
        RecommendedAddon(
            icon = Icons.Rounded.PlayCircle,
            name = TorrentioName,
            body = stringResource(Res.string.addons_guide_torrentio_body).withoutGuideTerms(),
            installed = TorrentioManifestUrl.manifestKey() in installed,
            installing = installing == TorrentioManifestUrl,
            onInstall = { install(TorrentioManifestUrl) },
        )
        RecommendedAddon(
            icon = Icons.Rounded.Subtitles,
            name = OpenSubtitlesName,
            body = stringResource(Res.string.addons_guide_opensubtitles_body),
            installed = OpenSubtitlesManifestUrl.manifestKey() in installed,
            installing = installing == OpenSubtitlesManifestUrl,
            onInstall = { install(OpenSubtitlesManifestUrl) },
        )
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Rounded.WarningAmber,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = stringResource(Res.string.addons_guide_torrent_warning).withoutGuideTerms(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    SectionLabel(stringResource(Res.string.onboarding_addons_link_title))
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = link,
            onValueChange = { link = it.trim() },
            label = { Text(stringResource(Res.string.onboarding_addons_link)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { install(link) }),
            modifier = Modifier.weight(1f),
        )
        FilledTonalButton(
            onClick = { install(link) },
            enabled = link.isNotBlank() && installing == null,
            modifier = Modifier.heightIn(min = 56.dp),
        ) {
            if (installing == link && link.isNotBlank()) {
                SmallLoadingSpinner(size = 20.dp)
            } else {
                Text(stringResource(Res.string.onboarding_addons_add))
            }
        }
    }
}

@Composable
private fun RecommendedAddon(
    icon: ImageVector,
    name: String,
    body: String,
    installed: Boolean,
    installing: Boolean,
    onInstall: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon)
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FilledTonalButton(
            onClick = onInstall,
            enabled = !installed && !installing,
        ) {
            when {
                installing -> SmallLoadingSpinner(size = 18.dp)
                installed -> {
                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(Res.string.addons_guide_installed))
                }
                else -> Text(stringResource(Res.string.addons_guide_install))
            }
        }
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
    OnboardingGroup {
        state.createdProfileName?.let { name ->
            FeatureRow(
                icon = Icons.Rounded.People,
                title = stringResource(Res.string.onboarding_done_profile),
                body = name,
            )
        }
        FeatureRow(
            icon = Icons.Rounded.Extension,
            title = stringResource(Res.string.onboarding_done_addons),
            body = if (addons.addons.isEmpty()) {
                stringResource(Res.string.onboarding_done_addons_none)
            } else {
                addons.addons.mapNotNull { it.manifest?.name }.joinToString(", ")
                    .ifBlank { addons.addons.size.toString() }
            },
        )
        FeatureRow(
            icon = Icons.Rounded.Sync,
            title = stringResource(Res.string.onboarding_done_tracking),
            body = trackers.joinToString(", ").ifBlank { stringResource(Res.string.onboarding_done_tracking_none) },
        )
    }
}

/** Rows that belong together, on one rounded surface. */
@Composable
private fun OnboardingGroup(content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) { content() }
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

@Composable
private fun FeatureRow(icon: ImageVector, title: String, body: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ChoiceRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .shapedClickable(shape, enabled = enabled, onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                LocalSyncProgress(modifier = Modifier.size(24.dp))
            }
        } else {
            IconBadge(icon)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
    }
}

/** A choice that leads somewhere, as a tile of its own: large to tap, and large on a TV. */
@Composable
private fun BigChoice(
    icon: ImageVector,
    title: String,
    body: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(28.dp)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = shape,
        modifier = Modifier
            .fillMaxWidth()
            .shapedClickable(shape, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(icon, size = 56.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun IconBadge(icon: ImageVector, size: androidx.compose.ui.unit.Dp = 40.dp) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = CircleShape,
        modifier = Modifier.size(size),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(size * 0.55f))
        }
    }
}

/** A round choice, ringed when chosen, with a tick over it. */
@Composable
private fun SelectableCircle(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(SwatchSize + 12.dp)
            .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
            .padding(6.dp)
            .shapedClickable(CircleShape, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
        if (selected) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.size(22.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

/** The addons guide marks terms it explains as [[key|text]]; here they read as plain text. */
private fun String.withoutGuideTerms(): String = replace(GuideTermPattern, "$2")

private val GuideTermPattern = Regex("""\[\[([a-z]+)\|([^\]]+)]]""")
private val SwatchSize = 44.dp
private const val ProfileNameMaxLength = 30
