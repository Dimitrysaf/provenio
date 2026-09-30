package io.github.dimitrysaf.provenio.core.settings

internal enum class DisplayScaleOption(val key: String, val scale: Float?) {
    AUTOMATIC("automatic", null),
    SCALE_75("75", 0.75f),
    SCALE_100("100", 1f),
    SCALE_125("125", 1.25f),
    SCALE_150("150", 1.5f),
    SCALE_175("175", 1.75f),
    SCALE_200("200", 2f),
    SCALE_250("250", 2.5f),
    SCALE_300("300", 3f),
    ;

    companion object {
        fun fromKey(key: String?): DisplayScaleOption = entries.firstOrNull { it.key == key } ?: AUTOMATIC
    }
}

internal expect val displayScaleSettingSupported: Boolean
