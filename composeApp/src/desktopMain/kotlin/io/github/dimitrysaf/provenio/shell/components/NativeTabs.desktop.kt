package io.github.dimitrysaf.provenio.shell.components

internal actual fun publishNativeSelectedTab(tabName: String) = Unit

internal actual fun publishNativeTabAccentColor(hexColor: String) = Unit

internal actual fun publishNativeTabTitles(
    home: String,
    search: String,
    library: String,
    profile: String,
) = Unit

internal actual fun publishNativeProfileTabIcon(
    name: String?,
    avatarColorHex: String?,
    avatarImageUrl: String?,
    avatarBackgroundColorHex: String?,
) = Unit
