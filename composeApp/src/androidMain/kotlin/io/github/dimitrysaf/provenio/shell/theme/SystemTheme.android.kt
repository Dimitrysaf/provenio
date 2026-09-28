package io.github.dimitrysaf.provenio.shell.theme

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

private const val DarkNavigationScrim = 0xFF020404.toInt()
private const val LightNavigationScrim = 0xE6FFFFFF.toInt()

@Composable
internal actual fun systemPrefersDarkTheme(): Boolean = isSystemInDarkTheme()

@Composable
internal actual fun SystemBarsAppearance(darkTheme: Boolean) {
    val activity = LocalContext.current.findComponentActivity() ?: return
    LaunchedEffect(activity, darkTheme) {
        activity.enableEdgeToEdge(
            statusBarStyle = if (darkTheme) {
                SystemBarStyle.dark(Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
            },
            navigationBarStyle = if (darkTheme) {
                SystemBarStyle.dark(DarkNavigationScrim)
            } else {
                SystemBarStyle.light(LightNavigationScrim, DarkNavigationScrim)
            },
        )
    }
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findComponentActivity()
    else -> null
}
