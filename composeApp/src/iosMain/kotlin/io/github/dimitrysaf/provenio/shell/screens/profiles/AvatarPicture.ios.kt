package io.github.dimitrysaf.provenio.shell.screens.profiles

import androidx.compose.runtime.Composable

@Composable
internal actual fun rememberPicturePicker(onPicked: (ByteArray?) -> Unit): (() -> Unit)? = null

internal actual fun squareAvatarJpeg(bytes: ByteArray, size: Int): ByteArray? = null

internal actual fun fittedPictureJpeg(bytes: ByteArray, maxSide: Int): ByteArray? = null
