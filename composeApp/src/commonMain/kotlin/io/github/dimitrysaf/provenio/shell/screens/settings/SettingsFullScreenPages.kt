package io.github.dimitrysaf.provenio.shell.screens.settings

import io.github.dimitrysaf.provenio.core.build.AppFeaturePolicy
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.provenio.shell.components.ScreenScaffold
import io.github.dimitrysaf.provenio.core.addons.AddonRepository
import io.github.dimitrysaf.provenio.core.addons.enabledAddons
import io.github.dimitrysaf.provenio.core.addons.firstEnabledManifestError
import io.github.dimitrysaf.provenio.core.addons.hasPendingEnabledManifests
import io.github.dimitrysaf.provenio.core.addons.isWaitingForFirstEnabledManifest
import io.github.dimitrysaf.provenio.core.collection.CollectionRepository
import io.github.dimitrysaf.provenio.core.metadata.MetaScreenSettingsRepository
import io.github.dimitrysaf.provenio.core.plugins.PluginRepository
import io.github.dimitrysaf.provenio.core.home.HomeCatalogSettingsRepository
import io.github.dimitrysaf.provenio.core.home.buildAddonCatalogRefreshSignature
import io.github.dimitrysaf.provenio.core.watch.progress.ContinueWatchingPreferencesRepository
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.compose_settings_page_account
import provenio.composeapp.generated.resources.compose_settings_page_addons
import provenio.composeapp.generated.resources.compose_settings_page_continue_watching
import provenio.composeapp.generated.resources.compose_settings_page_homescreen
import provenio.composeapp.generated.resources.compose_settings_page_meta_screen
import provenio.composeapp.generated.resources.compose_settings_page_plugins
import org.jetbrains.compose.resources.stringResource

@Composable
fun HomescreenSettingsScreen(
    onBack: () -> Unit,
) {
    val addonsUiState by AddonRepository.uiState.collectAsStateWithLifecycle()
    val homescreenCatalogRefreshKey = remember(addonsUiState.addons) {
        buildAddonCatalogRefreshSignature(addonsUiState.addons)
    }
    val addonManifestsLoading = addonsUiState.addons.hasPendingEnabledManifests()
    val addonManifestErrorMessage = addonsUiState.addons.firstEnabledManifestError()
    val homescreenSettingsUiState by remember {
        HomeCatalogSettingsRepository.snapshot()
        HomeCatalogSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val collections by CollectionRepository.collections.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        AddonRepository.initialize()
        CollectionRepository.initialize()
    }

    LaunchedEffect(homescreenCatalogRefreshKey) {
        val enabledAddons = addonsUiState.addons.enabledAddons()
        if (!enabledAddons.isWaitingForFirstEnabledManifest()) {
            HomeCatalogSettingsRepository.syncCatalogs(enabledAddons)
        }
    }

    LaunchedEffect(collections) {
        HomeCatalogSettingsRepository.syncCollections(collections)
    }

    ScreenScaffold(
        title = stringResource(Res.string.compose_settings_page_homescreen),
        modifier = Modifier.fillMaxSize(),
        onBack = onBack,
    ) {
        homescreenSettingsContent(
            isTablet = false,
            heroEnabled = homescreenSettingsUiState.heroEnabled,
            showCatalogType = homescreenSettingsUiState.showCatalogType,
            hideUnreleasedContent = homescreenSettingsUiState.hideUnreleasedContent,
            items = homescreenSettingsUiState.items,
            isCatalogLoading = addonManifestsLoading,
            catalogErrorMessage = addonManifestErrorMessage,
        )
    }
}

@Composable
fun MetaScreenSettingsScreen(
    onBack: () -> Unit,
) {
    val metaScreenSettingsUiState by remember {
        MetaScreenSettingsRepository.ensureLoaded()
        MetaScreenSettingsRepository.uiState
    }.collectAsStateWithLifecycle()

    ScreenScaffold(
        title = stringResource(Res.string.compose_settings_page_meta_screen),
        modifier = Modifier.fillMaxSize(),
        onBack = onBack,
    ) {
        metaScreenSettingsContent(
            isTablet = false,
            uiState = metaScreenSettingsUiState,
        )
    }
}

@Composable
fun ContinueWatchingSettingsScreen(
    onBack: () -> Unit,
) {
    val continueWatchingPreferencesUiState by remember {
        ContinueWatchingPreferencesRepository.ensureLoaded()
        ContinueWatchingPreferencesRepository.uiState
    }.collectAsStateWithLifecycle()

    ScreenScaffold(
        title = stringResource(Res.string.compose_settings_page_continue_watching),
        modifier = Modifier.fillMaxSize(),
        onBack = onBack,
    ) {
        continueWatchingSettingsContent(
            isTablet = false,
            isVisible = continueWatchingPreferencesUiState.isVisible,
            style = continueWatchingPreferencesUiState.style,
            upNextFromFurthestEpisode = continueWatchingPreferencesUiState.upNextFromFurthestEpisode,
            useEpisodeThumbnails = continueWatchingPreferencesUiState.useEpisodeThumbnails,
            showUnairedNextUp = continueWatchingPreferencesUiState.showUnairedNextUp,
            blurNextUp = continueWatchingPreferencesUiState.blurNextUp,
            sortMode = continueWatchingPreferencesUiState.sortMode,
        )
    }
}

@Composable
fun AddonsSettingsScreen(
    onBack: () -> Unit,
) {
    LaunchedEffect(Unit) {
        AddonRepository.initialize()
    }

    ScreenScaffold(
        title = stringResource(Res.string.compose_settings_page_addons),
        modifier = Modifier.fillMaxSize(),
        onBack = onBack,
    ) {
        addonsSettingsContent()
    }
}

@Composable
fun PluginsSettingsScreen(
    onBack: () -> Unit,
) {
    if (!AppFeaturePolicy.pluginsEnabled) {
        AddonsSettingsScreen(onBack = onBack)
        return
    }

    LaunchedEffect(Unit) {
        PluginRepository.initialize()
    }

    ScreenScaffold(
        title = stringResource(Res.string.compose_settings_page_plugins),
        modifier = Modifier.fillMaxSize(),
        onBack = onBack,
    ) {
        pluginsSettingsContent()
    }
}


