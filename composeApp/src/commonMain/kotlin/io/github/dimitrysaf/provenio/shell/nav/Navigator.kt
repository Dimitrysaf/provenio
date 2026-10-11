package io.github.dimitrysaf.provenio.shell.nav

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlin.reflect.KClass

internal class Navigator(
    private val backStack: NavBackStack<NavKey>,
    private val onExternalNavigate: ((AppRoute, launchSingleTop: Boolean) -> Unit)? = null,
    private val onExternalBack: (() -> Unit)? = null,
    private val onExternalReplace: ((AppRoute) -> Unit)? = null,
    private val onLocalNavigate: ((AppRoute?, AppRoute) -> Unit)? = null,
    private val onLocalPop: (() -> Unit)? = null,
) {
    val currentRoute: AppRoute?
        get() = backStack.lastOrNull() as? AppRoute

    val routes: List<AppRoute>
        get() = backStack.filterIsInstance<AppRoute>()

    // Pages left by going back, newest last, so forward can return to them like a browser does.
    private val forwardRoutes = ArrayDeque<AppRoute>()

    val canGoForward: Boolean
        get() = forwardRoutes.isNotEmpty()

    /** Returns to the page last left by going back, if nothing new has been opened since. */
    fun navigateForward(): Boolean {
        val route = forwardRoutes.removeLastOrNull() ?: return false
        openRoute(route)
        return true
    }

    fun navigate(route: AppRoute, options: NavigateOptions.() -> Unit = {}) {
        val resolvedOptions = NavigateOptions().apply(options)

        if (resolvedOptions.launchSingleTop && currentRoute == route) return

        val popUpToRoute = resolvedOptions.popUpToRoute
        if (popUpToRoute != null) {
            val targetIndex = backStack.indexOfLast { popUpToRoute.isInstance(it) }
            if (targetIndex >= 0) {
                if (
                    onExternalReplace != null &&
                    resolvedOptions.popUpToInclusive &&
                    targetIndex == backStack.lastIndex
                ) {
                    onExternalReplace(route)
                    return
                }

                val firstRemovedIndex = if (resolvedOptions.popUpToInclusive) targetIndex else targetIndex + 1
                if (firstRemovedIndex <= backStack.lastIndex) {
                    backStack.subList(firstRemovedIndex, backStack.size).clear()
                }
            }
        }

        if (resolvedOptions.launchSingleTop && currentRoute == route) return
        forwardRoutes.clear()
        if (onExternalNavigate != null) {
            onExternalNavigate.invoke(route, resolvedOptions.launchSingleTop)
        } else {
            openRoute(route)
        }
    }

    private fun openRoute(route: AppRoute) {
        onLocalNavigate?.invoke(currentRoute, route)
        backStack.add(route)
    }

    fun popBackStack(expectedRoute: AppRoute? = null): Boolean {
        if (expectedRoute != null && currentRoute != expectedRoute) return false
        if (backStack.size > 1) {
            onLocalPop?.invoke()
            val left = backStack.removeAt(backStack.lastIndex) as? AppRoute
            // Playback and the sources sheet are not pages to return to.
            if (left != null && left !is PlayerRoute && left !is StreamRoute) forwardRoutes.addLast(left)
            return true
        }
        onExternalBack?.invoke()
        return onExternalBack != null
    }
}

internal class NavigateOptions {
    var launchSingleTop: Boolean = false
    internal var popUpToRoute: KClass<out AppRoute>? = null
    internal var popUpToInclusive: Boolean = false

    inline fun <reified T : AppRoute> popUpTo(noinline options: PopUpToOptions.() -> Unit = {}) {
        val resolved = PopUpToOptions().apply(options)
        popUpToRoute = T::class
        popUpToInclusive = resolved.inclusive
    }
}

internal class PopUpToOptions {
    var inclusive: Boolean = false
}
