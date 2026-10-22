package io.github.dimitrysaf.provenio.shell.screens.profiles

import androidx.compose.runtime.Composable

@Composable
internal actual fun rememberAvatarPicturePicker(onPicked: (ByteArray?) -> Unit): (() -> Unit)? = null

internal actual fun squareAvatarJpeg(bytes: ByteArray, size: Int): ByteArray? = null
