package io.github.dimitrysaf.provenio.shell.screens.profiles

import androidx.compose.runtime.Composable
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Opens the system's picker for a picture and gives what was picked, or null when nothing was;
 * null itself where pictures cannot be picked.
 */
@Composable
internal expect fun rememberAvatarPicturePicker(onPicked: (ByteArray?) -> Unit): (() -> Unit)?

/** The middle square of a picture, [size] pixels across, as a JPEG; null when it cannot be read. */
internal expect fun squareAvatarJpeg(bytes: ByteArray, size: Int): ByteArray?

/**
 * A picked picture as the profile keeps it: small, and inside the profile itself, so it goes with
 * the profile to every device it is synced to rather than pointing at a file on one of them.
 */
@OptIn(ExperimentalEncodingApi::class)
internal fun avatarPictureDataUri(bytes: ByteArray): String? =
    squareAvatarJpeg(bytes, AvatarPicturePixels)?.let { jpeg -> "$AvatarPictureDataUriPrefix${Base64.encode(jpeg)}" }

internal const val AvatarPictureDataUriPrefix = "data:image/jpeg;base64,"
private const val AvatarPicturePixels = 256
