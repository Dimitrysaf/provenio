package io.github.dimitrysaf.provenio.core.settings

// The desktop entry's icon comes from the Flatpak and cannot be switched at runtime.
internal actual object AppIconPlatform {
    actual val requiresCloseConfirmation: Boolean = false

    actual fun currentIconName(): String? = null

    actual suspend fun activateIcon(name: String?): Boolean = false
}
