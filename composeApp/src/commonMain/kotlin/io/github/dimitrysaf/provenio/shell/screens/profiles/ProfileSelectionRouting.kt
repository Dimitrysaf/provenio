package io.github.dimitrysaf.provenio.shell.screens.profiles

import io.github.dimitrysaf.provenio.shell.components.ToastController
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.profile_already_active
import org.jetbrains.compose.resources.getString
import io.github.dimitrysaf.provenio.core.profiles.Profile

internal suspend fun showAlreadyActiveProfileToast(profile: Profile) {
    ToastController.show(getString(Res.string.profile_already_active, profile.name))
}
