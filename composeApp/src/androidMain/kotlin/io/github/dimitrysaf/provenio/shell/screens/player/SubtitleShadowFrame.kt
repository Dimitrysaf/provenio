package io.github.dimitrysaf.provenio.shell.screens.player

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.os.Build
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.media3.ui.SubtitleView

/** Where the shadow falls from the subtitles and how it looks, in pixels. */
internal data class SubtitleShadow(
    val dx: Float,
    val dy: Float,
    val blurRadius: Float,
    val color: Int,
)

/**
 * Holds ExoPlayer's subtitle view and draws a shadow of whatever it shows: its text and outline,
 * drawn again in the shadow's colour, moved by the shadow's offset and softened, under the text
 * itself. The shadow so follows every cue wherever it is placed, whichever source set it.
 *
 * Softening needs Android 12's render effects; before that the shadow has a sharp edge.
 */
internal class SubtitleShadowFrame(context: Context) : FrameLayout(context) {
    var shadow: SubtitleShadow? = null
        set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    private val layerPaint = Paint()
    private var node: Any? = null

    override fun dispatchDraw(canvas: Canvas) {
        shadow?.let { drawShadow(canvas, it) }
        super.dispatchDraw(canvas)
    }

    private fun drawShadow(canvas: Canvas, shadow: SubtitleShadow) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && canvas.isHardwareAccelerated) {
            drawSoftShadow(canvas, shadow)
            return
        }
        layerPaint.colorFilter = PorterDuffColorFilter(shadow.color, PorterDuff.Mode.SRC_IN)
        val saved = canvas.saveLayer(null, layerPaint)
        canvas.translate(shadow.dx, shadow.dy)
        super.dispatchDraw(canvas)
        canvas.restoreToCount(saved)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun drawSoftShadow(canvas: Canvas, shadow: SubtitleShadow) {
        val renderNode = (node as? RenderNode) ?: RenderNode("subtitleShadow").also { node = it }
        renderNode.setPosition(0, 0, width, height)
        val recording = renderNode.beginRecording()
        try {
            super.dispatchDraw(recording)
        } finally {
            renderNode.endRecording()
        }
        val tint = RenderEffect.createColorFilterEffect(PorterDuffColorFilter(shadow.color, PorterDuff.Mode.SRC_IN))
        renderNode.setRenderEffect(
            if (shadow.blurRadius > 0f) {
                RenderEffect.createBlurEffect(shadow.blurRadius, shadow.blurRadius, tint, Shader.TileMode.DECAL)
            } else {
                tint
            },
        )
        val saved = canvas.save()
        canvas.translate(shadow.dx, shadow.dy)
        canvas.drawRenderNode(renderNode)
        canvas.restoreToCount(saved)
    }
}

/**
 * Puts [subtitleView] inside a [SubtitleShadowFrame] in the same place, once, and returns that
 * frame. The player keeps its own reference to the view, so moving it changes nothing for it.
 */
internal fun SubtitleView.shadowFrame(): SubtitleShadowFrame? {
    (parent as? SubtitleShadowFrame)?.let { return it }
    val container = parent as? ViewGroup ?: return null
    val index = container.indexOfChild(this)
    val params = layoutParams
    container.removeViewAt(index)
    val frame = SubtitleShadowFrame(context)
    frame.addView(this, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    container.addView(frame, index, params)
    return frame
}
