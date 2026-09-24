package io.github.dimitrysaf.provenio.shell.components

import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSUserDefaults

private const val nativeSelectedTabKey = "ProvenioNativeSelectedTab"

private const val nativeTabAccentColorKey = "ProvenioNativeTabAccentColor"

private const val nativeTabTitleHomeKey = "ProvenioNativeTabTitleHome"

private const val nativeTabTitleSearchKey = "ProvenioNativeTabTitleSearch"

private const val nativeTabTitleLibraryKey = "ProvenioNativeTabTitleLibrary"

private const val nativeTabTitleProfileKey = "ProvenioNativeTabTitleProfile"

private const val nativeProfileNameKey = "ProvenioNativeProfileName"

private const val nativeProfileAvatarColorKey = "ProvenioNativeProfileAvatarColor"

private const val nativeProfileAvatarUrlKey = "ProvenioNativeProfileAvatarURL"

private const val nativeProfileAvatarBackgroundColorKey = "ProvenioNativeProfileAvatarBackgroundColor"

private const val nativeTabChromeDidChangeNotification = "ProvenioNativeTabChromeDidChange"

internal actual fun publishNativeSelectedTab(tabName: String) {
    NSUserDefaults.standardUserDefaults.setObject(tabName, forKey = nativeSelectedTabKey)
    notifyNativeTabChromeChanged()
}

internal actual fun publishNativeTabAccentColor(hexColor: String) {
    NSUserDefaults.standardUserDefaults.setObject(hexColor, forKey = nativeTabAccentColorKey)
    notifyNativeTabChromeChanged()
}

internal actual fun publishNativeTabTitles(
    home: String,
    search: String,
    library: String,
    profile: String,
) {
    publishString(nativeTabTitleHomeKey, home)
    publishString(nativeTabTitleSearchKey, search)
    publishString(nativeTabTitleLibraryKey, library)
    publishString(nativeTabTitleProfileKey, profile)
    notifyNativeTabChromeChanged()
}

internal actual fun publishNativeProfileTabIcon(
    name: String?,
    avatarColorHex: String?,
    avatarImageUrl: String?,
    avatarBackgroundColorHex: String?,
) {
    publishString(nativeProfileNameKey, name)
    publishString(nativeProfileAvatarColorKey, avatarColorHex)
    publishString(nativeProfileAvatarUrlKey, avatarImageUrl)
    publishString(nativeProfileAvatarBackgroundColorKey, avatarBackgroundColorHex)
    notifyNativeTabChromeChanged()
}

private fun publishString(key: String, value: String?) {
    if (value.isNullOrBlank()) {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(key)
    } else {
        NSUserDefaults.standardUserDefaults.setObject(value, forKey = key)
    }
}

private fun notifyNativeTabChromeChanged() {
    NSNotificationCenter.defaultCenter.postNotificationName(nativeTabChromeDidChangeNotification, null)
}
