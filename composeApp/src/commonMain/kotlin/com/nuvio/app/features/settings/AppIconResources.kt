package com.nuvio.app.features.settings

import com.nuvio.app.shell.theme.AppTheme
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import com.nuvio.app.core.settings.AppIconOption

internal val AppIconOption.wordmarkResource: DrawableResource
    get() = when (this) {
        AppIconOption.ORIGINAL -> Res.drawable.app_logo_wordmark_original
        AppIconOption.ARCTIC_BLUE -> Res.drawable.app_logo_wordmark_arctic_blue
        AppIconOption.EMERALD -> Res.drawable.app_logo_wordmark_emerald
        AppIconOption.ROSE_GOLD -> Res.drawable.app_logo_wordmark_rose_gold
        AppIconOption.COPPER -> Res.drawable.app_logo_wordmark_copper
        AppIconOption.GRAPHITE -> Res.drawable.app_logo_wordmark_graphite
    }

internal fun AppTheme.wordmarkResource(fallback: AppIconOption): DrawableResource =
    when (this) {
        AppTheme.GOLD -> Res.drawable.app_logo_wordmark_gold
        AppTheme.JADE -> AppIconOption.EMERALD.wordmarkResource
        AppTheme.ROSE_GOLD -> AppIconOption.ROSE_GOLD.wordmarkResource
        AppTheme.ARCTIC_BLUE -> AppIconOption.ARCTIC_BLUE.wordmarkResource
        AppTheme.GRAPHITE -> AppIconOption.GRAPHITE.wordmarkResource
        else -> fallback.wordmarkResource
    }
