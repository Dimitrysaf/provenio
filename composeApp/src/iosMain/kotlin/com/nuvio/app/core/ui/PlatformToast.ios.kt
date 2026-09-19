package com.nuvio.app.core.ui

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSThread
import platform.UIKit.NSTextAlignmentCenter
import platform.UIKit.UIColor
import platform.UIKit.UIFont
import platform.UIKit.UILabel
import platform.UIKit.UIView
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UIKit.UIApplication
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * iOS has no toast of its own, so this is the nearest native equivalent: a dark rounded capsule
 * added to the key window, above everything the app draws, that fades itself out.
 *
 * It lives in UIKit rather than in a Compose overlay so it behaves as Android's toast does: it
 * belongs to the window rather than to any one screen, so it survives navigation, no scaffold can
 * clip it, and no screen has to place a host for it.
 */
@OptIn(ExperimentalForeignApi::class)
internal object IosToastPresenter {

    private const val VISIBLE_SECONDS = 2.0
    private const val FADE_SECONDS = 0.25
    private const val SIDE_MARGIN = 24.0
    private const val BOTTOM_MARGIN = 96.0
    private const val TEXT_INSET_X = 16.0
    private const val TEXT_INSET_Y = 10.0

    private var current: UIView? = null
    private var generation = 0

    fun show(message: String) = onMain {
        val window = keyWindow() ?: return@onMain
        // One at a time, as on Android: a new message replaces the one on screen rather than
        // stacking on top of it.
        current?.removeFromSuperview()
        generation += 1
        val presented = generation

        val windowWidth = window.bounds.useContents { size.width }
        val windowHeight = window.bounds.useContents { size.height }
        val maxTextWidth = windowWidth - (SIDE_MARGIN * 2) - (TEXT_INSET_X * 2)

        val label = UILabel().apply {
            text = message
            textColor = UIColor.whiteColor
            font = UIFont.systemFontOfSize(15.0)
            textAlignment = NSTextAlignmentCenter
            // NSInteger, so a Long rather than an Int.
            numberOfLines = 3L
        }
        val textSize = label.sizeThatFits(CGSizeMake(maxTextWidth, 0.0))
        val textWidth = textSize.useContents { width }
        val textHeight = textSize.useContents { height }
        val width = textWidth + (TEXT_INSET_X * 2)
        val height = textHeight + (TEXT_INSET_Y * 2)

        val capsule = UIView().apply {
            setFrame(
                CGRectMake(
                    x = (windowWidth - width) / 2.0,
                    y = windowHeight - BOTTOM_MARGIN - height,
                    width = width,
                    height = height,
                ),
            )
            backgroundColor = UIColor.colorWithWhite(white = 0.13, alpha = 0.95)
            alpha = 0.0
            clipsToBounds = true
            // A notice, not a control: it must never take a touch meant for what it covers.
            setUserInteractionEnabled(false)
        }
        capsule.layer.cornerRadius = height / 2.0

        label.setFrame(
            CGRectMake(
                x = TEXT_INSET_X,
                y = TEXT_INSET_Y,
                width = width - (TEXT_INSET_X * 2),
                height = height - (TEXT_INSET_Y * 2),
            ),
        )
        capsule.addSubview(label)
        window.addSubview(capsule)
        current = capsule

        UIView.animateWithDuration(FADE_SECONDS) { capsule.alpha = 1.0 }
        UIView.animateWithDuration(
            duration = FADE_SECONDS,
            delay = VISIBLE_SECONDS,
            options = 0uL,
            animations = { capsule.alpha = 0.0 },
            completion = {
                // Only tear down if this is still the message on screen; a later one owns the
                // view by now and must not be removed by this one's timer.
                if (generation == presented) {
                    capsule.removeFromSuperview()
                    current = null
                }
            },
        )
    }

    fun dismiss() = onMain {
        current?.removeFromSuperview()
        current = null
    }

    private fun onMain(block: () -> Unit) {
        if (NSThread.isMainThread()) {
            block()
        } else {
            dispatch_async(dispatch_get_main_queue()) { block() }
        }
    }

    private fun keyWindow(): UIWindow? {
        for (scene in UIApplication.sharedApplication.connectedScenes) {
            val windows = (scene as? UIWindowScene)?.windows ?: continue
            val key = windows.firstOrNull { (it as? UIWindow)?.isKeyWindow() == true }
            if (key != null) return key as UIWindow
        }
        return null
    }
}

internal actual fun platformShowToast(message: String) = IosToastPresenter.show(message)

internal actual fun platformDismissToast() = IosToastPresenter.dismiss()
