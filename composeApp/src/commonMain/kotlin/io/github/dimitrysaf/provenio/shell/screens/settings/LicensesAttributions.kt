package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.dimitrysaf.provenio.shell.components.ListSubheader
import io.github.dimitrysaf.provenio.shell.components.ScreenScaffold
import io.github.dimitrysaf.provenio.core.cloud.PremiumizeCloudLibraryPosterUrl
import io.github.dimitrysaf.provenio.core.cloud.TorboxCloudLibraryPosterUrl
import io.github.dimitrysaf.provenio.core.cloud.cloudLibraryDisplayArtworkUrl
import io.github.dimitrysaf.provenio.core.build.AppFeaturePolicy
import io.github.dimitrysaf.provenio.core.build.isDesktop
import io.github.dimitrysaf.provenio.core.build.isIos
import provenio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private const val TmdbUrl = "https://www.themoviedb.org"
private const val ImdbUrl = "https://www.imdb.com"
private const val TraktUrl = "https://trakt.tv"
private const val SimklUrl = "https://simkl.com"
private const val PremiumizeUrl = "https://www.premiumize.me"
private const val TorboxUrl = "https://torbox.app"
private const val MdbListUrl = "https://mdblist.com"
private const val IntroDbUrl = "https://introdb.app/"
private const val JustWatchUrl = "https://www.justwatch.com"
private const val StreamingAvailabilityUrl = "https://www.movieofthenight.com/about/api"
private const val RealDebridUrl = "https://real-debrid.com"
private const val StremioUrl = "https://www.stremio.com"
private const val YouTubeUrl = "https://www.youtube.com"
private const val InternetArchiveUrl = "https://archive.org"
private const val AniSkipUrl = "https://aniskip.com"
private const val AnimeSkipUrl = "https://anime-skip.com"
private const val TiffaraUrl = "https://api.tiffara.com"
private const val RepositoryUrl = "https://github.com/Dimitrysaf/provenio"
private const val NuvioMobileUrl = "https://github.com/NuvioMedia/NuvioMobile"
private const val MpvKitUrl = "https://github.com/mpvkit/MPVKit"
// The fork commit composeApp/libs' Media3 AARs were built from.
private const val Media3ForkUrl = "https://github.com/moneytoo/media/tree/9a3eaf3b73128d74aeb7460fcc2b0f9bd11e3f33"
private data class AttributionItem(
    val titleRes: StringResource,
    val bodyRes: StringResource,
    val logo: IntegrationLogo?,
    val logoUrl: String? = null,
    val link: String,
)

private data class LicenseItem(
    val titleRes: StringResource,
    val bodyRes: StringResource,
    val licenseRes: StringResource,
    val link: String,
)

/** An open-source component the app ships, with its licence as its project states it. */
private data class ComponentCredit(
    val name: String,
    val license: String,
    val link: String,
)

@Composable
fun LicensesAttributionsSettingsScreen(
    onBack: () -> Unit,
) {
    ScreenScaffold(
        title = stringResource(Res.string.compose_settings_page_licenses_attributions),
        modifier = Modifier.fillMaxSize(),
        onBack = onBack,
        contentMaxWidth = SettingsContentMaxWidth,
    ) {
        licensesAttributionsContent(isTablet = false)
    }
}

internal fun LazyListScope.licensesAttributionsContent(
    isTablet: Boolean,
) {
    item {
        LicensesAttributionsBody(isTablet = isTablet)
    }
}

@Composable
private fun LicensesAttributionsBody(
    isTablet: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(if (isTablet) 28.dp else 24.dp),
    ) {
        LicenseSection(stringResource(Res.string.settings_licenses_attributions_section_app)) {
            licenseRow(appLicenseItem())
            licenseRow(upstreamLicenseItem())
        }

        LicenseSection(stringResource(Res.string.settings_licenses_attributions_section_data)) {
            attributionItems().forEach { item -> attributionRow(item, isTablet) }
        }

        LicenseSection(stringResource(Res.string.settings_licenses_attributions_section_playback)) {
            licenseRow(platformLicenseItem())
            playbackLibraries().forEach { item -> componentRow(item) }
        }

        if (AppFeaturePolicy.p2pEnabled) {
            LicenseSection(stringResource(Res.string.settings_licenses_attributions_section_p2p)) {
                p2pEngineLibraries.forEach { item -> componentRow(item) }
            }
        }

        LicenseSection(stringResource(Res.string.settings_licenses_attributions_section_libraries)) {
            appLibraries().forEach { item -> componentRow(item) }
        }
    }
}

/** A named group of credits, as one segmented list. */
@Composable
private fun LicenseSection(
    title: String,
    content: @Composable SettingsListScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ListSubheader(text = title)
        Spacer(modifier = Modifier.height(10.dp))
        SettingsList(content = content)
    }
}

/** Something the app is built on, and where its licence lives. */
// Declares rows, so it must not be skippable: see SettingsListScope.
@Composable
@NonRestartableComposable
private fun SettingsListScope.licenseRow(item: LicenseItem) {
    val uriHandler = LocalUriHandler.current
    linkedRow(
        title = stringResource(item.titleRes),
        body = stringResource(item.bodyRes) + "\n" + stringResource(item.licenseRes),
        link = item.link,
        onOpen = { uriHandler.openUri(item.link) },
    )
}

/** A component the app is built with, and its licence. */
// Declares rows, so it must not be skippable: see SettingsListScope.
@Composable
@NonRestartableComposable
private fun SettingsListScope.componentRow(item: ComponentCredit) {
    val uriHandler = LocalUriHandler.current
    linkedRow(
        title = item.name,
        body = item.license,
        link = item.link,
        onOpen = { uriHandler.openUri(item.link) },
    )
}

/** Somewhere the app's data comes from, shown with its own mark where it has one. */
// Declares rows, so it must not be skippable: see SettingsListScope.
@Composable
@NonRestartableComposable
private fun SettingsListScope.attributionRow(item: AttributionItem, isTablet: Boolean) {
    val uriHandler = LocalUriHandler.current
    val title = stringResource(item.titleRes)
    // Resolved out here: a `val` inside a when branch makes the lambda after it parse as a
    // trailing lambda on that val's own call. Locals also keep the smart casts inside the lambdas.
    val logoUrl = item.logoUrl
    val logoPainter = item.logo?.let { logo -> integrationLogoPainter(logo) }
    val leading: (@Composable () -> Unit)? = when {
        logoPainter != null -> {
            { IntegrationLogoImage(painter = logoPainter, contentDescription = title, isTablet = isTablet) }
        }
        logoUrl != null -> {
            { ProviderLogoImage(url = logoUrl, contentDescription = title, isTablet = isTablet) }
        }
        else -> null
    }

    linkedRow(
        title = title,
        body = stringResource(item.bodyRes),
        link = item.link,
        leadingContent = leading,
        onOpen = { uriHandler.openUri(item.link) },
    )
}

/** A credit that opens its source. The link rides the supporting text, as it did before. */
private fun SettingsListScope.linkedRow(
    title: String,
    body: String,
    link: String,
    onOpen: () -> Unit,
    leadingContent: (@Composable () -> Unit)? = null,
) {
    navigationRow(
        title = title,
        description = body + "\n" + link,
        leadingContent = leadingContent,
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                contentDescription = null,
            )
        },
        onClick = onOpen,
    )
}

@Composable
private fun IntegrationLogoImage(
    painter: Painter,
    contentDescription: String,
    isTablet: Boolean,
) {
    Image(
        painter = painter,
        contentDescription = contentDescription,
        modifier = Modifier
            .size(if (isTablet) 40.dp else 36.dp),
        contentScale = ContentScale.Fit,
    )
}

@Composable
private fun ProviderLogoImage(
    url: String,
    contentDescription: String,
    isTablet: Boolean,
) {
    AsyncImage(
        model = url,
        contentDescription = contentDescription,
        modifier = Modifier
            .size(if (isTablet) 40.dp else 36.dp),
        contentScale = ContentScale.Fit,
    )
}

private fun attributionItems(): List<AttributionItem> = listOf(
    AttributionItem(
        titleRes = Res.string.settings_licenses_attributions_tmdb_title,
        bodyRes = Res.string.settings_licenses_attributions_tmdb_body,
        logo = IntegrationLogo.Tmdb,
        link = TmdbUrl,
    ),
    AttributionItem(
        titleRes = Res.string.settings_licenses_attributions_trakt_title,
        bodyRes = Res.string.settings_licenses_attributions_trakt_body,
        logo = IntegrationLogo.Trakt,
        link = TraktUrl,
    ),
    AttributionItem(
        titleRes = Res.string.settings_licenses_attributions_simkl_title,
        bodyRes = Res.string.settings_licenses_attributions_simkl_body,
        logo = IntegrationLogo.Simkl,
        link = SimklUrl,
    ),
    AttributionItem(
        titleRes = Res.string.settings_licenses_attributions_premiumize_title,
        bodyRes = Res.string.settings_licenses_attributions_premiumize_body,
        logo = null,
        logoUrl = PremiumizeCloudLibraryPosterUrl,
        link = PremiumizeUrl,
    ),
    AttributionItem(
        titleRes = Res.string.settings_licenses_attributions_torbox_title,
        bodyRes = Res.string.settings_licenses_attributions_torbox_body,
        logo = null,
        logoUrl = cloudLibraryDisplayArtworkUrl(TorboxCloudLibraryPosterUrl),
        link = TorboxUrl,
    ),
    AttributionItem(
        titleRes = Res.string.settings_licenses_attributions_realdebrid_title,
        bodyRes = Res.string.settings_licenses_attributions_realdebrid_body,
        logo = null,
        link = RealDebridUrl,
    ),
    AttributionItem(
        titleRes = Res.string.settings_licenses_attributions_mdblist_title,
        bodyRes = Res.string.settings_licenses_attributions_mdblist_body,
        logo = IntegrationLogo.MdbList,
        link = MdbListUrl,
    ),
    AttributionItem(
        titleRes = Res.string.settings_licenses_attributions_justwatch_title,
        bodyRes = Res.string.settings_licenses_attributions_justwatch_body,
        logo = null,
        link = JustWatchUrl,
    ),
    AttributionItem(
        titleRes = Res.string.settings_licenses_attributions_streaming_availability_title,
        bodyRes = Res.string.settings_licenses_attributions_streaming_availability_body,
        logo = null,
        link = StreamingAvailabilityUrl,
    ),
    AttributionItem(
        titleRes = Res.string.settings_licenses_attributions_introdb_title,
        bodyRes = Res.string.settings_licenses_attributions_introdb_body,
        logo = IntegrationLogo.IntroDb,
        link = IntroDbUrl,
    ),
    AttributionItem(
        titleRes = Res.string.settings_licenses_attributions_aniskip_title,
        bodyRes = Res.string.settings_licenses_attributions_aniskip_body,
        logo = null,
        link = AniSkipUrl,
    ),
    AttributionItem(
        titleRes = Res.string.settings_licenses_attributions_animeskip_title,
        bodyRes = Res.string.settings_licenses_attributions_animeskip_body,
        logo = null,
        link = AnimeSkipUrl,
    ),
    AttributionItem(
        titleRes = Res.string.settings_licenses_attributions_tiffara_title,
        bodyRes = Res.string.settings_licenses_attributions_tiffara_body,
        logo = null,
        link = TiffaraUrl,
    ),
    AttributionItem(
        titleRes = Res.string.settings_licenses_attributions_stremio_title,
        bodyRes = Res.string.settings_licenses_attributions_stremio_body,
        logo = null,
        link = StremioUrl,
    ),
    AttributionItem(
        titleRes = Res.string.settings_licenses_attributions_youtube_title,
        bodyRes = Res.string.settings_licenses_attributions_youtube_body,
        logo = null,
        link = YouTubeUrl,
    ),
    AttributionItem(
        titleRes = Res.string.settings_licenses_attributions_internet_archive_title,
        bodyRes = Res.string.settings_licenses_attributions_internet_archive_body,
        logo = null,
        link = InternetArchiveUrl,
    ),
    AttributionItem(
        titleRes = Res.string.settings_licenses_attributions_imdb_title,
        bodyRes = Res.string.settings_licenses_attributions_imdb_body,
        logo = null,
        link = ImdbUrl,
    ),
)

private fun appLicenseItem(): LicenseItem =
    LicenseItem(
        titleRes = Res.string.settings_licenses_attributions_upstream_title,
        bodyRes = Res.string.settings_licenses_attributions_upstream_body,
        licenseRes = Res.string.settings_licenses_attributions_upstream_license,
        link = RepositoryUrl,
    )

private fun upstreamLicenseItem(): LicenseItem =
    LicenseItem(
        titleRes = Res.string.settings_licenses_attributions_nuvio_title,
        bodyRes = Res.string.settings_licenses_attributions_nuvio_body,
        licenseRes = Res.string.settings_licenses_attributions_upstream_license,
        link = NuvioMobileUrl,
    )

private fun platformLicenseItem(): LicenseItem =
    if (isIos) {
        LicenseItem(
            titleRes = Res.string.settings_licenses_attributions_mpvkit_title,
            bodyRes = Res.string.settings_licenses_attributions_mpvkit_body,
            licenseRes = Res.string.settings_licenses_attributions_mpvkit_license,
            link = MpvKitUrl,
        )
    } else {
        LicenseItem(
            titleRes = Res.string.settings_licenses_attributions_exoplayer_title,
            bodyRes = Res.string.settings_licenses_attributions_exoplayer_body,
            licenseRes = Res.string.settings_licenses_attributions_exoplayer_license,
            link = Media3ForkUrl,
        )
    }

// What each build plays with besides the player above. Licences are as each project and the
// shipped binaries state them: Android's mpv and both desktop libmpv builds use GPL FFmpeg
// builds, while the Media3 FFmpeg extension and the Linux FFmpeg are LGPL builds.
private fun playbackLibraries(): List<ComponentCredit> = when {
    isIos -> emptyList()
    isDesktop -> listOf(
        ComponentCredit(
            name = "mpv",
            license = "GPL-2.0-or-later",
            link = "https://github.com/mpv-player/mpv",
        ),
        ComponentCredit(
            name = "libmpv for Windows (mpv-winbuild-cmake)",
            license = "The prebuilt libmpv the Windows build ships, with FFmpeg under GPL-3.0-or-later.",
            link = "https://github.com/shinchiro/mpv-winbuild-cmake",
        ),
        ComponentCredit(
            name = "FFmpeg",
            license = "LGPL-2.1-or-later on Linux, GPL-3.0-or-later inside the Windows libmpv.",
            link = "https://github.com/FFmpeg/FFmpeg",
        ),
        ComponentCredit(
            name = "libplacebo",
            license = "LGPL-2.1-or-later",
            link = "https://code.videolan.org/videolan/libplacebo",
        ),
        ComponentCredit(
            name = "libass",
            license = "ISC",
            link = "https://github.com/libass/libass",
        ),
    )
    else -> listOf(
        ComponentCredit(
            name = "Media3 FFmpeg extension",
            license = "Apache-2.0, with FFmpeg 6.0 under LGPL-2.1-or-later.",
            link = "$Media3ForkUrl/libraries/decoder_ffmpeg",
        ),
        ComponentCredit(
            name = "Media3 AV1 extension",
            license = "Apache-2.0, with libgav1 under Apache-2.0.",
            link = "$Media3ForkUrl/libraries/decoder_av1",
        ),
        ComponentCredit(
            name = "mpv-android-lib",
            license = "MIT. Bundles mpv and FFmpeg (GPL-3.0-or-later as built), libass (ISC), libplacebo " +
                "and FriBidi (LGPL-2.1-or-later), FreeType (FreeType License), HarfBuzz, Lua and libxml2 " +
                "(MIT), mbedTLS (Apache-2.0) and dav1d (BSD-2-Clause).",
            link = "https://github.com/abdallahmehiz/mpv-android",
        ),
        ComponentCredit(
            name = "libass-android",
            license = "MIT. Bundles libass (ISC).",
            link = "https://github.com/peerless2012/libass-android",
        ),
    )
}

// The torrent engine, and its inputs as engine/THIRD_PARTY_NOTICES.md pins them.
private val p2pEngineLibraries = listOf(
    ComponentCredit(
        name = "Provenio engine",
        license = "GPL-3.0-or-later",
        link = "$RepositoryUrl/tree/main/engine",
    ),
    ComponentCredit(
        name = "libtorrent 2.0.12",
        license = "BSD-3-Clause",
        link = "https://github.com/arvidn/libtorrent",
    ),
    ComponentCredit(
        name = "Boost 1.86.0",
        license = "BSL-1.0",
        link = "https://www.boost.org",
    ),
    ComponentCredit(
        name = "OpenSSL 3.5.7",
        license = "Apache-2.0",
        link = "https://www.openssl.org",
    ),
)

private fun appLibraries(): List<ComponentCredit> = buildList {
    add(ComponentCredit("Kotlin and kotlinx", "Apache-2.0", "https://kotlinlang.org"))
    add(ComponentCredit("Compose Multiplatform", "Apache-2.0", "https://github.com/JetBrains/compose-multiplatform"))
    if (!isIos && !isDesktop) {
        add(ComponentCredit("AndroidX, including CameraX and WorkManager", "Apache-2.0", "https://developer.android.com/jetpack/androidx"))
        add(ComponentCredit("desugar_jdk_libs", "GPL-2.0-only WITH Classpath-exception-2.0", "https://github.com/google/desugar_jdk_libs"))
    }
    add(ComponentCredit("Ktor", "Apache-2.0", "https://ktor.io"))
    if (!isIos) {
        add(ComponentCredit("OkHttp", "Apache-2.0", "https://square.github.io/okhttp/"))
        add(ComponentCredit("ZXing", "Apache-2.0", "https://github.com/zxing/zxing"))
    }
    add(ComponentCredit("Coil", "Apache-2.0", "https://coil-kt.github.io/coil/"))
    add(ComponentCredit("JetBrains Markdown", "Apache-2.0", "https://github.com/JetBrains/markdown"))
    add(ComponentCredit("Kermit", "Apache-2.0", "https://github.com/touchlab/Kermit"))
    add(ComponentCredit("Reorderable", "Apache-2.0", "https://github.com/Calvin-LL/Reorderable"))
    add(ComponentCredit("MaterialKolor", "MIT", "https://github.com/jordond/materialkolor"))
    if (isDesktop) {
        add(ComponentCredit("JNA", "LGPL-2.1-or-later or Apache-2.0", "https://github.com/java-native-access/jna"))
    }
    add(ComponentCredit("Roboto", "OFL-1.1", "https://github.com/googlefonts/roboto-classic"))
}
