package io.github.dimitrysaf.provenio.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ProvidableCompositionLocal
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
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import co.touchlab.kermit.Logger
import io.github.dimitrysaf.provenio.core.build.isMacOs
import io.github.dimitrysaf.provenio.shell.components.LocalPageTextSelection
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy

private val log = Logger.withTag("PageTextSelection")

/**
 * Every word in the window can be selected, as on a web page: dragged across with the mouse,
 * copied with Ctrl+C, and all of it chosen with Ctrl+A or the right-click menu's Select all.
 *
 * Tooltips, menus and dialogs are windows of their own, which a selection cannot reach across:
 * their text is left out of it, rather than the selection failing on it. Compose offers no way to
 * say so, so the selection's own parts are reached for; should they ever change, the window simply
 * has no page-wide selection.
 */
@Composable
internal fun PageTextSelection(content: @Composable () -> Unit) {
    val parts = remember { SelectionParts.load() }
    if (parts == null) {
        content()
        return
    }
    // The selection never takes focus of its own, so moving with the keys is unchanged; its
    // shortcuts still hear the keys pressed in whatever inside it has focus.
    SelectionContainer(modifier = Modifier.focusProperties { canFocus = false }) {
        val registrar = parts.local.current
        val window = remember { WindowRoot() }
        val guarded = remember(registrar) { registrar?.let { runCatching { parts.guard(it, window) }.getOrNull() } }
        if (registrar == null || guarded == null) {
            content()
            return@SelectionContainer
        }
        DisposableEffect(registrar) {
            PageTextSelectAll.target = registrar
            onDispose { if (PageTextSelectAll.target === registrar) PageTextSelectAll.target = null }
        }
        Box(
            modifier = Modifier.onGloballyPositioned { window.root = it.findRootCoordinates() },
            propagateMinConstraints = true,
        ) {
            CompositionLocalProvider(
                parts.local provides guarded,
                LocalPageTextSelection provides true,
                content = content,
            )
        }
    }
}

/** The window's own layout, which text must be part of to join the selection. */
private class WindowRoot {
    var root: LayoutCoordinates? = null

    fun holds(coordinates: LayoutCoordinates): Boolean {
        val window = root ?: return true
        return coordinates.isAttached && coordinates.findRootCoordinates() === window
    }
}

/** Compose's selection registrar, and its text's selectable, as far as they can be reached. */
private class SelectionParts(
    val local: ProvidableCompositionLocal<Any?>,
    private val registrarType: Class<*>,
    private val selectableType: Class<*>,
) {
    /** The registrar as the window's text sees it: text outside the window is not offered to the selection. */
    fun guard(registrar: Any, window: WindowRoot): Any =
        Proxy.newProxyInstance(registrarType.classLoader, arrayOf(registrarType)) { proxy, method, args ->
            val arguments = args.orEmpty()
            when {
                method.isObjectMethod() -> objectMethod(proxy, method, arguments)
                method.name == "subscribe" && arguments.size == 1 && arguments[0] != null ->
                    forward(registrar, method, arrayOf(guardSelectable(arguments[0]!!, window)))
                arguments.any { it is LayoutCoordinates && !window.holds(it) } -> method.returnType.emptyValue()
                else -> forward(registrar, method, arguments)
            }
        }

    private fun guardSelectable(selectable: Any, window: WindowRoot): Any {
        val coordinatesOf = selectableType.getMethod("getLayoutCoordinates")
        val outside = { (forward(selectable, coordinatesOf, emptyArray()) as? LayoutCoordinates)?.let { !window.holds(it) } == true }
        return Proxy.newProxyInstance(selectableType.classLoader, arrayOf(selectableType), InvocationHandler { proxy, method, args ->
            val arguments = args.orEmpty()
            when {
                method.isObjectMethod() -> objectMethod(proxy, method, arguments)
                method.name in OutsideHidden && outside() -> method.returnType.emptyValue()
                else -> forward(selectable, method, arguments)
            }
        })
    }

    companion object {
        // What a selection asks of a text to place it; a text in another window answers nothing.
        private val OutsideHidden = setOf("getLayoutCoordinates", "appendSelectableInfoToBuilder", "getSelectAllSelection")
        private const val Package = "androidx.compose.foundation.text.selection"

        @Suppress("UNCHECKED_CAST")
        fun load(): SelectionParts? = runCatching {
            val holder = Class.forName("$Package.SelectionRegistrarKt")
            val getter = holder.declaredMethods.first { it.name.startsWith("getLocalSelectionRegistrar") && it.parameterCount == 0 }
            getter.isAccessible = true
            SelectionParts(
                local = getter.invoke(null) as ProvidableCompositionLocal<Any?>,
                registrarType = Class.forName("$Package.SelectionRegistrar"),
                selectableType = Class.forName("$Package.Selectable"),
            )
        }.onFailure { log.w(it) { "The text selection's parts are out of reach" } }.getOrNull()
    }
}

private fun Method.isObjectMethod(): Boolean = declaringClass == Any::class.java

private fun objectMethod(proxy: Any, method: Method, arguments: Array<out Any?>): Any? = when (method.name) {
    "equals" -> proxy === arguments.firstOrNull()
    "hashCode" -> System.identityHashCode(proxy)
    else -> "PageTextSelection proxy"
}

private fun forward(target: Any, method: Method, arguments: Array<out Any?>): Any? =
    try {
        method.invoke(target, *arguments)
    } catch (error: InvocationTargetException) {
        throw error.targetException
    }

private fun Class<*>.emptyValue(): Any? = when (this) {
    java.lang.Boolean.TYPE -> false
    java.lang.Long.TYPE -> 0L
    java.lang.Integer.TYPE -> 0
    java.lang.Float.TYPE -> 0f
    else -> null
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
            .onFailure { log.w(it) { "Selecting all the text failed" } }
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
