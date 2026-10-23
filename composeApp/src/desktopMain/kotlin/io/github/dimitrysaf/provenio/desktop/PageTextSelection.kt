package io.github.dimitrysaf.provenio.desktop

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import co.touchlab.kermit.Logger
import io.github.dimitrysaf.provenio.core.build.isMacOs
import io.github.dimitrysaf.provenio.shell.components.LocalPageTextSelection

/**
 * Every word in the window can be selected, as on a web page: dragged across with the mouse,
 * copied with Ctrl+C, and all of it chosen with Ctrl+A or the right-click menu's Select all.
 */
@Composable
internal fun PageTextSelection(content: @Composable () -> Unit) {
    // The selection never takes focus of its own, so moving with the keys is unchanged; its
    // shortcuts still hear the keys pressed in whatever inside it has focus.
    SelectionContainer(modifier = Modifier.focusProperties { canFocus = false }) {
        val registrarLocal = remember { selectionRegistrarLocal() }
        val registrar = registrarLocal?.current
        DisposableEffect(registrar) {
            PageTextSelectAll.target = registrar
            onDispose { if (PageTextSelectAll.target === registrar) PageTextSelectAll.target = null }
        }
        CompositionLocalProvider(LocalPageTextSelection provides true, content = content)
    }
}

/**
 * Ctrl+A, or ⌘A on macOS, for the window's text. Compose's selection keeps its select all for its
 * menu, so it is reached through the selection's own registrar; should that ever change, the key
 * simply does nothing.
 */
internal object PageTextSelectAll {
    var target: Any? = null

    /** Handles the key when nothing before it did, such as a text field selecting its own text. */
    fun handle(event: KeyEvent): Boolean {
        if (event.type != KeyEventType.KeyDown || event.key != Key.A) return false
        val command = if (isMacOs) event.isMetaPressed else event.isCtrlPressed
        if (!command || event.isShiftPressed || event.isAltPressed) return false
        val registrar = target ?: return false
        return runCatching { selectAll(registrar) }
            .onFailure { Logger.withTag("PageTextSelection").w(it) { "Selecting all the text failed" } }
            .getOrDefault(false)
    }

    // The registrar's callbacks are the selection manager's own lambdas, so one of them holds it.
    private fun selectAll(registrar: Any): Boolean {
        val manager = registrar.javaClass.declaredFields.asSequence()
            .mapNotNull { field -> readField(field, registrar) }
            .flatMap { callback ->
                callback.javaClass.declaredFields.asSequence()
                    .filter { it.type.name.endsWith(".SelectionManager") }
                    .mapNotNull { field -> readField(field, callback) }
            }
            .firstOrNull() ?: return false
        val selectAll = manager.javaClass.declaredMethods.firstOrNull {
            it.parameterCount == 0 && (it.name == "selectAll" || it.name.startsWith("selectAll$"))
        } ?: return false
        selectAll.isAccessible = true
        selectAll.invoke(manager)
        return true
    }

    private fun readField(field: java.lang.reflect.Field, owner: Any): Any? =
        runCatching { field.isAccessible = true; field.get(owner) }.getOrNull()
}

@Suppress("UNCHECKED_CAST")
private fun selectionRegistrarLocal(): CompositionLocal<Any?>? = runCatching {
    val holder = Class.forName("androidx.compose.foundation.text.selection.SelectionRegistrarKt")
    val getter = holder.declaredMethods.first { it.name.startsWith("getLocalSelectionRegistrar") && it.parameterCount == 0 }
    getter.isAccessible = true
    getter.invoke(null) as CompositionLocal<Any?>
}.onFailure { Logger.withTag("PageTextSelection").w(it) { "The text selection's registrar is out of reach" } }
    .getOrNull()
