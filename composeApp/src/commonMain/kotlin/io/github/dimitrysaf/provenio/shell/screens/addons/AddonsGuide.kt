package io.github.dimitrysaf.provenio.shell.screens.addons

import io.github.dimitrysaf.provenio.shell.components.SheetNavigationButton
import io.github.dimitrysaf.provenio.shell.components.SheetNavigation
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.PlayDisabled
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.provenio.core.addons.AddAddonResult
import io.github.dimitrysaf.provenio.core.addons.AddonRepository
import io.github.dimitrysaf.provenio.shell.components.BottomSheetBodyMargin
import io.github.dimitrysaf.provenio.shell.components.ListSubheader
import io.github.dimitrysaf.provenio.shell.components.ModalSheet
import io.github.dimitrysaf.provenio.shell.components.ToastController
import io.github.dimitrysaf.provenio.shell.components.dismissBottomSheet
import io.github.dimitrysaf.provenio.shell.screens.settings.OuterCorner
import io.github.dimitrysaf.provenio.shell.screens.settings.SettingsList
import io.github.dimitrysaf.provenio.shell.screens.settings.SettingsListScope
import io.github.dimitrysaf.provenio.shell.theme.provenio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.*

private const val CinemetaName = "Cinemeta"
private const val CinemetaManifestUrl = "https://v3-cinemeta.strem.io/manifest.json"
private const val TorrentioName = "Torrentio"
private const val TorrentioManifestUrl = "https://torrentio.strem.fun/manifest.json"
private const val AioStreamsName = "AIOStreams"
private const val AioStreamsUrl = "https://github.com/Viren070/AIOStreams"
private const val AioMetadataName = "AIOMetadata"
private const val AioMetadataUrl = "https://github.com/cedya77/aiometadata"
private const val MidnightAddonsUrl = "https://addonsfortheweebs.midnightignite.me/addons"
private const val AioStreamsPublicInstancesUrl = "https://docs.aiostreams.viren070.me/getting-started/public-instances"
private const val CommunityAddonsUrl = "https://stremio-addons.net"
private const val RedditName = "r/StremioAddons"
private const val RedditUrl = "https://www.reddit.com/r/StremioAddons/"

private val ScreenshotMaxWidth = 300.dp

internal object AddonAddRequests {
    private val pending = MutableStateFlow(false)
    val requested: StateFlow<Boolean> = pending.asStateFlow()

    fun request() {
        pending.value = true
    }

    fun consume() {
        pending.value = false
    }
}

private enum class GuideTerm(
    val key: String,
    val titleRes: StringResource,
    val bodyRes: StringResource,
) {
    Manifest("manifest", Res.string.addons_guide_term_manifest_title, Res.string.addons_guide_term_manifest_body),
    Metadata("metadata", Res.string.addons_guide_term_metadata_title, Res.string.addons_guide_term_metadata_body),
    Streams("streams", Res.string.addons_guide_term_streams_title, Res.string.addons_guide_term_streams_body),
    Torrent("torrent", Res.string.addons_guide_term_torrent_title, Res.string.addons_guide_term_torrent_body),
    Debrid("debrid", Res.string.addons_guide_term_debrid_title, Res.string.addons_guide_term_debrid_body),
    Aggregator("aggregator", Res.string.addons_guide_term_aggregator_title, Res.string.addons_guide_term_aggregator_body),
    Instance("instance", Res.string.addons_guide_term_instance_title, Res.string.addons_guide_term_instance_body),
    Configure("configure", Res.string.addons_guide_term_configure_title, Res.string.addons_guide_term_configure_body),
    ;

    companion object {
        fun fromKey(key: String): GuideTerm? = entries.firstOrNull { it.key == key }
    }
}

private val GuideTermPattern = Regex("""\[\[([a-z]+)\|([^\]]+)]]""")

internal fun LazyListScope.addonsGuideContent(
    isTablet: Boolean,
    onAddAddonClick: () -> Unit,
) {
    item {
        AddonsGuideBody(
            isTablet = isTablet,
            onAddAddonClick = onAddAddonClick,
        )
    }
}

@Composable
private fun AddonsGuideBody(
    isTablet: Boolean,
    onAddAddonClick: () -> Unit,
) {
    LaunchedEffect(Unit) {
        AddonRepository.initialize()
    }

    val uiState by AddonRepository.uiState.collectAsStateWithLifecycle()
    val installedKeys = remember(uiState.addons) {
        uiState.addons.map { it.manifestUrl.manifestKey() }.toSet()
    }
    var installingUrl by remember { mutableStateOf<String?>(null) }
    var openTermKey by rememberSaveable { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val openTerm: (GuideTerm) -> Unit = { openTermKey = it.key }
    val openLink: (String) -> Unit = { url -> runCatching { uriHandler.openUri(url) } }

    fun install(manifestUrl: String) {
        if (installingUrl != null || manifestUrl.manifestKey() in installedKeys) return
        installingUrl = manifestUrl
        coroutineScope.launch {
            when (val result = AddonRepository.addAddon(manifestUrl)) {
                is AddAddonResult.Success -> ToastController.show(
                    getString(Res.string.addons_modal_success_message, result.manifest.name),
                )
                is AddAddonResult.Error -> ToastController.show(result.message)
            }
            installingUrl = null
        }
    }

    fun installState(manifestUrl: String): GuideInstallState = when {
        manifestUrl.manifestKey() in installedKeys -> GuideInstallState.Installed
        installingUrl == manifestUrl -> GuideInstallState.Installing
        else -> GuideInstallState.Available
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(if (isTablet) 32.dp else 28.dp),
    ) {
        GuideSection(stringResource(Res.string.addons_guide_section_what)) {
            GuideParagraph(stringResource(Res.string.addons_guide_intro), openTerm)
            GuideParagraph(stringResource(Res.string.addons_guide_kinds_intro), openTerm)
            SettingsList {
                guideTermRow(
                    title = stringResource(Res.string.addons_guide_metadata_title),
                    body = stringResource(Res.string.addons_guide_metadata_body),
                    icon = Icons.Rounded.Movie,
                    term = GuideTerm.Metadata,
                    onTermClick = openTerm,
                )
                guideTermRow(
                    title = stringResource(Res.string.addons_guide_streams_title),
                    body = stringResource(Res.string.addons_guide_streams_body),
                    icon = Icons.Rounded.PlayCircle,
                    term = GuideTerm.Streams,
                    onTermClick = openTerm,
                )
            }
            GuideNotice(
                kind = GuideNoticeKind.Info,
                text = stringResource(Res.string.addons_guide_compat_tip),
                onTermClick = openTerm,
            )
        }

        GuideSection(stringResource(Res.string.addons_guide_section_sources)) {
            GuideParagraph(stringResource(Res.string.addons_guide_no_hosting), openTerm)
            GuideNotice(
                kind = GuideNoticeKind.Warning,
                text = stringResource(Res.string.addons_guide_legal_warning),
                onTermClick = openTerm,
            )
        }

        GuideSection(stringResource(Res.string.addons_guide_section_start)) {
            GuideParagraph(stringResource(Res.string.addons_guide_start_intro), openTerm)
            SettingsList {
                guideInstallRow(
                    title = CinemetaName,
                    body = stringResource(Res.string.addons_guide_cinemeta_body),
                    icon = Icons.Rounded.Movie,
                    state = installState(CinemetaManifestUrl),
                    onTermClick = openTerm,
                    onInstall = { install(CinemetaManifestUrl) },
                )
                guideInstallRow(
                    title = TorrentioName,
                    body = stringResource(Res.string.addons_guide_torrentio_body),
                    icon = Icons.Rounded.PlayCircle,
                    state = installState(TorrentioManifestUrl),
                    onTermClick = openTerm,
                    onInstall = { install(TorrentioManifestUrl) },
                )
            }
            GuideNotice(
                kind = GuideNoticeKind.Warning,
                text = stringResource(Res.string.addons_guide_torrent_warning),
                onTermClick = openTerm,
            )
        }

        GuideSection(stringResource(Res.string.addons_guide_section_aggregators)) {
            GuideParagraph(stringResource(Res.string.addons_guide_aggregators_intro), openTerm)
            GuideScreenshot(
                resource = Res.drawable.addons_guide_streams,
                caption = stringResource(Res.string.addons_guide_streams_screenshot_caption),
            )
            SettingsList {
                guideLinkRow(
                    title = AioStreamsName,
                    body = stringResource(Res.string.addons_guide_aiostreams_body),
                    icon = Icons.Rounded.PlayCircle,
                    onTermClick = openTerm,
                    onOpen = { openLink(AioStreamsUrl) },
                )
                guideLinkRow(
                    title = AioMetadataName,
                    body = stringResource(Res.string.addons_guide_aiometadata_body),
                    icon = Icons.Rounded.Movie,
                    onTermClick = openTerm,
                    onOpen = { openLink(AioMetadataUrl) },
                )
            }
        }

        GuideSection(stringResource(Res.string.addons_guide_section_hosting)) {
            GuideParagraph(stringResource(Res.string.addons_guide_hosting_intro), openTerm)
            SettingsList {
                guideLinkRow(
                    title = stringResource(Res.string.addons_guide_midnight_title),
                    body = stringResource(Res.string.addons_guide_midnight_body),
                    icon = Icons.Rounded.Dns,
                    onTermClick = openTerm,
                    onOpen = { openLink(MidnightAddonsUrl) },
                )
                guideLinkRow(
                    title = stringResource(Res.string.addons_guide_public_instances_title),
                    body = stringResource(Res.string.addons_guide_public_instances_body),
                    icon = Icons.Rounded.Dns,
                    onTermClick = openTerm,
                    onOpen = { openLink(AioStreamsPublicInstancesUrl) },
                )
            }
            GuideScreenshot(
                resource = Res.drawable.addons_guide_configure,
                caption = stringResource(Res.string.addons_guide_configure_screenshot_caption),
            )
        }

        GuideSection(stringResource(Res.string.addons_guide_section_install)) {
            GuideSteps(
                steps = listOf(
                    stringResource(Res.string.addons_guide_step_open),
                    stringResource(Res.string.addons_guide_step_settings),
                    stringResource(Res.string.addons_guide_step_install),
                ),
                onTermClick = openTerm,
            )
            GuideNotice(
                kind = GuideNoticeKind.Info,
                text = stringResource(Res.string.addons_guide_manual_tip),
                onTermClick = openTerm,
                action = {
                    FilledTonalButton(onClick = onAddAddonClick) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                        )
                        Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                        Text(stringResource(Res.string.addons_guide_add_button))
                    }
                },
            )
            GuideNotice(
                kind = GuideNoticeKind.Warning,
                text = stringResource(Res.string.addons_guide_private_link_warning),
                onTermClick = openTerm,
            )
        }

        GuideSection(stringResource(Res.string.addons_guide_section_trouble)) {
            SettingsList {
                guideStaticRow(
                    title = stringResource(Res.string.addons_guide_trouble_home_title),
                    body = stringResource(Res.string.addons_guide_trouble_home_body),
                    icon = Icons.Rounded.Home,
                )
                guideStaticRow(
                    title = stringResource(Res.string.addons_guide_trouble_play_title),
                    body = stringResource(Res.string.addons_guide_trouble_play_body),
                    icon = Icons.Rounded.PlayDisabled,
                )
                guideStaticRow(
                    title = stringResource(Res.string.addons_guide_trouble_order_title),
                    body = stringResource(Res.string.addons_guide_trouble_order_body),
                    icon = Icons.Rounded.SwapVert,
                )
                guideStaticRow(
                    title = stringResource(Res.string.addons_guide_trouble_slow_title),
                    body = stringResource(Res.string.addons_guide_trouble_slow_body),
                    icon = Icons.Rounded.HourglassEmpty,
                )
            }
        }

        GuideSection(stringResource(Res.string.addons_guide_section_more)) {
            SettingsList {
                guideLinkRow(
                    title = stringResource(Res.string.addons_guide_community_title),
                    body = stringResource(Res.string.addons_guide_community_body),
                    icon = Icons.Rounded.Public,
                    onTermClick = openTerm,
                    onOpen = { openLink(CommunityAddonsUrl) },
                )
                guideLinkRow(
                    title = RedditName,
                    body = stringResource(Res.string.addons_guide_reddit_body),
                    icon = Icons.Rounded.Forum,
                    onTermClick = openTerm,
                    onOpen = { openLink(RedditUrl) },
                )
            }
        }
    }

    openTermKey?.let(GuideTerm::fromKey)?.let { term ->
        GuideTermSheet(
            term = term,
            onDismiss = { openTermKey = null },
        )
    }
}

private enum class GuideInstallState { Available, Installing, Installed }

private enum class GuideNoticeKind { Info, Warning }

private fun String.manifestKey(): String = substringBefore("?").trimEnd('/').lowercase()

@Composable
private fun GuideSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ListSubheader(text = title)
        content()
    }
}

@Composable
private fun GuideParagraph(
    text: String,
    onTermClick: (GuideTerm) -> Unit,
) {
    GuideText(
        text = text,
        onTermClick = onTermClick,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun GuideText(
    text: String,
    onTermClick: (GuideTerm) -> Unit,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val currentOnTermClick by rememberUpdatedState(onTermClick)
    val linkColor = MaterialTheme.colorScheme.primary
    val annotated = remember(text, linkColor) {
        buildAnnotatedString {
            var cursor = 0
            GuideTermPattern.findAll(text).forEach { match ->
                append(text.substring(cursor, match.range.first))
                val label = match.groupValues[2]
                val term = GuideTerm.fromKey(match.groupValues[1])
                if (term == null) {
                    append(label)
                } else {
                    withLink(
                        LinkAnnotation.Clickable(
                            tag = term.key,
                            styles = TextLinkStyles(
                                style = SpanStyle(
                                    color = linkColor,
                                    fontWeight = FontWeight.Medium,
                                    textDecoration = TextDecoration.Underline,
                                ),
                            ),
                            linkInteractionListener = { currentOnTermClick(term) },
                        ),
                    ) {
                        append(label)
                    }
                }
                cursor = match.range.last + 1
            }
            append(text.substring(cursor))
        }
    }
    Text(
        text = annotated,
        modifier = modifier,
        style = style,
        color = color,
    )
}

@Composable
private fun GuideNotice(
    kind: GuideNoticeKind,
    text: String,
    onTermClick: (GuideTerm) -> Unit,
    action: (@Composable () -> Unit)? = null,
) {
    val warning = MaterialTheme.provenio.colors.warning
    val containerColor = when (kind) {
        GuideNoticeKind.Info -> MaterialTheme.colorScheme.secondaryContainer
        GuideNoticeKind.Warning -> warning.copy(alpha = 0.12f)
    }
    val iconTint = when (kind) {
        GuideNoticeKind.Info -> MaterialTheme.colorScheme.onSecondaryContainer
        GuideNoticeKind.Warning -> warning
    }
    val textColor = when (kind) {
        GuideNoticeKind.Info -> MaterialTheme.colorScheme.onSecondaryContainer
        GuideNoticeKind.Warning -> MaterialTheme.colorScheme.onSurface
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OuterCorner))
            .background(containerColor)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = when (kind) {
                    GuideNoticeKind.Info -> Icons.Rounded.Info
                    GuideNoticeKind.Warning -> Icons.Rounded.Warning
                },
                contentDescription = null,
                tint = iconTint,
            )
            GuideText(
                text = text,
                onTermClick = onTermClick,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                modifier = Modifier.weight(1f),
            )
        }
        if (action != null) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd,
            ) {
                action()
            }
        }
    }
}

@Composable
private fun GuideSteps(
    steps: List<String>,
    onTermClick: (GuideTerm) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        steps.forEachIndexed { index, step ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = (index + 1).toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                GuideText(
                    text = step,
                    onTermClick = onTermClick,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun GuideScreenshot(
    resource: DrawableResource,
    caption: String,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val shape = RoundedCornerShape(OuterCorner)
        Image(
            painter = painterResource(resource),
            contentDescription = caption,
            modifier = Modifier
                .widthIn(max = ScreenshotMaxWidth)
                .fillMaxWidth()
                .clip(shape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
            contentScale = ContentScale.FillWidth,
        )
        Text(
            text = caption,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val guideListItemColors
    @Composable
    get() = ListItemDefaults.segmentedColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    )

@Composable
private fun GuideLeadingIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(24.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun SettingsListScope.guideTermRow(
    title: String,
    body: String,
    icon: ImageVector,
    term: GuideTerm,
    onTermClick: (GuideTerm) -> Unit,
) = customRow { shapes ->
    GuideListItem(
        shapes = shapes,
        title = title,
        body = body,
        icon = icon,
        onTermClick = onTermClick,
        onClick = { onTermClick(term) },
        trailingContent = {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun SettingsListScope.guideLinkRow(
    title: String,
    body: String,
    icon: ImageVector,
    onTermClick: (GuideTerm) -> Unit,
    onOpen: () -> Unit,
) = customRow { shapes ->
    GuideListItem(
        shapes = shapes,
        title = title,
        body = body,
        icon = icon,
        onTermClick = onTermClick,
        onClick = onOpen,
        trailingContent = {
            Icon(
                imageVector = Icons.Rounded.Link,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun SettingsListScope.guideInstallRow(
    title: String,
    body: String,
    icon: ImageVector,
    state: GuideInstallState,
    onTermClick: (GuideTerm) -> Unit,
    onInstall: () -> Unit,
) = customRow { shapes ->
    GuideListItem(
        shapes = shapes,
        title = title,
        body = body,
        icon = icon,
        onTermClick = onTermClick,
        onClick = onInstall,
        trailingContent = { GuideInstallAction(state = state, onInstall = onInstall) },
    )
}

private fun SettingsListScope.guideStaticRow(
    title: String,
    body: String,
    icon: ImageVector,
) = shapedRow { shape ->
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GuideLeadingIcon(icon)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GuideListItem(
    shapes: ListItemShapes,
    title: String,
    body: String,
    icon: ImageVector,
    onTermClick: (GuideTerm) -> Unit,
    onClick: () -> Unit,
    trailingContent: @Composable () -> Unit,
) {
    SegmentedListItem(
        onClick = onClick,
        shapes = shapes,
        colors = guideListItemColors,
        leadingContent = { GuideLeadingIcon(icon) },
        supportingContent = {
            GuideText(
                text = body,
                onTermClick = onTermClick,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = trailingContent,
    ) {
        Text(title)
    }
}

@Composable
private fun GuideInstallAction(
    state: GuideInstallState,
    onInstall: () -> Unit,
) {
    when (state) {
        GuideInstallState.Installed -> Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(Res.string.addons_guide_installed),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        GuideInstallState.Installing -> CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.dp,
        )
        GuideInstallState.Available -> FilledTonalButton(onClick = onInstall) {
            Icon(
                imageVector = Icons.Rounded.Download,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
            Text(stringResource(Res.string.addons_guide_install))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GuideTermSheet(
    term: GuideTerm,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalSheet(
        onDismissRequest = {
            scope.launch {
                dismissBottomSheet(sheetState = sheetState, onDismiss = onDismiss)
            }
        },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = BottomSheetBodyMargin)
                .padding(top = BottomSheetBodyMargin, bottom = BottomSheetBodyMargin * 2),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(term.titleRes),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                SheetNavigationButton(
                    navigation = SheetNavigation.Close,
                    onClick = {
                        scope.launch {
                            dismissBottomSheet(sheetState = sheetState, onDismiss = onDismiss)
                        }
                    },
                )
            }
            Text(
                text = stringResource(term.bodyRes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
