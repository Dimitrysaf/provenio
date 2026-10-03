package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerIconService
import androidx.compose.ui.platform.LocalPointerIconService
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties

internal enum class PointerCursorKind { Default, Hand, Blocked, Text }

@Composable
internal expect fun rememberPointerCursorTracking(onCursor: (PointerCursorKind) -> Unit): Modifier

internal expect fun blockedPointerIcon(): PointerIcon?

@Composable
fun PointerCursorHost(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val base = LocalPointerIconService.current
    if (base == null) {
        Box(modifier = modifier, content = content)
        return
    }
    val service = remember(base) { FallbackPointerIconService(base) }
    val tracking = rememberPointerCursorTracking { kind -> service.setFallback(kind.pointerIcon()) }
    CompositionLocalProvider(LocalPointerIconService provides service) {
        Box(modifier = modifier.then(tracking), content = content)
    }
}

internal fun List<SemanticsNode>.pointerCursorAt(
    position: Offset,
    bounds: SemanticsNode.() -> Rect,
): PointerCursorKind {
    for (root in asReversed()) {
        root.pointerCursorAt(position, bounds)?.let { return it }
    }
    return PointerCursorKind.Default
}

private fun SemanticsNode.pointerCursorAt(
    position: Offset,
    bounds: SemanticsNode.() -> Rect,
): PointerCursorKind? {
    if (!bounds().contains(position)) return null
    for (child in children.asReversed()) {
        child.pointerCursorAt(position, bounds)?.let { return it }
    }
    return config.pointerCursorKind()
}

private fun SemanticsConfiguration.pointerCursorKind(): PointerCursorKind? {
    val interactive = SemanticsActions.OnClick in this ||
        SemanticsActions.OnLongClick in this ||
        SemanticsActions.SetProgress in this
    return when {
        SemanticsProperties.Disabled in this && (interactive || SemanticsProperties.EditableText in this) ->
            PointerCursorKind.Blocked
        SemanticsProperties.EditableText in this -> PointerCursorKind.Text
        interactive -> PointerCursorKind.Hand
        else -> null
    }
}

private fun PointerCursorKind.pointerIcon(): PointerIcon? = when (this) {
    PointerCursorKind.Default -> null
    PointerCursorKind.Hand -> PointerIcon.Hand
    PointerCursorKind.Text -> PointerIcon.Text
    PointerCursorKind.Blocked -> blockedPointerIcon() ?: PointerIcon.Default
}

private class FallbackPointerIconService(private val base: PointerIconService) : PointerIconService {
    private var requested: PointerIcon? = null
    private var fallback: PointerIcon? = null

    fun setFallback(icon: PointerIcon?) {
        fallback = icon
        apply()
    }

    override fun getIcon(): PointerIcon = requested ?: fallback ?: base.getIcon()

    override fun setIcon(value: PointerIcon?) {
        requested = value
        apply()
    }

    override fun getStylusHoverIcon(): PointerIcon? = base.getStylusHoverIcon()

    override fun setStylusHoverIcon(value: PointerIcon?) {
        base.setStylusHoverIcon(value)
    }

    private fun apply() {
        base.setIcon(requested ?: fallback)
    }
}
