package io.github.dimitrysaf.provenio.shell.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.ktx.themeColorOrNull
import com.materialkolor.ktx.toHct
import io.github.dimitrysaf.provenio.core.settings.AppIconOption
import io.github.dimitrysaf.provenio.core.settings.previewResource
import org.jetbrains.compose.resources.imageResource

@Composable
internal fun rememberIconColorScheme(icon: AppIconOption, darkTheme: Boolean): ColorScheme? {
    val bitmap = imageResource(icon.previewResource)
    val seed = remember(bitmap) { bitmap.seedColor() }
    return remember(seed, darkTheme) {
        seed?.let {
            dynamicColorScheme(
                seedColor = it,
                isDark = darkTheme,
                style = if (it.toHct().chroma < NeutralSeedChroma) PaletteStyle.Neutral else PaletteStyle.TonalSpot,
            )
        }
    }
}

private fun ImageBitmap.seedColor(): Color? = themeColorOrNull() ?: themeColorOrNull(filter = false)

private const val NeutralSeedChroma = 16.0
