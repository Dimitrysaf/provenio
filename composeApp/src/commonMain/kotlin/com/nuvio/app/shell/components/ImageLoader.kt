package com.nuvio.app.shell.components

import coil3.ImageLoader

internal expect fun ImageLoader.Builder.configurePlatformImageLoader(): ImageLoader.Builder
