package io.github.dimitrysaf.provenio.shell.components.markdown

import androidx.compose.runtime.Immutable
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.parser.MarkdownParser

/** A Markdown document read into the blocks the app draws, the way GitHub draws them. */
@Immutable
class MarkdownDocument(val blocks: List<MarkdownBlock>) {
    /** Each heading's anchor, as GitHub makes it (`## Getting started` is `getting-started`), to its block. */
    val anchors: Map<String, Int> = blocks.withIndex()
        .mapNotNull { (index, block) -> (block as? MarkdownBlock.Heading)?.let { it.anchor to index } }
        .toMap()
}

@Immutable
sealed interface MarkdownBlock {
    data class Heading(val level: Int, val content: List<MarkdownInline>, val anchor: String) : MarkdownBlock
    data class Paragraph(val content: List<MarkdownInline>) : MarkdownBlock
    data class Code(val language: String?, val code: String) : MarkdownBlock
    data class Quote(val blocks: List<MarkdownBlock>) : MarkdownBlock
    data class Alert(val kind: MarkdownAlertKind, val blocks: List<MarkdownBlock>) : MarkdownBlock
    data class ItemList(val ordered: Boolean, val start: Int, val items: List<MarkdownListItem>) : MarkdownBlock
    data class Table(
        val alignments: List<MarkdownAlignment>,
        val header: List<List<MarkdownInline>>,
        val rows: List<List<List<MarkdownInline>>>,
    ) : MarkdownBlock

    /** A `<details>` section, folded under its `<summary>` until opened. */
    data class Details(val summary: List<MarkdownInline>, val blocks: List<MarkdownBlock>) : MarkdownBlock

    /** Links that stand alone, a list of nothing but links or a link on a line of its own, drawn as rows. */
    data class Links(val links: List<MarkdownInline.Link>) : MarkdownBlock

    /** Pictures written as HTML, such as `<p align="center"><img src="…"></p>`. */
    data class Images(val images: List<MarkdownInline.Image>, val centered: Boolean) : MarkdownBlock
    data object Rule : MarkdownBlock
}

/** A list item; [checked] is set for a task list's `- [ ]` and `- [x]`. */
@Immutable
data class MarkdownListItem(val checked: Boolean?, val blocks: List<MarkdownBlock>)

enum class MarkdownAlertKind { Note, Tip, Important, Warning, Caution }

enum class MarkdownAlignment { Start, Center, End }

enum class MarkdownTextStyle { Bold, Italic, Strikethrough, Highlight, Underline, Keyboard, Subscript, Superscript }

@Immutable
sealed interface MarkdownInline {
    data class Text(val text: String) : MarkdownInline
    data class Styled(val style: MarkdownTextStyle, val content: List<MarkdownInline>) : MarkdownInline
    data class Code(val code: String) : MarkdownInline
    data class Link(val url: String, val content: List<MarkdownInline>) : MarkdownInline

    /** [width] is the width an HTML `<img width="…">` asks for, in pixels that are drawn as dp. */
    data class Image(val url: String, val alt: String, val width: Int? = null) : MarkdownInline
    data object LineBreak : MarkdownInline
}

/**
 * Reads [markdown] as GitHub does, wiki links (`[[Page]]`, `[[Text|Page]]`) included. Pictures
 * with a relative address are looked up from [baseUrl]; links are left as written, for whoever
 * opens them to resolve.
 */
fun parseMarkdown(markdown: String, baseUrl: String? = null): MarkdownDocument {
    val source = withWikiLinks(markdown.replace("\r\n", "\n"))
    val flavour = GFMFlavourDescriptor(
        useSafeLinks = true,
        absolutizeAnchorLinks = false,
        makeHttpsAutoLinks = true,
        useTagFilter = false,
    )
    val tree = MarkdownParser(flavour).buildMarkdownTreeFromString(source)
    return MarkdownReader(source, baseUrl, linkDefinitions(tree, source)).document(tree)
}

/** The text of [content] without its styling, as a heading's anchor and a picture's description are made of. */
fun plainText(content: List<MarkdownInline>): String = buildString {
    content.forEach { inline ->
        when (inline) {
            is MarkdownInline.Text -> append(inline.text)
            is MarkdownInline.Code -> append(inline.code)
            is MarkdownInline.Styled -> append(plainText(inline.content))
            is MarkdownInline.Link -> append(plainText(inline.content))
            is MarkdownInline.Image -> append(inline.alt)
            MarkdownInline.LineBreak -> append(' ')
        }
    }
}

private class LinkDefinition(val url: String)

private class MarkdownReader(
    private val source: String,
    private val baseUrl: String?,
    private val definitions: Map<String, LinkDefinition>,
) {
    private val usedAnchors = mutableMapOf<String, Int>()

    fun document(root: ASTNode): MarkdownDocument = MarkdownDocument(blocks(root.children))

    // A list item's own content is left as written; only the list as a whole can become rows of links.
    private fun blocks(nodes: List<ASTNode>, groupLinks: Boolean = true): List<MarkdownBlock> {
        val result = mutableListOf<MarkdownBlock>()
        var index = 0
        while (index < nodes.size) {
            val node = nodes[index]
            index++
            when (node.type) {
                MarkdownElementTypes.ATX_1, MarkdownElementTypes.ATX_2, MarkdownElementTypes.ATX_3,
                MarkdownElementTypes.ATX_4, MarkdownElementTypes.ATX_5, MarkdownElementTypes.ATX_6,
                -> {
                    val content = node.children.firstOrNull { it.type == MarkdownTokenTypes.ATX_CONTENT }
                    result += heading(atxLevel(node), inlines(content?.children.orEmpty()))
                }
                MarkdownElementTypes.SETEXT_1, MarkdownElementTypes.SETEXT_2 -> {
                    val content = node.children.firstOrNull { it.type == MarkdownTokenTypes.SETEXT_CONTENT }
                    val level = if (node.type == MarkdownElementTypes.SETEXT_1) 1 else 2
                    result += heading(level, inlines(content?.children.orEmpty()))
                }
                MarkdownElementTypes.PARAGRAPH -> inlines(node.children).takeIf { it.isNotEmpty() }?.let {
                    result += MarkdownBlock.Paragraph(it)
                }
                MarkdownElementTypes.CODE_FENCE -> result += fencedCode(node)
                MarkdownElementTypes.CODE_BLOCK -> result += indentedCode(node)
                GFMElementTypes.BLOCK_MATH -> result += MarkdownBlock.Code("math", text(node).trim().trim('$').trim())
                MarkdownElementTypes.BLOCK_QUOTE -> result += MarkdownBlock.Quote(blocks(node.children))
                GFMElementTypes.ALERT -> result += MarkdownBlock.Alert(alertKind(node), blocks(node.children))
                MarkdownElementTypes.UNORDERED_LIST, MarkdownElementTypes.ORDERED_LIST -> result += list(node)
                GFMElementTypes.TABLE -> table(node)?.let { result += it }
                MarkdownTokenTypes.HORIZONTAL_RULE -> result += MarkdownBlock.Rule
                MarkdownElementTypes.HTML_BLOCK -> {
                    val html = text(node)
                    if (OpenDetails.containsMatchIn(html) && !CloseDetails.containsMatchIn(html)) {
                        // The section's Markdown sits between this block and the one that closes it.
                        val inner = mutableListOf<ASTNode>()
                        var depth = 1
                        var closing: String? = null
                        while (index < nodes.size) {
                            val next = nodes[index]
                            index++
                            if (next.type == MarkdownElementTypes.HTML_BLOCK) {
                                val nextHtml = text(next)
                                depth += OpenDetails.findAll(nextHtml).count() - CloseDetails.findAll(nextHtml).count()
                                if (depth <= 0) {
                                    closing = nextHtml
                                    break
                                }
                            }
                            inner += next
                        }
                        result += MarkdownBlock.Details(summary(html), blocks(inner))
                        closing?.let { result += htmlBlocks(it.substringAfterLast("</details>", "")) }
                    } else if (OpenDetails.containsMatchIn(html)) {
                        val body = html.substringAfter("</summary>", html.substringAfter('>'))
                            .substringBeforeLast("</details>")
                        result += MarkdownBlock.Details(summary(html), htmlBlocks(body))
                        result += htmlBlocks(html.substringAfterLast("</details>", ""))
                    } else {
                        result += htmlBlocks(html)
                    }
                }
                else -> Unit
            }
        }
        return if (groupLinks) linkRows(result) else result
    }

    private fun heading(level: Int, content: List<MarkdownInline>): MarkdownBlock.Heading {
        val slug = anchorOf(plainText(content))
        val seen = usedAnchors[slug]
        usedAnchors[slug] = (seen ?: -1) + 1
        return MarkdownBlock.Heading(level, content, if (seen == null) slug else "$slug-${seen + 1}")
    }

    private fun atxLevel(node: ASTNode): Int = when (node.type) {
        MarkdownElementTypes.ATX_1 -> 1
        MarkdownElementTypes.ATX_2 -> 2
        MarkdownElementTypes.ATX_3 -> 3
        MarkdownElementTypes.ATX_4 -> 4
        MarkdownElementTypes.ATX_5 -> 5
        else -> 6
    }

    private fun alertKind(node: ASTNode): MarkdownAlertKind {
        val title = node.children.firstOrNull { it.type == GFMTokenTypes.ALERT_TITLE }?.let(::text).orEmpty()
        val name = title.trim().removePrefix("[!").removeSuffix("]")
        return MarkdownAlertKind.entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: MarkdownAlertKind.Note
    }

    private fun fencedCode(node: ASTNode): MarkdownBlock.Code {
        val language = node.children.firstOrNull { it.type == MarkdownTokenTypes.FENCE_LANG }
            ?.let(::text)?.trim()?.substringBefore(' ')?.takeIf(String::isNotEmpty)
        val lines = mutableListOf<String>()
        var line = StringBuilder()
        var started = false
        for (child in node.children) {
            when (child.type) {
                MarkdownTokenTypes.CODE_FENCE_END -> break
                MarkdownTokenTypes.EOL -> if (started) {
                    lines += line.toString()
                    line = StringBuilder()
                } else {
                    started = true
                }
                MarkdownTokenTypes.CODE_FENCE_CONTENT -> if (started) line.append(text(child))
                else -> Unit
            }
        }
        if (line.isNotEmpty()) lines += line.toString()
        return MarkdownBlock.Code(language, lines.joinToString("\n").trimEnd('\n'))
    }

    private fun indentedCode(node: ASTNode): MarkdownBlock.Code {
        val code = node.children.joinToString("") { child ->
            when (child.type) {
                MarkdownTokenTypes.CODE_LINE -> text(child).removeIndent(4)
                MarkdownTokenTypes.EOL -> "\n"
                else -> ""
            }
        }
        return MarkdownBlock.Code(null, code.trimEnd('\n'))
    }

    private fun list(node: ASTNode): MarkdownBlock.ItemList {
        val ordered = node.type == MarkdownElementTypes.ORDERED_LIST
        val items = node.children.filter { it.type == MarkdownElementTypes.LIST_ITEM }
        val start = items.firstOrNull()
            ?.children?.firstOrNull { it.type == MarkdownTokenTypes.LIST_NUMBER }
            ?.let { text(it).trim().trimEnd('.', ')').toIntOrNull() }
            ?: 1
        return MarkdownBlock.ItemList(
            ordered = ordered,
            start = start,
            items = items.map { item ->
                val checkBox = item.children.firstOrNull { it.type == GFMTokenTypes.CHECK_BOX }
                MarkdownListItem(
                    checked = checkBox?.let { text(it).contains('x', ignoreCase = true) },
                    blocks = blocks(item.children, groupLinks = false),
                )
            },
        )
    }

    private fun table(node: ASTNode): MarkdownBlock.Table? {
        val header = node.children.firstOrNull { it.type == GFMElementTypes.HEADER } ?: return null
        val separator = node.children.firstOrNull { it.type == GFMTokenTypes.TABLE_SEPARATOR }
        val alignments = separator?.let { alignmentsOf(text(it)) }.orEmpty()
        val headerCells = cells(header)
        val columns = maxOf(headerCells.size, alignments.size)
        val rows = node.children.filter { it.type == GFMElementTypes.ROW }.map { row ->
            val cells = cells(row)
            cells.take(columns) + List((columns - cells.size).coerceAtLeast(0)) { emptyList() }
        }
        return MarkdownBlock.Table(
            alignments = List(columns) { alignments.getOrElse(it) { MarkdownAlignment.Start } },
            header = headerCells + List((columns - headerCells.size).coerceAtLeast(0)) { emptyList() },
            rows = rows,
        )
    }

    private fun cells(row: ASTNode): List<List<MarkdownInline>> =
        row.children.filter { it.type == GFMTokenTypes.CELL }.map { inlines(it.children) }

    // HTML in a document is read for what GitHub shows of it: pictures, headings and text.
    private fun htmlBlocks(html: String): List<MarkdownBlock> {
        val visible = html.replace(HtmlComment, "")
        if (visible.isBlank()) return emptyList()
        val result = mutableListOf<MarkdownBlock>()
        HtmlHeading.findAll(visible).forEach { match ->
            val level = match.groupValues[1].toInt()
            result += heading(level, listOf(MarkdownInline.Text(decodeEntities(stripTags(match.groupValues[2])).trim())))
        }
        val withoutHeadings = visible.replace(HtmlHeading, "")
        val images = HtmlImage.findAll(withoutHeadings).mapNotNull { imageFromTag(it.value) }.toList()
        if (images.isNotEmpty()) {
            result += MarkdownBlock.Images(images, centered = CenterAligned.containsMatchIn(withoutHeadings))
        }
        val rest = decodeEntities(stripTags(withoutHeadings.replace(HtmlBreak, "\n")))
            .lines().joinToString("\n") { it.trim() }.trim()
        if (rest.isNotEmpty()) result += MarkdownBlock.Paragraph(listOf(MarkdownInline.Text(rest)))
        return result
    }

    private fun summary(html: String): List<MarkdownInline> {
        val summary = SummaryTag.find(html)?.groupValues?.get(1)?.let { decodeEntities(stripTags(it)).trim() }
        return listOf(MarkdownInline.Text(summary?.takeIf(String::isNotEmpty) ?: "Details"))
    }

    private fun inlines(nodes: List<ASTNode>): List<MarkdownInline> {
        val builder = InlineBuilder()
        nodes.forEach { appendInline(it, builder) }
        return normalize(builder.finish())
    }

    private fun appendInline(node: ASTNode, builder: InlineBuilder) {
        when (node.type) {
            MarkdownTokenTypes.TEXT -> builder.add(MarkdownInline.Text(decodeEntities(unescape(text(node)))))
            MarkdownTokenTypes.WHITE_SPACE, MarkdownTokenTypes.EOL -> builder.add(MarkdownInline.Text(" "))
            MarkdownTokenTypes.HARD_LINE_BREAK -> builder.add(MarkdownInline.LineBreak)
            MarkdownTokenTypes.BLOCK_QUOTE, GFMTokenTypes.CHECK_BOX, GFMTokenTypes.ALERT_TITLE -> Unit
            MarkdownTokenTypes.ESCAPED_BACKTICKS -> builder.add(MarkdownInline.Text(text(node).replace("\\", "")))
            MarkdownElementTypes.EMPH -> builder.add(styled(MarkdownTextStyle.Italic, node.children.trimEnds(1)))
            MarkdownElementTypes.STRONG -> builder.add(styled(MarkdownTextStyle.Bold, node.children.trimEnds(2)))
            GFMElementTypes.STRIKETHROUGH ->
                builder.add(styled(MarkdownTextStyle.Strikethrough, node.children.trimDelimiters(GFMTokenTypes.TILDE)))
            GFMElementTypes.HIGHLIGHT ->
                builder.add(styled(MarkdownTextStyle.Highlight, node.children.trimDelimiters(GFMTokenTypes.EQUALS)))
            MarkdownElementTypes.CODE_SPAN -> builder.add(MarkdownInline.Code(codeSpan(node)))
            GFMElementTypes.INLINE_MATH -> builder.add(MarkdownInline.Code(text(node).trim('$')))
            MarkdownElementTypes.INLINE_LINK -> {
                val destination = node.children.firstOrNull { it.type == MarkdownElementTypes.LINK_DESTINATION }
                builder.add(MarkdownInline.Link(destination?.let { linkUrl(text(it)) }.orEmpty(), linkText(node)))
            }
            MarkdownElementTypes.FULL_REFERENCE_LINK, MarkdownElementTypes.SHORT_REFERENCE_LINK -> {
                val label = node.children.firstOrNull { it.type == MarkdownElementTypes.LINK_LABEL }
                val definition = label?.let { definitions[labelKey(text(it))] }
                val content = if (node.type == MarkdownElementTypes.FULL_REFERENCE_LINK) linkText(node) else {
                    inlines(label?.children.orEmpty().trimEnds(1))
                }
                if (definition != null) {
                    builder.add(MarkdownInline.Link(definition.url, content))
                } else {
                    builder.add(MarkdownInline.Text(unescape(text(node))))
                }
            }
            MarkdownElementTypes.AUTOLINK -> {
                val email = node.children.firstOrNull { it.type == MarkdownTokenTypes.EMAIL_AUTOLINK }
                val url = node.children.firstOrNull { it.type == MarkdownTokenTypes.AUTOLINK }?.let(::text)
                when {
                    email != null -> builder.add(MarkdownInline.Link("mailto:${text(email)}", listOf(MarkdownInline.Text(text(email)))))
                    url != null -> builder.add(MarkdownInline.Link(url, listOf(MarkdownInline.Text(url))))
                    else -> builder.add(MarkdownInline.Text(text(node)))
                }
            }
            GFMTokenTypes.GFM_AUTOLINK -> {
                val url = text(node)
                val absolute = if (url.startsWith("www.", ignoreCase = true)) "https://$url" else url
                builder.add(MarkdownInline.Link(absolute, listOf(MarkdownInline.Text(url))))
            }
            MarkdownElementTypes.IMAGE -> {
                val link = node.children.firstOrNull { it.type != MarkdownTokenTypes.EXCLAMATION_MARK }
                val image = link?.let(::imageOf)
                if (image != null) builder.add(image) else builder.add(MarkdownInline.Text(text(node)))
            }
            MarkdownTokenTypes.HTML_TAG -> htmlTag(text(node), builder)
            else -> if (node.children.isEmpty()) {
                builder.add(MarkdownInline.Text(decodeEntities(unescape(text(node)))))
            } else {
                node.children.forEach { appendInline(it, builder) }
            }
        }
    }

    private fun styled(style: MarkdownTextStyle, children: List<ASTNode>): MarkdownInline =
        MarkdownInline.Styled(style, inlines(children))

    private fun linkText(node: ASTNode): List<MarkdownInline> =
        inlines(node.children.firstOrNull { it.type == MarkdownElementTypes.LINK_TEXT }?.children.orEmpty().trimEnds(1))

    private fun imageOf(link: ASTNode): MarkdownInline.Image? {
        val url = when (link.type) {
            MarkdownElementTypes.INLINE_LINK ->
                link.children.firstOrNull { it.type == MarkdownElementTypes.LINK_DESTINATION }?.let { linkUrl(text(it)) }
            else -> link.children.firstOrNull { it.type == MarkdownElementTypes.LINK_LABEL }
                ?.let { definitions[labelKey(text(it))]?.url }
        } ?: return null
        val alt = when (link.type) {
            MarkdownElementTypes.SHORT_REFERENCE_LINK -> inlines(
                link.children.firstOrNull { it.type == MarkdownElementTypes.LINK_LABEL }?.children.orEmpty().trimEnds(1),
            )
            else -> linkText(link)
        }
        return MarkdownInline.Image(resolve(url), plainText(alt))
    }

    private fun codeSpan(node: ASTNode): String {
        val first = node.children.firstOrNull() ?: return ""
        val last = node.children.lastOrNull()?.takeIf { it !== first } ?: return ""
        val code = source.substring(first.endOffset, last.startOffset).replace('\n', ' ')
        val trimmed = if (code.length >= 2 && code.startsWith(' ') && code.endsWith(' ') && code.isNotBlank()) {
            code.substring(1, code.length - 1)
        } else {
            code
        }
        // A pipe inside a table cell's code is written escaped, so the table does not split on it.
        return trimmed.replace("\\|", "|")
    }

    private fun htmlTag(tag: String, builder: InlineBuilder) {
        val match = HtmlTag.matchEntire(tag.trim()) ?: return
        val closing = match.groupValues[1] == "/"
        val name = match.groupValues[2].lowercase()
        val attributes = match.groupValues[3]
        when (name) {
            "br" -> builder.add(MarkdownInline.LineBreak)
            "img" -> imageFromTag(tag)?.let(builder::add)
            "a" -> if (closing) builder.close("a") else builder.open("a") { content ->
                val href = attributeOf(attributes, "href")
                if (href == null) MarkdownInline.Styled(MarkdownTextStyle.Underline, content) else MarkdownInline.Link(href, content)
            }
            else -> {
                val style = HtmlStyles[name] ?: return
                if (closing) builder.close(name) else builder.open(name) { content -> MarkdownInline.Styled(style, content) }
            }
        }
    }

    private fun imageFromTag(tag: String): MarkdownInline.Image? {
        val src = attributeOf(tag, "src") ?: return null
        return MarkdownInline.Image(
            url = resolve(decodeEntities(src)),
            alt = attributeOf(tag, "alt")?.let(::decodeEntities).orEmpty(),
            width = attributeOf(tag, "width")?.removeSuffix("px")?.toIntOrNull(),
        )
    }

    private fun resolve(url: String): String {
        val base = baseUrl ?: return url
        if (url.isEmpty() || HasScheme.containsMatchIn(url) || url.startsWith("//") || url.startsWith("#")) return url
        return if (url.startsWith("/")) {
            Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://[^/]+").find(base)?.value.orEmpty() + url
        } else {
            base.substringBeforeLast('/') + "/" + url.removePrefix("./")
        }
    }

    private fun text(node: ASTNode): String = source.substring(node.startOffset, node.endOffset)
}

// Lists made only of links, and runs of paragraphs that are each a single link, become rows of links.
private fun linkRows(blocks: List<MarkdownBlock>): List<MarkdownBlock> {
    val result = mutableListOf<MarkdownBlock>()
    blocks.forEach { block ->
        val listLinks = (block as? MarkdownBlock.ItemList)?.items?.map { item ->
            item.takeIf { it.checked == null }?.blocks?.singleOrNull()?.let { (it as? MarkdownBlock.Paragraph)?.soleLink() }
        }
        val paragraphLink = (block as? MarkdownBlock.Paragraph)?.soleLink()
        val previous = result.lastOrNull() as? MarkdownBlock.Links
        when {
            listLinks != null && listLinks.isNotEmpty() && listLinks.all { it != null } ->
                result += MarkdownBlock.Links(listLinks.filterNotNull())
            paragraphLink != null && previous != null -> result[result.lastIndex] = MarkdownBlock.Links(previous.links + paragraphLink)
            paragraphLink != null -> result += MarkdownBlock.Links(listOf(paragraphLink))
            else -> result += block
        }
    }
    return result
}

// The link a paragraph consists of, unless it is made of pictures, as a badge is.
private fun MarkdownBlock.Paragraph.soleLink(): MarkdownInline.Link? {
    val link = content.filterNot { it is MarkdownInline.Text && it.text.isBlank() }.singleOrNull() as? MarkdownInline.Link
        ?: return null
    return link.takeIf { inner -> inner.content.any { it !is MarkdownInline.Image && !(it is MarkdownInline.Text && it.text.isBlank()) } }
}

/** Collects inlines, nesting what comes between an HTML tag and its closing tag inside it. */
private class InlineBuilder {
    private class Frame(val tag: String, val wrap: (List<MarkdownInline>) -> MarkdownInline) {
        val content = mutableListOf<MarkdownInline>()
    }

    private val root = mutableListOf<MarkdownInline>()
    private val frames = ArrayDeque<Frame>()

    fun add(inline: MarkdownInline) {
        (frames.lastOrNull()?.content ?: root) += inline
    }

    fun open(tag: String, wrap: (List<MarkdownInline>) -> MarkdownInline) {
        frames.addLast(Frame(tag, wrap))
    }

    fun close(tag: String) {
        if (frames.none { it.tag == tag }) return
        while (frames.isNotEmpty()) {
            val frame = frames.removeLast()
            add(frame.wrap(frame.content))
            if (frame.tag == tag) return
        }
    }

    fun finish(): List<MarkdownInline> {
        while (frames.isNotEmpty()) {
            val frame = frames.removeLast()
            add(frame.wrap(frame.content))
        }
        return root
    }
}

// Joins neighbouring text, folds runs of spaces as a browser does, and trims the ends.
private fun normalize(content: List<MarkdownInline>): List<MarkdownInline> {
    val merged = mutableListOf<MarkdownInline>()
    content.forEach { inline ->
        val previous = merged.lastOrNull()
        if (inline is MarkdownInline.Text && previous is MarkdownInline.Text) {
            merged[merged.lastIndex] = MarkdownInline.Text(previous.text + inline.text)
        } else {
            merged += inline
        }
    }
    val folded = merged.map { if (it is MarkdownInline.Text) MarkdownInline.Text(it.text.replace(Spaces, " ")) else it }
        .toMutableList()
    (folded.firstOrNull() as? MarkdownInline.Text)?.let { folded[0] = MarkdownInline.Text(it.text.trimStart()) }
    (folded.lastOrNull() as? MarkdownInline.Text)?.let { folded[folded.lastIndex] = MarkdownInline.Text(it.text.trimEnd()) }
    for (index in folded.indices) {
        if (folded[index] == MarkdownInline.LineBreak) {
            (folded.getOrNull(index + 1) as? MarkdownInline.Text)?.let { folded[index + 1] = MarkdownInline.Text(it.text.trimStart()) }
            (folded.getOrNull(index - 1) as? MarkdownInline.Text)?.let { folded[index - 1] = MarkdownInline.Text(it.text.trimEnd()) }
        }
    }
    return folded.filterNot { it is MarkdownInline.Text && it.text.isEmpty() }
}

private fun linkDefinitions(root: ASTNode, source: String): Map<String, LinkDefinition> {
    val definitions = mutableMapOf<String, LinkDefinition>()
    fun visit(node: ASTNode) {
        if (node.type == MarkdownElementTypes.LINK_DEFINITION) {
            val label = node.children.firstOrNull { it.type == MarkdownElementTypes.LINK_LABEL }
            val destination = node.children.firstOrNull { it.type == MarkdownElementTypes.LINK_DESTINATION }
            if (label != null && destination != null) {
                val key = labelKey(source.substring(label.startOffset, label.endOffset))
                // The first definition of a label wins, as in CommonMark.
                definitions.getOrPut(key) {
                    LinkDefinition(linkUrl(source.substring(destination.startOffset, destination.endOffset)))
                }
            }
        }
        node.children.forEach(::visit)
    }
    visit(root)
    return definitions
}

private fun labelKey(label: String): String = label.trim().removePrefix("[").removeSuffix("]").trim()
    .replace(Spaces, " ").lowercase()

private fun linkUrl(destination: String): String =
    unescape(destination.trim().removePrefix("<").removeSuffix(">")).replace(" ", "%20")

/** A heading's anchor as GitHub makes it: lower case, punctuation dropped, spaces as dashes. */
fun anchorOf(heading: String): String = heading.trim().lowercase()
    .filter { it.isLetterOrDigit() || it == ' ' || it == '-' || it == '_' }
    .replace(' ', '-')

// GitHub wiki links, `[[Page Name]]` and `[[Link text|Page Name]]`, as ordinary links to the page.
// Code is left alone, so a wiki link written as an example stays as written.
private fun withWikiLinks(markdown: String): String {
    if (!markdown.contains("[[")) return markdown
    var fence: String? = null
    return markdown.lines().joinToString("\n") { line ->
        val marker = FenceMarker.find(line.trimStart())?.value
        val open = fence
        when {
            open != null -> {
                if (marker != null && marker.first() == open.first() && marker.length >= open.length) fence = null
                line
            }
            marker != null -> {
                fence = marker
                line
            }
            else -> line.replace(WikiLink) { match ->
                val text = match.groupValues[1].trim()
                val page = match.groupValues[2].trim().ifEmpty { text }
                "[$text](${page.replace(' ', '-')})"
            }
        }
    }
}

private fun String.removeIndent(columns: Int): String {
    var removed = 0
    while (removed < columns && removed < length && this[removed] == ' ') removed++
    return substring(removed)
}

private fun List<ASTNode>.trimEnds(count: Int): List<ASTNode> =
    if (size <= count * 2) emptyList() else subList(count, size - count)

private fun List<ASTNode>.trimDelimiters(type: org.intellij.markdown.IElementType): List<ASTNode> {
    val leading = takeWhile { it.type == type }.size
    return trimEnds(leading.coerceAtLeast(1))
}

private fun alignmentsOf(separator: String): List<MarkdownAlignment> {
    val cells = separator.trim().removePrefix("|").removeSuffix("|").split('|')
    return cells.map { cell ->
        val trimmed = cell.trim()
        when {
            trimmed.startsWith(':') && trimmed.endsWith(':') -> MarkdownAlignment.Center
            trimmed.endsWith(':') -> MarkdownAlignment.End
            else -> MarkdownAlignment.Start
        }
    }
}

private fun attributeOf(tag: String, name: String): String? =
    Regex("""\b$name\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s>]+))""", RegexOption.IGNORE_CASE).find(tag)
        ?.groupValues?.drop(1)?.firstOrNull { it.isNotEmpty() }

private fun stripTags(html: String): String = html.replace(AnyTag, "")

private fun unescape(text: String): String = text.replace(Escaped) { it.groupValues[1] }

private fun decodeEntities(text: String): String {
    if (!text.contains('&')) return text
    return text.replace(Entity) { match ->
        val entity = match.groupValues[1]
        when {
            entity.startsWith("#x", ignoreCase = true) -> entity.drop(2).toIntOrNull(16)?.let(::codePointString)
            entity.startsWith("#") -> entity.drop(1).toIntOrNull()?.let(::codePointString)
            else -> NamedEntities[entity]
        } ?: match.value
    }
}

private fun codePointString(codePoint: Int): String? = when {
    codePoint in 0..0xFFFF -> codePoint.toChar().toString()
    codePoint in 0x10000..0x10FFFF -> {
        val offset = codePoint - 0x10000
        charArrayOf((0xD800 + (offset shr 10)).toChar(), (0xDC00 + (offset and 0x3FF)).toChar()).concatToString()
    }
    else -> null
}

private val NamedEntities = mapOf(
    "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'", "nbsp" to "\u00A0",
    "copy" to "©", "reg" to "®", "trade" to "™", "hellip" to "…", "mdash" to "—", "ndash" to "–",
    "larr" to "←", "rarr" to "→", "uarr" to "↑", "darr" to "↓", "times" to "×", "middot" to "·",
    "bull" to "•", "laquo" to "«", "raquo" to "»", "deg" to "°",
)

private val HtmlStyles = mapOf(
    "b" to MarkdownTextStyle.Bold,
    "strong" to MarkdownTextStyle.Bold,
    "i" to MarkdownTextStyle.Italic,
    "em" to MarkdownTextStyle.Italic,
    "s" to MarkdownTextStyle.Strikethrough,
    "del" to MarkdownTextStyle.Strikethrough,
    "strike" to MarkdownTextStyle.Strikethrough,
    "mark" to MarkdownTextStyle.Highlight,
    "u" to MarkdownTextStyle.Underline,
    "ins" to MarkdownTextStyle.Underline,
    "kbd" to MarkdownTextStyle.Keyboard,
    "code" to MarkdownTextStyle.Keyboard,
    "sub" to MarkdownTextStyle.Subscript,
    "sup" to MarkdownTextStyle.Superscript,
)

private val Spaces = Regex("[ \\t]+")
private val Escaped = Regex("""\\([!-/:-@\[-`{-~])""")
private val Entity = Regex("&(#[xX]?[0-9a-fA-F]+|[a-zA-Z]+);")
private val HtmlTag = Regex("""<(/?)([a-zA-Z][a-zA-Z0-9]*)([^>]*)>""", RegexOption.DOT_MATCHES_ALL)
private val HtmlImage = Regex("""<img\b[^>]*>""", RegexOption.IGNORE_CASE)
private val HtmlHeading = Regex("""<h([1-6])\b[^>]*>(.*?)</h\1\s*>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
private val HtmlBreak = Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE)
private val HtmlComment = Regex("""<!--.*?-->""", RegexOption.DOT_MATCHES_ALL)
private val AnyTag = Regex("""<[^>]+>""")
private val OpenDetails = Regex("""<details\b""", RegexOption.IGNORE_CASE)
private val CloseDetails = Regex("""</details\s*>""", RegexOption.IGNORE_CASE)
private val SummaryTag = Regex("""<summary\b[^>]*>(.*?)</summary\s*>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
private val CenterAligned = Regex("""align\s*=\s*["']?center""", RegexOption.IGNORE_CASE)
private val HasScheme = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:")
private val FenceMarker = Regex("^(`{3,}|~{3,})")
private val WikiLink = Regex("""\[\[([^\]|]+)(?:\|([^\]]+))?]]""")
