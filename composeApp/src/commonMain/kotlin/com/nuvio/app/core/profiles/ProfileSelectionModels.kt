package com.nuvio.app.core.profiles

internal fun routeProfileSelection(
    profile: NuvioProfile,
    isEditMode: Boolean,
    activeProfileIndex: Int? = null,
    onEditProfile: (NuvioProfile) -> Unit,
    onActiveProfileSelected: (NuvioProfile) -> Unit,
    onPinRequired: (NuvioProfile) -> Unit,
    onProfileSelected: (NuvioProfile) -> Unit,
) {
    when {
        isEditMode -> onEditProfile(profile)
        profile.profileIndex == activeProfileIndex -> onActiveProfileSelected(profile)
        profile.pinEnabled -> onPinRequired(profile)
        else -> onProfileSelected(profile)
    }
}
