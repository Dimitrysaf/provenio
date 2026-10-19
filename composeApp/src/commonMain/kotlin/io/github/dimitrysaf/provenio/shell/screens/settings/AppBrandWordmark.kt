package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.painterResource
import io.github.dimitrysaf.provenio.core.settings.AppIconOption
import io.github.dimitrysaf.provenio.core.settings.AppIconRepository
import io.github.dimitrysaf.provenio.core.settings.wordmarkResource
import io.github.dimitrysaf.provenio.shell.theme.rememberIconColorScheme
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.app_logo_symbol

@Composable
internal fun AppBrandWordmark(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    icon: AppIconOption? = null,
) {
    val state by remember {
        AppIconRepository.ensureLoaded()
        AppIconRepository.state
    }.collectAsStateWithLifecycle()
    Image(
        painter = painterResource(
            (icon ?: state.selected).wordmarkResource,
        ),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}

/**
 * The app's symbol alone, without the icon's background, in the colour of the app icon chosen
 * in Appearance, so it matches what the launcher shows.
 */
@Composable
internal fun AppBrandSymbol(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val state by remember {
        AppIconRepository.ensureLoaded()
        AppIconRepository.state
    }.collectAsStateWithLifecycle()
    val darkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val tint = rememberIconColorScheme(state.selected, darkTheme)?.primary ?: MaterialTheme.colorScheme.primary
    Icon(
        painter = painterResource(Res.drawable.app_logo_symbol),
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint,
    )
}
