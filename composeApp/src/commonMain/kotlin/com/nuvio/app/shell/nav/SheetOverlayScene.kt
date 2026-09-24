package com.nuvio.app.shell.nav

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope

/**
 * A destination that is a sheet rather than a page.
 *
 * The display normally composes one destination at a time, so a destination that draws a sheet
 * would hang it over an empty screen. An overlay scene keeps the destination beneath it composed
 * and renders this one above, which is what makes the page you came from visible behind the
 * sheet's scrim.
 */
internal class SheetOverlayScene(
    override val key: Any,
    private val entry: NavEntry<NavKey>,
    override val previousEntries: List<NavEntry<NavKey>>,
    override val overlaidEntries: List<NavEntry<NavKey>>,
) : OverlayScene<NavKey> {

    override val entries: List<NavEntry<NavKey>> = listOf(entry)

    override val content: @Composable () -> Unit = { entry.Content() }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as SheetOverlayScene

        return key == other.key &&
            entry == other.entry &&
            previousEntries == other.previousEntries &&
            overlaidEntries == other.overlaidEntries
    }

    override fun hashCode(): Int {
        return key.hashCode() * 31 +
            entry.hashCode() * 31 +
            previousEntries.hashCode() * 31 +
            overlaidEntries.hashCode()
    }

    override fun toString(): String = "SheetOverlayScene(key=$key)"
}

/**
 * Marks a destination as a sheet. An entry carries this in its metadata, which is how a scene
 * strategy is meant to recognise its own entries: a [NavEntry]'s key is not its to read.
 */
internal const val SheetRouteMetadataKey: String = "com.nuvio.app.navigation.sheet_route"

/** The metadata an `entry` passes to be drawn as a sheet. */
internal fun sheetRouteMetadata(): Map<String, Any> = mapOf(SheetRouteMetadataKey to true)

/** Renders the entries marked with [sheetRouteMetadata] over whatever they were opened from. */
internal class SheetOverlaySceneStrategy : SceneStrategy<NavKey> {

    override fun SceneStrategyScope<NavKey>.calculateScene(
        entries: List<NavEntry<NavKey>>,
    ): Scene<NavKey>? {
        val last = entries.lastOrNull() ?: return null
        if (last.metadata[SheetRouteMetadataKey] != true) return null
        // A sheet needs something to sit over. Opened as the first destination there is nothing
        // beneath it, so it falls back to being a page of its own.
        if (entries.size < 2) return null
        return SheetOverlayScene(
            key = last.contentKey,
            entry = last,
            previousEntries = entries.dropLast(1),
            overlaidEntries = entries.dropLast(1),
        )
    }
}
