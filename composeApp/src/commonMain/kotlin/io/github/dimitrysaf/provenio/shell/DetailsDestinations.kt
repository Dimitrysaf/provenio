package io.github.dimitrysaf.provenio.shell

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
import io.github.dimitrysaf.provenio.shell.nav.Navigator
import io.github.dimitrysaf.provenio.shell.nav.PersonDetailRoute
import kotlinx.coroutines.launch
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.person_role_creator
import provenio.composeapp.generated.resources.person_role_director
import provenio.composeapp.generated.resources.person_role_writer
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
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val onBack = rememberGuardedPopBackStack(navController, route)
    val onOpenMeta = rememberOpenMeta(navController)
    val directorRole = stringResource(Res.string.person_role_director)
    val writerRole = stringResource(Res.string.person_role_writer)
    val creatorRole = stringResource(Res.string.person_role_creator)
    MetaDetailsScreen(
        type = route.type,
        id = route.id,
        onBack = onBack,
        onPlay = onPlay,
        onPlayManually = onPlayManually,
        onOpenMeta = onOpenMeta,
        onCastClick = { person, avatarTransitionKey ->
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
        },
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
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
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
        avatarTransitionKey = route.castAvatarTransitionKey,
        preferCrew = route.preferCrew,
        onBack = onBack,
        onOpenMeta = rememberOpenMeta(navController),
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
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
