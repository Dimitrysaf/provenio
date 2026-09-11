package io.github.dimitrysaf.provenio.core.platform

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
actual fun MatchHostSystemBars() {
    val view = LocalView.current
    val activity = LocalActivity.current
    // Only a dialog-hosted surface has a second window to correct; anything drawn in the
    // activity's own window already has the flags it needs.
    val provider = view.parent as? DialogWindowProvider

    DisposableEffect(view, provider, activity) {
        val host = activity?.window?.decorView
        val hostShowsBars = host == null || ViewCompat.getRootWindowInsets(host)
            ?.isVisible(WindowInsetsCompat.Type.systemBars()) != false

        if (provider != null && !hostShowsBars) {
            val window = provider.window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, window.decorView).apply {
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose { }
    }
}
