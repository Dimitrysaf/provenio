package com.nuvio.app.features.settings

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
import com.nuvio.app.core.ui.ListSubheader
import com.nuvio.app.core.ui.NuvioScreen
import com.nuvio.app.features.cloud.PremiumizeCloudLibraryPosterUrl
import com.nuvio.app.features.cloud.TorboxCloudLibraryPosterUrl
import com.nuvio.app.features.cloud.cloudLibraryDisplayArtworkUrl
import com.nuvio.app.isIos
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private const val TmdbUrl = "https://www.themoviedb.org"
private const val ImdbDatasetsUrl = "https://developer.imdb.com/non-commercial-datasets/"
private const val TraktUrl = "https://trakt.tv"
private const val SimklUrl = "https://simkl.com"
private const val PremiumizeUrl = "https://www.premiumize.me"
private const val TorboxUrl = "https://torbox.app"
private const val MdbListUrl = "https://mdblist.com"
private const val IntroDbUrl = "https://introdb.app/"
private const val NuvioRepositoryUrl = "https://github.com/NuvioMedia/NuvioMobile"
private const val MpvKitUrl = "https://github.com/mpvkit/MPVKit"
private const val ApacheLicenseUrl = "https://www.apache.org/licenses/LICENSE-2.0"
private const val HazeLicenseUrl = "https://github.com/chrisbanes/haze/blob/1.7.2/LICENSE"

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

@Composable
fun LicensesAttributionsSettingsScreen(
    onBack: () -> Unit,
) {
    NuvioScreen(
        title = stringResource(Res.string.compose_settings_page_licenses_attributions),
        modifier = Modifier.fillMaxSize(),
        onBack = onBack,
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
        }

        LicenseSection(stringResource(Res.string.settings_licenses_attributions_section_data)) {
            attributionItems().forEach { item -> attributionRow(item, isTablet) }
        }

        LicenseSection(stringResource(Res.string.settings_licenses_attributions_section_playback)) {
            licenseRow(platformLicenseItem())
        }

        LicenseSection(stringResource(Res.string.settings_licenses_attributions_section_ui)) {
            licenseRow(
                LicenseItem(
                    titleRes = Res.string.settings_licenses_attributions_haze_title,
                    bodyRes = Res.string.settings_licenses_attributions_haze_body,
                    licenseRes = Res.string.settings_licenses_attributions_haze_license,
                    link = HazeLicenseUrl,
                ),
            )
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
        titleRes = Res.string.settings_licenses_attributions_mdblist_title,
        bodyRes = Res.string.settings_licenses_attributions_mdblist_body,
        logo = IntegrationLogo.MdbList,
        link = MdbListUrl,
    ),
    AttributionItem(
        titleRes = Res.string.settings_licenses_attributions_introdb_title,
        bodyRes = Res.string.settings_licenses_attributions_introdb_body,
        logo = IntegrationLogo.IntroDb,
        link = IntroDbUrl,
    ),
    AttributionItem(
        titleRes = Res.string.settings_licenses_attributions_imdb_title,
        bodyRes = Res.string.settings_licenses_attributions_imdb_body,
        logo = null,
        link = ImdbDatasetsUrl,
    ),
)

private fun appLicenseItem(): LicenseItem =
    LicenseItem(
        titleRes = Res.string.settings_licenses_attributions_nuvio_title,
        bodyRes = Res.string.settings_licenses_attributions_nuvio_body,
        licenseRes = Res.string.settings_licenses_attributions_nuvio_license,
        link = NuvioRepositoryUrl,
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
            link = ApacheLicenseUrl,
        )
    }
