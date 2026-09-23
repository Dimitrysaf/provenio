package com.nuvio.app.features.profiles

import com.nuvio.app.shell.components.NuvioToastController
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.profile_already_active
import org.jetbrains.compose.resources.getString
import com.nuvio.app.core.profiles.NuvioProfile

internal suspend fun showAlreadyActiveProfileToast(profile: NuvioProfile) {
    NuvioToastController.show(getString(Res.string.profile_already_active, profile.name))
}
