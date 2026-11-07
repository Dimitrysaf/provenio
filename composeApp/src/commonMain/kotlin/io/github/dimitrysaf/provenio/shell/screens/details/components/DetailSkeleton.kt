package io.github.dimitrysaf.provenio.shell.screens.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.shell.components.SkeletonBlock
import io.github.dimitrysaf.provenio.shell.screens.home.components.HomeSkeletonHero

/**
 * The Details page while its title loads: the hero carousel, the logo, the facts line, the play
 * and menu buttons, the description and a section's list, each as the skeleton's wave.
 */
@Composable
internal fun DetailsSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState(), enabled = false),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HomeSkeletonHero()
        Column(
            modifier = Modifier
                .widthIn(max = DetailSkeletonMaxWidth)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SkeletonBlock(modifier = Modifier.fillMaxWidth(0.55f), height = 48.dp, cornerRadius = 8.dp)
            SkeletonBlock(modifier = Modifier.fillMaxWidth(0.4f), height = 14.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SkeletonBlock(modifier = Modifier.weight(1f), height = 56.dp, cornerRadius = 28.dp)
                SkeletonBlock(width = 56.dp, height = 56.dp, cornerRadius = 28.dp)
            }
            SkeletonBlock(modifier = Modifier.fillMaxWidth(), height = 12.dp)
            SkeletonBlock(modifier = Modifier.fillMaxWidth(), height = 12.dp)
            SkeletonBlock(modifier = Modifier.fillMaxWidth(0.7f), height = 12.dp)
            Spacer(modifier = Modifier.height(12.dp))
            SkeletonBlock(width = 120.dp, height = 20.dp)
            DetailListSkeleton()
        }
    }
}

private val DetailSkeletonMaxWidth = 840.dp
