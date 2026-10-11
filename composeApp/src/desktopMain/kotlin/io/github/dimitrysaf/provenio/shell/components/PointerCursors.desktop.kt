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
import javax.swing.SwingUtilities
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
        var overPopup = false
        // Compose sets the cursor itself while it handles the same event, so a cursor set here
        // directly waits until that has run, or Compose's default would replace it.
        fun setCursorAfterCompose(kind: PointerCursorKind) {
            val target = component ?: return
            SwingUtilities.invokeLater { target.cursor = kind.awtCursor() }
        }
        fun update() {
            val point = position ?: return
            val roots = window.semanticsOwners.map { it.unmergedRootSemanticsNode }
            val popup = roots.drop(1).asReversed().firstOrNull { it.coversPointer(point) { boundsInWindow } }
            if (popup != null) {
                overPopup = true
                setCursorAfterCompose(popup.pointerCursorAt(point) { boundsInWindow } ?: PointerCursorKind.Default)
                return
            }
            val kind = roots.firstOrNull()?.pointerCursorAt(point) { boundsInWindow } ?: PointerCursorKind.Default
            // Leaving a popup, the cursor it set stays until something changes it, and the page's
            // own cursor may already hold this value, so set it once directly.
            if (overPopup) {
                overPopup = false
                setCursorAfterCompose(kind)
            }
            currentOnCursor(kind)
        }
        fun track(event: MouseEvent) {
            component = event.component
            position = Offset(event.x * currentDensity, event.y * currentDensity)
        }
        // A click or a scroll moves what is under a still pointer, and pages take a moment to
        // settle, so the cursor is looked at again a few times after one.
        var settleTicks = 0
        val settle = Timer(CursorSettleIntervalMillis) {
            update()
            if (++settleTicks >= CursorSettleTicks) (it.source as Timer).stop()
        }
        fun resettle() {
            settleTicks = 0
            settle.restart()
        }
        val listener = object : MouseAdapter() {
            override fun mouseMoved(e: MouseEvent) {
                track(e)
                update()
            }

            override fun mouseDragged(e: MouseEvent) {
                track(e)
            }

            override fun mouseReleased(e: MouseEvent) {
                track(e)
                update()
                resettle()
            }

            override fun mouseWheelMoved(e: MouseWheelEvent) {
                track(e)
                resettle()
            }

            override fun mouseEntered(e: MouseEvent) {
                track(e)
                update()
            }

            override fun mouseExited(e: MouseEvent) {
                position = null
                settle.stop()
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

private const val CursorSettleIntervalMillis = 150

private const val CursorSettleTicks = 8
