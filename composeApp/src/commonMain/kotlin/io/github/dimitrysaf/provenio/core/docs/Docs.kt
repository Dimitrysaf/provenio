package io.github.dimitrysaf.provenio.core.docs

import co.touchlab.kermit.Logger
import io.github.dimitrysaf.provenio.core.addons.httpRequestRaw
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource

/** A documentation page: [name] is its file in the wiki, `Getting-Started` for "Getting Started". */
data class DocsPage(val name: String, val title: String, val section: String?)

data class DocsState(
    val pages: List<DocsPage> = emptyList(),
    val texts: Map<String, String> = emptyMap(),
    val loading: Boolean = false,
    /** The wiki could not be reached; what is shown, if anything, is the copy kept from before. */
    val offline: Boolean = false,
    /** The wiki was reached and has no pages yet. */
    val empty: Boolean = false,
)

/** What came of asking for a page. */
sealed interface DocsPageResult {
    data class Found(val text: String) : DocsPageResult

    /** The wiki answered that there is no such page. */
    data object Missing : DocsPageResult

    /** The wiki could not be reached and there is no copy of the page. */
    data object Unreachable : DocsPageResult
}

private sealed interface Fetched {
    data class Text(val text: String) : Fetched
    data object Missing : Fetched
    data object Failed : Fetched
}

/** A page the search found, with the words around the first place it matched. */
data class DocsSearchResult(val page: DocsPage, val snippet: String?)

/**
 * The app's documentation, read from the project's GitHub wiki, so every installed version shows
 * what the wiki says now. The wiki's sidebar is its table of contents, and the last copy of each
 * page is kept for when there is no connection.
 */
object Docs {
    const val WikiUrl = "https://github.com/Dimitrysaf/provenio/wiki"
    const val HomePage = "Home"
    private const val RawBaseUrl = "https://raw.githubusercontent.com/wiki/Dimitrysaf/provenio/"
    private const val SidebarPage = "_Sidebar"
    private const val MaxPageBytes = 2 * 1024 * 1024
    private const val MaxParallelRequests = 4
    private val RefreshInterval = 10.minutes

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val log = Logger.withTag("Docs")
    private val _state = MutableStateFlow(DocsState())
    val state: StateFlow<DocsState> = _state.asStateFlow()

    private var loadedSaved = false
    private var lastRefresh: TimeSource.Monotonic.ValueTimeMark? = null
    private var refreshing: Job? = null

    /** Shows what was kept from last time, then fetches the latest unless it was fetched moments ago. */
    fun refresh(force: Boolean = false) {
        loadSaved()
        if (refreshing?.isActive == true) return
        if (!force && lastRefresh?.let { it.elapsedNow() < RefreshInterval } == true) return
        refreshing = scope.launch {
            _state.update { it.copy(loading = true) }
            val sidebar = fetch(SidebarPage)
            if (sidebar is Fetched.Text) {
                DocsStorage.save(key(SidebarPage), sidebar.text)
                _state.update { it.copy(pages = sidebarPages(sidebar.text)) }
            }
            val requests = Semaphore(MaxParallelRequests)
            val fetched = coroutineScope {
                _state.value.pages.map { page ->
                    async { requests.withPermit { fetch(page.name).also { store(page.name, it) } } }
                }.awaitAll()
            }
            // An answer, even that a page does not exist, means the wiki was reached.
            val reached = (fetched + sidebar).any { it != Fetched.Failed }
            if (reached) lastRefresh = TimeSource.Monotonic.markNow()
            _state.update { it.copy(loading = false, offline = !reached, empty = reached && it.texts.isEmpty()) }
        }
    }

    /** A page: the copy at hand, or fetched now for a page not read before. */
    suspend fun page(name: String): DocsPageResult {
        loadSaved()
        _state.value.texts[name]?.let { return DocsPageResult.Found(it) }
        return when (val fetched = fetch(name)) {
            is Fetched.Text -> {
                store(name, fetched)
                DocsPageResult.Found(fetched.text)
            }
            Fetched.Missing -> DocsPageResult.Missing
            Fetched.Failed -> DocsPageResult.Unreachable
        }
    }

    /** Where a page's pictures with a relative address are found. */
    fun pageBaseUrl(name: String): String = RawBaseUrl + encodePath(name)

    /** The page on GitHub, for opening in a browser. */
    fun pageWebUrl(name: String): String = "$WikiUrl/${encodePath(name)}"

    /**
     * The page a link inside the documentation leads to, by name, or null for a link that leaves it:
     * `Getting-Started`, `./Getting-Started.md` and the page's address on GitHub all lead to the same page.
     */
    fun pageNameOf(link: String): String? {
        val target = link.trim()
        val inWiki = target.removePrefix(WikiUrl).takeIf { it != target }
        val path = when {
            inWiki != null -> inWiki.removePrefix("/")
            HasScheme.containsMatchIn(target) || target.startsWith("#") || target.startsWith("//") -> return null
            else -> target
        }
        val name = path.substringBefore('#').substringBefore('?').removePrefix("./").removeSuffix(".md")
        return name.ifEmpty { HomePage }
    }

    /** Pages whose title or text holds [query], titles first. */
    fun search(query: String): List<DocsSearchResult> {
        val needle = query.trim()
        if (needle.isEmpty()) return emptyList()
        val state = _state.value
        val byTitle = state.pages.filter { it.title.contains(needle, ignoreCase = true) }
        val byText = state.pages.filter { it !in byTitle && state.texts[it.name]?.contains(needle, ignoreCase = true) == true }
        return (byTitle + byText).map { page -> DocsSearchResult(page, state.texts[page.name]?.let { snippetOf(it, needle) }) }
    }

    private fun store(name: String, fetched: Fetched) {
        if (fetched !is Fetched.Text) return
        DocsStorage.save(key(name), fetched.text)
        _state.update { it.copy(texts = it.texts + (name to fetched.text)) }
    }

    private fun loadSaved() {
        if (loadedSaved) return
        loadedSaved = true
        val pages = DocsStorage.load(key(SidebarPage))?.let(::sidebarPages) ?: listOf(DocsPage(HomePage, HomePage, null))
        val texts = pages.mapNotNull { page -> DocsStorage.load(key(page.name))?.let { page.name to it } }.toMap()
        _state.update { it.copy(pages = it.pages.ifEmpty { pages }, texts = texts + it.texts) }
    }

    private suspend fun fetch(name: String): Fetched = runCatching {
        val response = httpRequestRaw(
            method = "GET",
            url = "$RawBaseUrl${encodePath(name)}.md",
            headers = emptyMap(),
            body = "",
            maxResponseBodyBytes = MaxPageBytes,
        )
        when (response.status) {
            in 200..299 -> Fetched.Text(response.body)
            404 -> Fetched.Missing
            else -> Fetched.Failed
        }
    }.onFailure { log.w(it) { "Fetching the documentation page $name failed" } }.getOrDefault(Fetched.Failed)

    private fun key(name: String): String = "page:$name"
}

/**
 * The pages the wiki's sidebar lists, in its order: a heading, or a line that is only bold text,
 * starts a section, and every link to a wiki page on a line is a page. Home always comes first.
 */
internal fun sidebarPages(sidebar: String): List<DocsPage> {
    val pages = mutableListOf<DocsPage>()
    var section: String? = null
    sidebar.lines().forEach { raw ->
        val line = raw.trim()
        SidebarHeading.matchEntire(line)?.let {
            section = cleanTitle(it.groupValues[1])
            return@forEach
        }
        val links = WikiLinkPattern.findAll(line).map { match ->
            val text = match.groupValues[1].trim()
            match.groupValues[2].trim().ifEmpty { text }.replace(' ', '-') to text
        } + MarkdownLinkPattern.findAll(line).mapNotNull { match ->
            Docs.pageNameOf(match.groupValues[2])?.let { it to cleanTitle(match.groupValues[1]) }
        }
        val found = links.toList()
        if (found.isEmpty()) {
            SidebarBold.matchEntire(line)?.let { section = cleanTitle(it.groupValues[1]) }
        } else {
            found.forEach { (name, title) -> pages += DocsPage(name, title.ifEmpty { name.replace('-', ' ') }, section) }
        }
    }
    if (pages.none { it.name.equals(Docs.HomePage, ignoreCase = true) }) pages.add(0, DocsPage(Docs.HomePage, Docs.HomePage, null))
    return pages.distinctBy { it.name.lowercase() }
}

private fun cleanTitle(text: String): String = text.replace(Regex("[*_`]"), "").trim()

private fun snippetOf(text: String, needle: String): String? {
    val plain = text.replace(MarkupNoise, " ").replace(Regex("\\s+"), " ")
    val at = plain.indexOf(needle, ignoreCase = true)
    if (at < 0) return null
    val start = (at - SnippetContext).coerceAtLeast(0)
    val end = (at + needle.length + SnippetContext).coerceAtMost(plain.length)
    return (if (start > 0) "…" else "") + plain.substring(start, end).trim() + (if (end < plain.length) "…" else "")
}

// A page name in an address: letters and digits as they are, anything else percent-encoded as UTF-8.
private fun encodePath(name: String): String = buildString {
    name.encodeToByteArray().forEach { byte ->
        val char = (byte.toInt() and 0xFF).toChar()
        if (char.isLetterOrDigit() && char.code < 0x80 || char in "-_.~") append(char) else {
            append('%')
            append((byte.toInt() and 0xFF).toString(16).uppercase().padStart(2, '0'))
        }
    }
}

private const val SnippetContext = 60
private val HasScheme = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:")
private val SidebarHeading = Regex("^#{1,6}\\s+(.+?)\\s*#*$")
private val SidebarBold = Regex("^(?:\\*\\*|__)(.+)(?:\\*\\*|__)$")
private val WikiLinkPattern = Regex("""\[\[([^\]|]+)(?:\|([^\]]+))?]]""")
private val MarkdownLinkPattern = Regex("""(?<!!)\[([^\]]+)]\(([^)\s]+)(?:\s+"[^"]*")?\)""")
private val MarkupNoise = Regex("""[#>*_`|\[\]]|\([^)]*\)|<[^>]+>""")
