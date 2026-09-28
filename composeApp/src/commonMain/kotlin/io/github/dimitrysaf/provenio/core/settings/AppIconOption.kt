package io.github.dimitrysaf.provenio.core.settings

internal enum class AppIconOption(
    val key: String,
    val platformName: String?,
) {
    ARCTIC_BLUE(
        key = "arctic_blue",
        platformName = "AppIconArcticBlue",
    ),
    ORIGINAL(
        key = "original",
        platformName = "AppIconOriginal",
    ),
    EMERALD(
        key = "emerald",
        platformName = "AppIconEmerald",
    ),
    ROSE_GOLD(
        key = "rose_gold",
        platformName = "AppIconRoseGold",
    ),
    COPPER(
        key = "copper",
        platformName = "AppIconCopper",
    ),
    GRAPHITE(
        key = "graphite",
        platformName = "AppIconGraphite",
    ),
    ;

    companion object {
        val DEFAULT = ARCTIC_BLUE

        fun fromPlatformName(name: String?): AppIconOption =
            entries.firstOrNull { it.platformName == name } ?: DEFAULT

        fun fromKey(key: String?): AppIconOption? =
            entries.firstOrNull { it.key == key }
    }
}
