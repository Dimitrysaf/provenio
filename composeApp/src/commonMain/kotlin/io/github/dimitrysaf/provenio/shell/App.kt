package io.github.dimitrysaf.provenio.shell

import io.github.dimitrysaf.provenio.core.settings.ThemeMode
import io.github.dimitrysaf.provenio.shell.theme.systemPrefersDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.dimitrysaf.provenio.shell.components.AdaptiveWindowRoot
import io.github.dimitrysaf.provenio.shell.components.AppSnackbarHost
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.CachePolicy
import coil3.request.crossfade
import coil3.svg.SvgDecoder
import io.github.dimitrysaf.provenio.shell.components.NativeProfileSwitcherController
import io.github.dimitrysaf.provenio.shell.theme.Theme
import io.github.dimitrysaf.provenio.shell.components.configurePlatformImageLoader
import io.github.dimitrysaf.provenio.shell.theme.isDynamicColorAvailable
import io.github.dimitrysaf.provenio.core.settings.ThemeSettingsRepository
import io.github.dimitrysaf.provenio.shell.nav.AppRoute
import io.github.dimitrysaf.provenio.shell.nav.TabsRoute
import androidx.compose.runtime.LaunchedEffect
import io.github.dimitrysaf.provenio.shell.components.NativeTabBridge
import io.github.dimitrysaf.provenio.shell.components.PointerCursorHost
import io.github.dimitrysaf.provenio.shell.theme.themePalette
import io.github.dimitrysaf.provenio.core.settings.AppIconOption
import io.github.dimitrysaf.provenio.core.settings.ColorPalette
import androidx.compose.material3.MaterialTheme

fun disposeRoute(route: AppRoute) {
    disposeRouteResources(route)
}

@OptIn(ExperimentalCoilApi::class)
@Composable
@Preview
fun App(
    initialTab: AppScreenTab = AppScreenTab.Home,
    initialRoute: AppRoute = TabsRoute,
    useNativeNavigation: Boolean = false,
    useNativeTabBar: Boolean = false,
    useTabletFloatingTabBar: Boolean = false,
    ownsAppRuntime: Boolean = true,
    bypassAppGate: Boolean = false,
    onNavigate: ((AppRoute, launchSingleTop: Boolean) -> Unit)? = null,
    onGoBack: (() -> Unit)? = null,
    onReplace: ((AppRoute) -> Unit)? = null,
    onActivate: ((AppScreenTab) -> Unit)? = null,
    onAppReady: ((Boolean) -> Unit)? = null,
    onTabTitles: ((home: String, search: String, library: String, profile: String, switchProfile: String, addProfile: String) -> Unit)? = null,
    nativeProfileSwitcherController: NativeProfileSwitcherController? = null,
    appGateController: AppGateController? = null,
) {
    AppEnvironment {
        PointerCursorHost(modifier = Modifier.fillMaxSize()) {
            AppGate(
                initialTab = initialTab,
                initialRoute = initialRoute,
                useNativeNavigation = useNativeNavigation,
                useNativeTabBar = useNativeTabBar,
                useTabletFloatingTabBar = useTabletFloatingTabBar,
                ownsAppRuntime = ownsAppRuntime,
                bypassAppGate = bypassAppGate,
                renderMainContent = true,
                onNavigate = onNavigate,
                onGoBack = onGoBack,
                onReplace = onReplace,
                onActivate = onActivate,
                onAppReady = onAppReady,
                onMainContentMountChanged = null,
                onMainContentVisibleChanged = null,
                onTabTitles = onTabTitles,
                nativeProfileSwitcherController = nativeProfileSwitcherController,
                appGateController = appGateController,
            )
            AppSnackbarHost(modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
internal fun AppEnvironment(content: @Composable () -> Unit) {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .crossfade(true)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .components {
                add(SvgDecoder.Factory())
                add(
                    coil3.network.ktor3.KtorNetworkFetcherFactory(
                        cacheStrategy = { coil3.network.cachecontrol.CacheControlCacheStrategy() },
                    ),
                )
            }
            .configurePlatformImageLoader()
            .build()
    }
    AppThemeEnvironment(content = content)
}

@Composable
internal fun AppThemeEnvironment(content: @Composable () -> Unit) {
    val amoledEnabled by remember {
        ThemeSettingsRepository.ensureLoaded()
        ThemeSettingsRepository.amoledEnabled
    }.collectAsStateWithLifecycle()

    val themeMode by remember {
        ThemeSettingsRepository.ensureLoaded()
        ThemeSettingsRepository.themeMode
    }.collectAsStateWithLifecycle()
    val colorPalette by remember {
        ThemeSettingsRepository.ensureLoaded()
        ThemeSettingsRepository.colorPalette
    }.collectAsStateWithLifecycle()
    val systemDarkTheme = systemPrefersDarkTheme()
    val dynamicColorAvailable = remember { isDynamicColorAvailable() }
    val palette = colorPalette
    val useDynamicColor = dynamicColorAvailable && palette == ColorPalette.Dynamic

    Theme(
        darkTheme = when (themeMode) {
            ThemeMode.SYSTEM -> systemDarkTheme
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        },
        amoled = amoledEnabled,
        useDynamicColor = useDynamicColor,
        paletteIcon = (palette as? ColorPalette.Icon)?.icon ?: AppIconOption.DEFAULT,
    ) {
        val accentHex = MaterialTheme.themePalette.nativeAccentHex
        LaunchedEffect(accentHex) {
            NativeTabBridge.publishAccentColor(accentHex)
        }
        AdaptiveWindowRoot(content = content)
    }
}
