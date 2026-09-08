package io.github.dimitrysaf.provenio.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alorma.compose.settings.ui.expressive.SettingsMenuLink

/**
 * The one settings row in the app.
 *
 * Every settings surface goes through this so they cannot drift apart. Two things are
 * deliberately not the library defaults:
 *
 * `ListItemDefaults.colors()` puts the container on `surface`, which in a dark scheme is
 * *darker* than the page behind it — the row sinks instead of sitting on top. `surfaceBright`
 * is the brightest surface role in both light and dark, so the row reads as a layer above
 * the page either way.
 *
 * The default shape is also much squarer than the platform settings it is modelled on.
 */
@Composable
fun settingsTileColors(): ListItemColors = ListItemDefaults.colors(
    containerColor = MaterialTheme.colorScheme.surfaceBright,
    contentColor = MaterialTheme.colorScheme.onSurface,
)

@Composable
fun settingsTileShapes(): ListItemShapes =
    ListItemDefaults.shapes(shape = RoundedCornerShape(28.dp))

@Composable
fun SettingsTile(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    action: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    SettingsMenuLink(
        title = title,
        modifier = modifier,
        subtitle = subtitle,
        icon = icon,
        enabled = enabled,
        action = action,
        colors = settingsTileColors(),
        shapes = settingsTileShapes(),
        onClick = onClick,
    )
}
