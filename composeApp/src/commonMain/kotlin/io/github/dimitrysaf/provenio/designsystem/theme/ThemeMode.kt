package io.github.dimitrysaf.provenio.designsystem.theme

import io.github.dimitrysaf.provenio.resources.Res
import io.github.dimitrysaf.provenio.resources.theme_amoled
import io.github.dimitrysaf.provenio.resources.theme_dark
import io.github.dimitrysaf.provenio.resources.theme_device
import io.github.dimitrysaf.provenio.resources.theme_light
import org.jetbrains.compose.resources.StringResource

enum class ThemeMode(val label: StringResource) {
    System(Res.string.theme_device),
    Light(Res.string.theme_light),
    Dark(Res.string.theme_dark),
    Amoled(Res.string.theme_amoled),
}
