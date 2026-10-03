package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.tooling.ComposeToolingApi
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.awt.LocalAwtWindow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.platform.LocalDensity
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Component
import java.awt.Cursor
import java.awt.Point
import java.awt.RenderingHints
import java.awt.Toolkit
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import java.awt.image.BufferedImage
import javax.swing.Timer

@OptIn(ComposeToolingApi::class, ExperimentalComposeUiApi::class)
@Composable
internal actual fun rememberPointerCursorTracking(onCursor: (PointerCursorKind) -> Unit): Modifier {
    val window = LocalAwtWindow.current as? ComposeWindow
    val currentOnCursor by rememberUpdatedState(onCursor)
    val currentDensity by rememberUpdatedState(LocalDensity.current.density)
    DisposableEffect(window) {
        if (window == null) return@DisposableEffect onDispose {}
        var position: Offset? = null
        var component: Component? = null
        fun update() {
            val point = position ?: return
            val roots = window.semanticsOwners.map { it.unmergedRootSemanticsNode }
            val popup = roots.drop(1).asReversed().firstOrNull { it.coversPointer(point) { boundsInWindow } }
            if (popup != null) {
                val kind = popup.pointerCursorAt(point) { boundsInWindow } ?: PointerCursorKind.Default
                component?.cursor = kind.awtCursor()
                return
            }
            currentOnCursor(roots.firstOrNull()?.pointerCursorAt(point) { boundsInWindow } ?: PointerCursorKind.Default)
        }
        fun track(event: MouseEvent) {
            component = event.component
            position = Offset(event.x * currentDensity, event.y * currentDensity)
        }
        val settle = Timer(CursorSettleMillis) { update() }.apply { isRepeats = false }
        val listener = object : MouseAdapter() {
            override fun mouseMoved(e: MouseEvent) {
                track(e)
                update()
            }

            override fun mouseReleased(e: MouseEvent) {
                track(e)
                update()
                settle.restart()
            }

            override fun mouseWheelMoved(e: MouseWheelEvent) {
                track(e)
                settle.restart()
            }

            override fun mouseExited(e: MouseEvent) {
                position = null
            }
        }
        window.addMouseListener(listener)
        window.addMouseMotionListener(listener)
        window.addMouseWheelListener(listener)
        onDispose {
            settle.stop()
            window.removeMouseListener(listener)
            window.removeMouseMotionListener(listener)
            window.removeMouseWheelListener(listener)
        }
    }
    return Modifier
}

internal actual fun blockedPointerIcon(): PointerIcon? = BlockedPointerIcon

private val BlockedPointerIcon: PointerIcon? by lazy { BlockedCursor?.let { PointerIcon(it) } }

private fun PointerCursorKind.awtCursor(): Cursor = when (this) {
    PointerCursorKind.Default -> Cursor.getDefaultCursor()
    PointerCursorKind.Hand -> Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
    PointerCursorKind.Text -> Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR)
    PointerCursorKind.Blocked -> BlockedCursor ?: Cursor.getDefaultCursor()
}

private val BlockedCursor: Cursor? by lazy {
    runCatching {
        val toolkit = Toolkit.getDefaultToolkit()
        val best = toolkit.getBestCursorSize(32, 32)
        val size = maxOf(best.width, best.height).takeIf { it > 0 } ?: 32
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val unit = size / 32.0
        val center = size / 2.0
        val radius = 7.5 * unit
        val slash = radius * 0.7071
        val ring = Ellipse2D.Double(center - radius, center - radius, radius * 2, radius * 2)
        val bar = Line2D.Double(center - slash, center - slash, center + slash, center + slash)
        image.createGraphics().apply {
            setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
            color = Color.BLACK
            stroke = BasicStroke((4.5 * unit).toFloat())
            draw(ring)
            draw(bar)
            color = Color.WHITE
            stroke = BasicStroke((2.5 * unit).toFloat())
            draw(ring)
            draw(bar)
            dispose()
        }
        toolkit.createCustomCursor(image, Point(size / 2, size / 2), "blocked")
    }.getOrNull()
}

private const val CursorSettleMillis = 250
