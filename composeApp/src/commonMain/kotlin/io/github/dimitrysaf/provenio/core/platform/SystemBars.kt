package io.github.dimitrysaf.provenio.core.platform

import androidx.compose.runtime.Composable

/**
 * Makes a dialog-hosted surface follow the system-bar visibility of the window behind it.
 *
 * A modal sheet is not drawn in the screen it appears over — it gets a window of its own,
 * and a new window does not inherit the immersive flags set on the activity. So a sheet
 * opened from the full-screen player brings the status and navigation bars back with it,
 * and its own insets then disagree with what is actually on screen: the container reaches
 * further up than the content inside it and the top of the list is clipped.
 *
 * Called from inside such a surface, this matches the host. Where the host has its bars on
 * show, it does nothing, so an ordinary screen's sheets are untouched.
 */
@Composable
expect fun MatchHostSystemBars()
