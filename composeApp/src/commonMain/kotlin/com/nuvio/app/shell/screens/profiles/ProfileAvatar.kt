package com.nuvio.app.shell.screens.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

/**
 * A profile's face, wherever it appears.
 *
 * Material has no avatar component, so this is the one the app draws: an image when the profile
 * has one, otherwise Google's generated [LetterTile] — the display name's first letter on a colour
 * hashed from [identifier] — and a person icon when there is neither.
 *
 * It is a circle and it stays a circle. Nothing about a profile changes when it is pressed, and a
 * face that squares off under a finger reads as a button rather than as a person; the press
 * belongs to whatever container owns the tap, which draws Material's own state layer over it.
 */
@Composable
internal fun ProfileAvatar(
    size: Dp,
    imageUrl: String?,
    monogram: String?,
    identifier: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    contentColor: Color? = null,
    iconSize: Dp = size * 0.45f,
    icon: ImageVector = Icons.Outlined.Person,
) {
    val letter = LetterTile.letterFor(monogram)
    val tileColor = containerColor ?: LetterTile.colorFor(identifier)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(tileColor),
        contentAlignment = Alignment.Center,
    ) {
        when {
            imageUrl != null -> AsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                modifier = Modifier.size(size).clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
            letter != null -> Text(
                text = letter,
                // Text size is a fraction of the tile, not a fixed point size, so the letter
                // scales with the avatar the way the drawable's does.
                fontSize = (size.value * LetterTile.LetterToTileRatio).sp,
                lineHeight = (size.value * LetterTile.LetterToTileRatio).sp,
                color = contentColor ?: LetterTile.FontColor,
            )
            else -> Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor ?: LetterTile.FontColor,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}
