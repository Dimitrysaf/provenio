package io.github.dimitrysaf.provenio.shell.screens.player

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

/**
 * A subtitle line as mpv gives it, in ASS markup, as text to draw. Line breaks and the italic,
 * bold, underline and strike-out switches carry over, since plain-text subtitles use them; every
 * other override, such as a position or a colour, is dropped, as the style sets those.
 */
internal fun assSubtitleText(raw: String): AnnotatedString = buildAnnotatedString {
    var italic = false
    var bold = false
    var underline = false
    var strike = false
    var index = 0
    val pending = StringBuilder()

    fun flush() {
        if (pending.isEmpty()) return
        val decorations = listOfNotNull(
            TextDecoration.Underline.takeIf { underline },
            TextDecoration.LineThrough.takeIf { strike },
        )
        val span = SpanStyle(
            fontStyle = if (italic) FontStyle.Italic else null,
            fontWeight = if (bold) FontWeight.Bold else null,
            textDecoration = decorations.takeIf { it.isNotEmpty() }?.let { TextDecoration.combine(it) },
        )
        withStyle(span) { append(pending.toString()) }
        pending.clear()
    }

    while (index < raw.length) {
        val char = raw[index]
        when {
            char == '{' -> {
                val end = raw.indexOf('}', index + 1)
                if (end < 0) {
                    pending.append(raw.substring(index))
                    index = raw.length
                    continue
                }
                flush()
                raw.substring(index + 1, end).split('\\').forEach { tag ->
                    when {
                        tag.startsWith("i") && tag.length == 2 -> italic = tag[1] != '0'
                        tag.startsWith("u") && tag.length == 2 -> underline = tag[1] != '0'
                        tag.startsWith("s") && tag.length == 2 -> strike = tag[1] != '0'
                        tag.startsWith("b") && tag.drop(1).all(Char::isDigit) && tag.length > 1 ->
                            bold = tag.drop(1) != "0"
                    }
                }
                index = end + 1
            }
            char == '\\' && index + 1 < raw.length && raw[index + 1] in "Nnh" -> {
                pending.append(if (raw[index + 1] == 'h') ' ' else '\n')
                index += 2
            }
            else -> {
                pending.append(char)
                index++
            }
        }
    }
    flush()
}
