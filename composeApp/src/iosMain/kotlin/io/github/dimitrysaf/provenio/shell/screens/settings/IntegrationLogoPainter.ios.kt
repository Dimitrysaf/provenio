package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import io.github.dimitrysaf.provenio.shell.screens.simkl.SimklBrandAsset
import io.github.dimitrysaf.provenio.shell.screens.simkl.simklBrandPainter
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.introdb_favicon
import provenio.composeapp.generated.resources.mdblist_logo
import provenio.composeapp.generated.resources.rating_tmdb
import provenio.composeapp.generated.resources.trakt_tv_favicon
import org.jetbrains.compose.resources.painterResource

@Composable
internal actual fun integrationLogoPainter(logo: IntegrationLogo): Painter =
    when (logo) {
        IntegrationLogo.Tmdb -> painterResource(Res.drawable.rating_tmdb)
        IntegrationLogo.Trakt -> painterResource(Res.drawable.trakt_tv_favicon)
        IntegrationLogo.Simkl -> simklBrandPainter(SimklBrandAsset.Glyph)
        IntegrationLogo.MdbList -> painterResource(Res.drawable.mdblist_logo)
        IntegrationLogo.IntroDb -> painterResource(Res.drawable.introdb_favicon)
    }
