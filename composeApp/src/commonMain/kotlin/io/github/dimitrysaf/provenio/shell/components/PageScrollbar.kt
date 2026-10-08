package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A scrollbar over the right edge of a scrolling page, where a mouse is the way to scroll: on
 * desktop it shows the page's position and can be dragged. Touch platforms show nothing, since
 * the gesture itself is the scroll and their lists draw no bar.
 *
 * Place it in a Box over the list, aligned to the end and filling the height.
 */
@Composable
internal expect fun PageScrollbar(state: LazyListState, modifier: Modifier = Modifier)
