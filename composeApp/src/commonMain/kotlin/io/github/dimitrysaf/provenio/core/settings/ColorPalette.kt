package io.github.dimitrysaf.provenio.core.settings

internal sealed interface ColorPalette {
    val key: String

    data object Dynamic : ColorPalette {
        override val key: String = "dynamic"
    }

    data class Icon(val icon: AppIconOption) : ColorPalette {
        override val key: String get() = icon.key
    }

    companion object {
        fun fromKey(key: String?): ColorPalette =
            AppIconOption.fromKey(key)?.let(::Icon) ?: Dynamic
    }
}
