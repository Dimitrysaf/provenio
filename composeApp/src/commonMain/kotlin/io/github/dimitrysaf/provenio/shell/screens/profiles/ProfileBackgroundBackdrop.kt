package io.github.dimitrysaf.provenio.shell.screens.profiles

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.dimitrysaf.provenio.core.profiles.Profile
import io.github.dimitrysaf.provenio.shell.components.ProfileMeshBackground

/** The backdrop behind a profile's screens: a mesh in the profile's colour. */
@Composable
fun ProfileBackgroundBackdrop(
    profile: Profile?,
    modifier: Modifier = Modifier,
) {
    val profileColor = remember(profile?.avatarColorHex) {
        profile?.avatarColorHex?.let(::parseHexColor) ?: Color(0xFF1E88E5)
    }
    ProfileMeshBackground(
        profileColor = profileColor,
        modifier = modifier,
    )
}
