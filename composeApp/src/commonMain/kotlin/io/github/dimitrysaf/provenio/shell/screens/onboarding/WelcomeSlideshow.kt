@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.github.dimitrysaf.provenio.shell.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.using
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.shell.components.M3Motion
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.onboarding_slide_addons
import provenio.composeapp.generated.resources.onboarding_slide_continue
import provenio.composeapp.generated.resources.onboarding_slide_more
import provenio.composeapp.generated.resources.onboarding_slide_sync
import provenio.composeapp.generated.resources.onboarding_slide_watch
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class WelcomeSlide(val caption: StringResource) {
    Watch(Res.string.onboarding_slide_watch),
    Addons(Res.string.onboarding_slide_addons),
    Continue(Res.string.onboarding_slide_continue),
    Sync(Res.string.onboarding_slide_sync),
    More(Res.string.onboarding_slide_more),
}

/**
 * What Provenio is, shown rather than told: scenes of the app drawn in the theme's colours, each
 * with a line under it, moving on by themselves and swiped through by hand. The scenes are drawn,
 * not pictures of real titles, so they suit any theme and need nothing from the network.
 */
@Composable
internal fun WelcomeSlideshow(modifier: Modifier = Modifier) {
    val slides = WelcomeSlide.entries
    // Pages run on far in both directions, so the slideshow loops without a jump back to the start.
    val pagerState = rememberPagerState(initialPage = LoopStart) { LoopPages }
    LaunchedEffect(pagerState) {
        while (true) {
            delay(SlideMillis)
            if (!pagerState.isScrollInProgress) {
                pagerState.animateScrollToPage(
                    page = pagerState.currentPage + 1,
                    animationSpec = tween(SlideTurnMillis, easing = M3Motion.Emphasized),
                )
            }
        }
    }
    val current = slides[pagerState.currentPage.mod(slides.size)]

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 16.dp,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) { page ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                when (slides[page.mod(slides.size)]) {
                    WelcomeSlide.Watch -> PosterWall()
                    WelcomeSlide.Addons -> AddonSwitches()
                    WelcomeSlide.Continue -> PlayerScene()
                    WelcomeSlide.Sync -> DevicesInSync()
                    WelcomeSlide.More -> MoreShapes()
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        AnimatedContent(
            targetState = current,
            transitionSpec = {
                (fadeIn(M3Motion.fadeThroughInSpec()) togetherWith fadeOut(M3Motion.fadeThroughOutSpec()))
                    .using(SizeTransform(clip = false))
            },
            modifier = Modifier.fillMaxWidth(),
            label = "WelcomeCaption",
        ) { slide ->
            Text(
                text = stringResource(slide.caption),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        SlideDots(count = slides.size, current = current.ordinal)
    }
}

@Composable
private fun SlideDots(count: Int, current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { index ->
            val selected = index == current
            val width by animateDpAsState(if (selected) 20.dp else 6.dp)
            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    ),
            )
        }
    }
}

/** Rows of posters drifting past, as a home screen full of movies, series and anime looks. */
@Composable
private fun PosterWall() {
    val transition = rememberInfiniteTransition(label = "PosterWall")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(PosterLoopMillis, easing = LinearEasing)),
        label = "PosterShift",
    )
    BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(vertical = 14.dp)) {
        val gap = 10.dp
        val posterHeight = (maxHeight - gap * 2) / 3
        val posterWidth = posterHeight * 2f / 3f
        val step = posterWidth + gap
        val visible = (maxWidth / step).toInt() + 2
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            repeat(3) { row ->
                // Alternate rows run the other way, at slightly different speeds.
                val leftward = row % 2 == 0
                val travel = step * PosterLoopCount * (1f + row * 0.15f)
                val distance = if (leftward) -(travel * shift) else -(travel * (1f - shift))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    modifier = Modifier
                        .wrapContentWidth(align = Alignment.Start, unbounded = true)
                        .offset { IntOffset(distance.roundToPx(), 0) },
                ) {
                    repeat(visible + (PosterLoopCount * 1.3f).roundToInt() + 2) { index ->
                        val colors = PosterGradients[(row * 3 + index) % PosterGradients.size]
                        PosterCard(colors = colors, width = posterWidth, height = posterHeight)
                    }
                }
            }
        }
    }
}

@Composable
private fun PosterCard(colors: Pair<Color, Color>, width: Dp, height: Dp) {
    Box(
        modifier = Modifier
            .size(width, height)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.verticalGradient(listOf(colors.first, colors.second))),
        contentAlignment = Alignment.BottomStart,
    ) {
        Box(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(0.7f)
                .height(6.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.55f)),
        )
    }
}

/** Addons being turned on, one after another: a source of titles, of streams, of subtitles. */
@Composable
private fun AddonSwitches() {
    val transition = rememberInfiniteTransition(label = "AddonSwitches")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(AddonLoopMillis, easing = LinearEasing)),
        label = "AddonProgress",
    )
    val icons = listOf(Icons.Rounded.Movie, Icons.Rounded.PlayCircle, Icons.Rounded.Subtitles, Icons.Rounded.Extension)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
    ) {
        icons.forEachIndexed { index, icon ->
            val on = progress > 0.12f + index * 0.17f
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(if (index == 0) 20.dp else 8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextBar(fraction = 0.5f + index * 0.08f, height = 8.dp, alpha = 0.7f)
                    TextBar(fraction = 0.8f - index * 0.06f, height = 6.dp, alpha = 0.35f)
                }
                DrawnSwitch(on = on)
            }
        }
    }
}

@Composable
private fun TextBar(fraction: Float, height: Dp, alpha: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth(fraction)
            .height(height)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)),
    )
}

@Composable
private fun DrawnSwitch(on: Boolean) {
    val thumb by animateDpAsState(if (on) 18.dp else 2.dp)
    val track = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest
    Box(
        modifier = Modifier
            .size(width = 40.dp, height = 24.dp)
            .clip(CircleShape)
            .background(track)
            .border(2.dp, if (on) track else MaterialTheme.colorScheme.outline, CircleShape),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .offset(x = thumb)
                .size(16.dp)
                .clip(CircleShape)
                .background(if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline),
        )
    }
}

/** A film playing: the picture slowly drawing in, the progress running on, the controls over it. */
@Composable
private fun PlayerScene() {
    val transition = rememberInfiniteTransition(label = "PlayerScene")
    val progress by transition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(PlayerLoopMillis, easing = LinearEasing)),
        label = "PlayerProgress",
    )
    val zoom by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(PlayerLoopMillis, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "PlayerZoom",
    )
    val sky = MaterialTheme.colorScheme.tertiary
    val ground = MaterialTheme.colorScheme.primary
    val sun = MaterialTheme.colorScheme.tertiaryContainer
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .clip(RoundedCornerShape(20.dp)),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoom
                    scaleY = zoom
                },
        ) {
            drawRect(Brush.verticalGradient(listOf(sky.copy(alpha = 0.9f), Color.Black)))
            drawCircle(color = sun, radius = size.minDimension * 0.16f, center = Offset(size.width * 0.7f, size.height * 0.35f))
            drawCircle(color = ground, radius = size.width * 0.55f, center = Offset(size.width * 0.2f, size.height * 1.25f))
            drawCircle(color = ground.copy(alpha = 0.7f), radius = size.width * 0.5f, center = Offset(size.width * 0.85f, size.height * 1.3f))
        }
        Icon(
            imageVector = Icons.Rounded.PlayCircle,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier
                .align(Alignment.Center)
                .size(56.dp),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.3f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(Icons.Rounded.Subtitles, Icons.Rounded.Audiotrack).forEach { icon ->
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

/** A phone, a tablet and a TV, each showing the same thing, kept in step around one sync. */
@Composable
private fun DevicesInSync() {
    val transition = rememberInfiniteTransition(label = "DevicesInSync")
    val turn by transition.animateFloat(
        initialValue = 0f,
        targetValue = -360f,
        animationSpec = infiniteRepeatable(tween(SyncTurnMillis, easing = LinearEasing)),
        label = "SyncTurn",
    )
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(SyncTurnMillis / 2, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "SyncPulse",
    )
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DeviceFrame(width = 34.dp, height = 62.dp, colors = PosterGradients[0], lit = pulse)
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .graphicsLayer {
                        scaleX = 1f + pulse * 0.15f
                        scaleY = 1f + pulse * 0.15f
                    }
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
            )
            Icon(
                imageVector = Icons.Rounded.Sync,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .size(28.dp)
                    .graphicsLayer { rotationZ = turn },
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            DeviceFrame(width = 96.dp, height = 56.dp, colors = PosterGradients[0], lit = pulse)
            DeviceFrame(width = 64.dp, height = 46.dp, colors = PosterGradients[0], lit = pulse)
        }
    }
}

@Composable
private fun DeviceFrame(width: Dp, height: Dp, colors: Pair<Color, Color>, lit: Float) {
    Box(
        modifier = Modifier
            .size(width, height)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f))
            .padding(3.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Brush.linearGradient(listOf(colors.first, colors.second)))
            .graphicsLayer { alpha = 0.75f + lit * 0.25f },
    )
}

/** Themes, subtitles, downloads and the rest, as shapes floating gently. */
@Composable
private fun MoreShapes() {
    val transition = rememberInfiniteTransition(label = "MoreShapes")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(MoreLoopMillis, easing = LinearEasing)),
        label = "MorePhase",
    )
    val colors = MaterialTheme.colorScheme
    val items = listOf(
        Triple(MaterialShapes.Flower.toShape(), colors.primaryContainer to colors.onPrimaryContainer, Icons.Rounded.Palette),
        Triple(MaterialShapes.Cookie9Sided.toShape(), colors.tertiaryContainer to colors.onTertiaryContainer, Icons.Rounded.Subtitles),
        Triple(MaterialShapes.Sunny.toShape(), colors.secondaryContainer to colors.onSecondaryContainer, Icons.Rounded.Download),
    )
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { index, (shape, palette, icon) ->
            val wave = sin((phase + index / 3f) * 2f * PI).toFloat()
            Box(
                modifier = Modifier
                    .size(if (index == 1) 88.dp else 72.dp)
                    .offset { IntOffset(0, (wave * 10.dp.toPx()).roundToInt()) },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationZ = phase * 360f * if (index % 2 == 0) 1f else -1f }
                        .clip(shape)
                        .background(palette.first),
                )
                Icon(icon, contentDescription = null, tint = palette.second, modifier = Modifier.size(30.dp))
            }
        }
    }
}

private val PosterGradients = listOf(
    Color(0xFF6E56CF) to Color(0xFF1E1B4B),
    Color(0xFFE85D75) to Color(0xFF4A1530),
    Color(0xFF2BB5A3) to Color(0xFF0B3B3C),
    Color(0xFFF2A541) to Color(0xFF5A2E0C),
    Color(0xFF4C8DF6) to Color(0xFF14244F),
    Color(0xFFB57EDC) to Color(0xFF2A1745),
    Color(0xFF3FBF6B) to Color(0xFF103A22),
    Color(0xFFEF6C3E) to Color(0xFF3D160A),
)

private const val LoopPages = 10_000
private const val LoopStart = 5_000
private const val SlideMillis = 4_000L
private const val SlideTurnMillis = 700
private const val PosterLoopCount = 6
private const val PosterLoopMillis = 24_000
private const val AddonLoopMillis = 4_000
private const val PlayerLoopMillis = 8_000
private const val SyncTurnMillis = 3_000
private const val MoreLoopMillis = 6_000
