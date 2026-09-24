package io.github.dimitrysaf.provenio.core.profiles

internal fun routeProfileSelection(
    profile: Profile,
    isEditMode: Boolean,
    activeProfileIndex: Int? = null,
    onEditProfile: (Profile) -> Unit,
    onActiveProfileSelected: (Profile) -> Unit,
    onPinRequired: (Profile) -> Unit,
    onProfileSelected: (Profile) -> Unit,
) {
    when {
        isEditMode -> onEditProfile(profile)
        profile.profileIndex == activeProfileIndex -> onActiveProfileSelected(profile)
        profile.pinEnabled -> onPinRequired(profile)
        else -> onProfileSelected(profile)
    }
}
