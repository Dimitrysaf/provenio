package io.github.dimitrysaf.provenio

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.uikit.OnFocusBehavior
import androidx.compose.ui.window.ComposeUIViewController
import io.github.dimitrysaf.provenio.shell.components.NativeProfileSwitcherController
import io.github.dimitrysaf.provenio.shell.nav.AppRoute
import platform.UIKit.UIColor
import platform.UIKit.UIViewController
import io.github.dimitrysaf.provenio.shell.App
import io.github.dimitrysaf.provenio.shell.AppGateController
import io.github.dimitrysaf.provenio.shell.AppGateOverlay
import io.github.dimitrysaf.provenio.shell.AppScreenTab

private val appBackgroundColor = UIColor(red = 0.051, green = 0.051, blue = 0.051, alpha = 1.0)

@Suppress("unused")
fun MainViewController(): UIViewController = composeViewController {
    App()
}

@Suppress("unused")
fun MainViewController(
    initialTabName: String,
    useNativeTabBar: Boolean,
    useTabletFloatingTabBar: Boolean,
    onNavigate: (AppRoute, Boolean) -> Unit,
    onGoBack: () -> Unit,
    onReplace: (AppRoute) -> Unit,
    onActivate: (String) -> Unit,
    onTabTitles: (String, String, String, String, String, String) -> Unit,
    appGateController: AppGateController,
): UIViewController {
    val initialTab = AppScreenTab.fromName(initialTabName)
    return composeViewController {
        App(
            initialTab = initialTab,
            useNativeNavigation = true,
            useNativeTabBar = useNativeTabBar,
            useTabletFloatingTabBar = useTabletFloatingTabBar,
            ownsAppRuntime = initialTab == AppScreenTab.Home,
            bypassAppGate = true,
            onNavigate = onNavigate,
            onGoBack = onGoBack,
            onReplace = onReplace,
            onActivate = { tab -> onActivate(tab.name) },
            onTabTitles = onTabTitles,
            appGateController = appGateController,
        )
    }
}

@Suppress("unused")
fun ScreenViewController(
    route: AppRoute,
    onNavigate: (AppRoute, Boolean) -> Unit,
    onGoBack: () -> Unit,
    onReplace: (AppRoute) -> Unit,
    onActivate: (String) -> Unit,
    appGateController: AppGateController,
): UIViewController = composeViewController {
    App(
        initialRoute = route,
        useNativeNavigation = true,
        ownsAppRuntime = false,
        bypassAppGate = true,
        onNavigate = onNavigate,
        onGoBack = onGoBack,
        onReplace = onReplace,
        onActivate = { tab -> onActivate(tab.name) },
        appGateController = appGateController,
    )
}

@Suppress("unused")
@OptIn(ExperimentalComposeUiApi::class)
fun AppGateViewController(
    appGateController: AppGateController,
    nativeProfileSwitcherController: NativeProfileSwitcherController,
    onActivate: (String) -> Unit,
    onAppReady: (Boolean) -> Unit,
    onMainContentMountChanged: (Boolean) -> Unit,
    onMainContentVisibleChanged: (Boolean) -> Unit,
): UIViewController = ComposeUIViewController(
    configure = {
        onFocusBehavior = OnFocusBehavior.DoNothing
        opaque = false
    },
    content = {
        AppGateOverlay(
            onActivate = { tab -> onActivate(tab.name) },
            onAppReady = onAppReady,
            onMainContentMountChanged = onMainContentMountChanged,
            onMainContentVisibleChanged = onMainContentVisibleChanged,
            nativeProfileSwitcherController = nativeProfileSwitcherController,
            appGateController = appGateController,
        )
    },
).apply {
    view.backgroundColor = UIColor.clearColor
}

private fun composeViewController(
    content: @androidx.compose.runtime.Composable () -> Unit,
): UIViewController = ComposeUIViewController(
    configure = { onFocusBehavior = OnFocusBehavior.DoNothing },
    content = content,
).apply {
    view.backgroundColor = appBackgroundColor
}
