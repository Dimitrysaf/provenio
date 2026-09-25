package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.painterResource
import io.github.dimitrysaf.provenio.core.settings.AppIconOption
import io.github.dimitrysaf.provenio.core.settings.AppIconRepository
import io.github.dimitrysaf.provenio.core.settings.wordmarkResource

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
