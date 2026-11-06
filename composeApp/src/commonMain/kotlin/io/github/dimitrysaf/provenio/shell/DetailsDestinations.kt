package io.github.dimitrysaf.provenio.shell

import io.github.dimitrysaf.provenio.shell.screens.details.EpisodeDetailScreen
import io.github.dimitrysaf.provenio.core.metadata.MetaTrailer
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import io.github.dimitrysaf.provenio.shell.screens.details.MetaDetailsScreen
import io.github.dimitrysaf.provenio.shell.screens.details.PersonDetailScreen
import io.github.dimitrysaf.provenio.shell.screens.details.TmdbEntityBrowseScreen
import io.github.dimitrysaf.provenio.core.home.MetaPreview
import io.github.dimitrysaf.provenio.core.metadata.tmdb.TmdbEntityKind
import io.github.dimitrysaf.provenio.core.metadata.tmdb.TmdbService
import io.github.dimitrysaf.provenio.shell.nav.DetailRoute
import io.github.dimitrysaf.provenio.shell.nav.EntityBrowseRoute
import io.github.dimitrysaf.provenio.shell.nav.EpisodeDetailRoute
import io.github.dimitrysaf.provenio.shell.nav.SeasonDetailRoute
import io.github.dimitrysaf.provenio.shell.nav.CastListRoute
import io.github.dimitrysaf.provenio.shell.nav.CommentsListRoute
import io.github.dimitrysaf.provenio.shell.screens.details.CastListScreen
import io.github.dimitrysaf.provenio.shell.screens.details.CommentsListScreen
import io.github.dimitrysaf.provenio.core.metadata.MetaKeyword
import io.github.dimitrysaf.provenio.core.metadata.MetaPerson
import io.github.dimitrysaf.provenio.core.streams.SeasonSourceMode
import io.github.dimitrysaf.provenio.shell.screens.details.SeasonDetailScreen
import io.github.dimitrysaf.provenio.shell.nav.Navigator
import io.github.dimitrysaf.provenio.shell.nav.PersonDetailRoute
import kotlinx.coroutines.launch
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.person_role_creator
import provenio.composeapp.generated.resources.person_role_director
import provenio.composeapp.generated.resources.person_role_writer
import provenio.composeapp.generated.resources.settings_meta_cast
import provenio.composeapp.generated.resources.detail_comments_title
import org.jetbrains.compose.resources.stringResource

internal typealias ContentPlayAction = (
    type: String,
    videoId: String,
    parentMetaId: String,
    parentMetaType: String,
    title: String,
    logo: String?,
    poster: String?,
    background: String?,
    seasonNumber: Int?,
    episodeNumber: Int?,
    episodeTitle: String?,
    episodeThumbnail: String?,
    pauseDescription: String?,
    resumePositionMs: Long?,
) -> Unit

@Composable
private fun rememberOpenMeta(navController: Navigator): (MetaPreview) -> Unit {
    val scope = rememberCoroutineScope()
    return { preview ->
        scope.launch {
            val resolvedId = if (preview.id.startsWith("tmdb:")) {
                val tmdbId = preview.id.removePrefix("tmdb:").toIntOrNull()
                tmdbId?.let {
                    TmdbService.tmdbToImdb(
                        tmdbId = it,
                        mediaType = preview.type,
                    )
                } ?: preview.id
            } else {
                preview.id
            }
            navController.navigate(
                DetailRoute(
                    type = preview.type,
                    id = resolvedId,
                    title = preview.name,
                ),
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun DetailsDestination(
    route: DetailRoute,
    navController: Navigator,
    onPlay: ContentPlayAction,
    onPlayManually: ContentPlayAction,
    onPlayWholeSeason: (SeasonSourceMode) -> ContentPlayAction,
    onPlayTrailer: (MetaTrailer, title: String, logo: String?, poster: String?, background: String?) -> Unit,
    onKeywordSearch: (MetaKeyword) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val onBack = rememberGuardedPopBackStack(navController, route)
    val onOpenMeta = rememberOpenMeta(navController)
    val openPerson = rememberOpenPerson(navController)
    val castTitle = stringResource(Res.string.settings_meta_cast)
    val commentsTitle = stringResource(Res.string.detail_comments_title)
    MetaDetailsScreen(
        type = route.type,
        id = route.id,
        onBack = onBack,
        onPlay = onPlay,
        onPlayManually = onPlayManually,
        onPlayWholeSeason = onPlayWholeSeason,
        onPlayTrailer = onPlayTrailer,
        onOpenMeta = onOpenMeta,
        onCastClick = openPerson,
        onCompanyClick = { company, entityKind ->
            val tmdbId = company.tmdbId
            if (tmdbId != null && tmdbId > 0) {
                navController.navigate(
                    EntityBrowseRoute(
                        entityKind = entityKind,
                        entityId = tmdbId,
                        entityName = company.name,
                        sourceType = route.type,
                    ),
                )
            }
        },
        onOpenEpisode = { episode ->
            navController.navigate(
                EpisodeDetailRoute(
                    type = route.type,
                    metaId = route.id,
                    videoId = episode.id,
                    episodeTitle = episode.title,
                ),
            )
        },
        onViewAllCast = { navController.navigate(CastListRoute(type = route.type, metaId = route.id, castTitle = castTitle)) },
        onViewAllComments = {
            navController.navigate(CommentsListRoute(type = route.type, metaId = route.id, commentsTitle = commentsTitle))
        },
        onKeywordClick = onKeywordSearch,
        onOpenSeason = { season, seasonTitle ->
            navController.navigate(
                SeasonDetailRoute(
                    type = route.type,
                    metaId = route.id,
                    season = season,
                    seasonTitle = seasonTitle,
                ),
            )
        },
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
internal fun EpisodeDestination(
    route: EpisodeDetailRoute,
    navController: Navigator,
) {
    val onBack = rememberGuardedPopBackStack(navController, route)
    EpisodeDetailScreen(
        type = route.type,
        metaId = route.metaId,
        videoId = route.videoId,
        episodeTitle = route.episodeTitle,
        onBack = onBack,
        modifier = Modifier.fillMaxSize(),
    )
}

/** Opens a cast or crew member's page, preferring their crew credits when listed as one. */
@Composable
private fun rememberOpenPerson(navController: Navigator): (MetaPerson, String?) -> Unit {
    val directorRole = stringResource(Res.string.person_role_director)
    val writerRole = stringResource(Res.string.person_role_writer)
    val creatorRole = stringResource(Res.string.person_role_creator)
    return { person, avatarTransitionKey ->
        val tmdbId = person.tmdbId
        if (tmdbId != null && tmdbId > 0) {
            navController.navigate(
                PersonDetailRoute(
                    personId = tmdbId,
                    personName = person.name,
                    personPhoto = person.photo,
                    castAvatarTransitionKey = avatarTransitionKey,
                    preferCrew = person.role?.let {
                        it.equals("Director", ignoreCase = true) ||
                            it.equals(directorRole, ignoreCase = true) ||
                            it.equals("Writer", ignoreCase = true) ||
                            it.equals(writerRole, ignoreCase = true) ||
                            it.equals("Creator", ignoreCase = true) ||
                            it.equals(creatorRole, ignoreCase = true)
                    } ?: false,
                ),
            )
        }
    }
}

@Composable
internal fun CastListDestination(
    route: CastListRoute,
    navController: Navigator,
) {
    val onBack = rememberGuardedPopBackStack(navController, route)
    CastListScreen(
        type = route.type,
        metaId = route.metaId,
        onBack = onBack,
        onCastClick = rememberOpenPerson(navController),
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
internal fun CommentsListDestination(
    route: CommentsListRoute,
    navController: Navigator,
) {
    val onBack = rememberGuardedPopBackStack(navController, route)
    CommentsListScreen(
        type = route.type,
        metaId = route.metaId,
        onBack = onBack,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
internal fun SeasonDestination(
    route: SeasonDetailRoute,
    navController: Navigator,
) {
    val onBack = rememberGuardedPopBackStack(navController, route)
    SeasonDetailScreen(
        type = route.type,
        metaId = route.metaId,
        season = route.season,
        seasonTitle = route.seasonTitle,
        onBack = onBack,
        modifier = Modifier.fillMaxSize(),
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun PersonDestination(
    route: PersonDetailRoute,
    navController: Navigator,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val onBack = rememberGuardedPopBackStack(navController, route)
    PersonDetailScreen(
        personId = route.personId,
        personName = route.personName,
        initialProfilePhoto = route.personPhoto,
        preferCrew = route.preferCrew,
        onBack = onBack,
        onOpenMeta = rememberOpenMeta(navController),
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
internal fun EntityDestination(
    route: EntityBrowseRoute,
    navController: Navigator,
) {
    val onBack = rememberGuardedPopBackStack(navController, route)
    TmdbEntityBrowseScreen(
        entityKind = TmdbEntityKind.fromRouteValue(route.entityKind),
        entityId = route.entityId,
        entityName = route.entityName,
        sourceType = route.sourceType,
        onBack = onBack,
        onOpenMeta = rememberOpenMeta(navController),
        modifier = Modifier.fillMaxSize(),
    )
}
