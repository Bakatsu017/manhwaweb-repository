package eu.kanade.tachiyomi.extension.es.manhwaweb

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.*
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class ManhwaWeb : ParsedHttpSource() {

    override val name = "ManhwaWeb"
    override val baseUrl = "https://manhwaweb.com"
    override val lang = "es"
    override val supportsLatest = true

    // --- POPULAR ---
    override fun popularMangaRequest(page: Int): Request = GET("$baseUrl/manhwas?page=$page", headers)
    override fun popularMangaSelector() = "div.element"
    override fun popularMangaFromElement(element: Element): SManga = SManga.create().apply {
        title = element.select("h4 a").text()
        thumbnail_url = element.select("img").attr("src")
        setUrlWithoutDomain(element.select("h4 a").attr("href"))
    }
    override fun popularMangaNextPageSelector() = "a.next"

    // --- LATEST ---
    override fun latestUpdatesRequest(page: Int): Request = GET("$baseUrl/manhwas?page=$page&order=update", headers)
    override fun latestUpdatesSelector() = popularMangaSelector()
    override fun latestUpdatesFromElement(element: Element) = popularMangaFromElement(element)
    override fun latestUpdatesNextPageSelector() = popularMangaNextPageSelector()

    // --- SEARCH ---
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request = 
        GET("$baseUrl/buscar?q=$query&page=$page", headers)
    override fun searchMangaSelector() = popularMangaSelector()
    override fun searchMangaFromElement(element: Element) = popularMangaFromElement(element)
    override fun searchMangaNextPageSelector() = popularMangaNextPageSelector()

    // --- DETAILS ---
    override fun mangaDetailsParse(document: Document): SManga = SManga.create().apply {
        title = document.select("h1").text()
        thumbnail_url = document.select("div.cover img").attr("src")
        description = document.select("div.description").text()
        genre = document.select("div.genres a").joinToString { it.text() }
        status = SManga.UNKNOWN
    }

    // --- CHAPTERS ---
    override fun chapterListSelector() = "div.chapters div.element"
    override fun chapterFromElement(element: Element): SChapter = SChapter.create().apply {
        name = element.select("a").text()
        setUrlWithoutDomain(element.select("a").attr("href"))
    }

    // --- PAGES (EL LECTOR) ---
    override fun pageListParse(document: Document): List<Page> {
        val images = document.select("div.reading-content img, div#images img, img.imagen, script")
        val urls = mutableListOf<String>()
        val regex = Regex("""https?://[^\s"'\\]+?\.(?:webp|jpg|jpeg|png)""")
        
        val matches = regex.findAll(document.html())
        for (match in matches) {
            val url = match.value
            if (!url.contains("avatar") && !url.contains("logo") && !urls.contains(url)) {
                urls.add(url)
            }
        }
        
        if (urls.isEmpty()) {
            return images.mapIndexedNotNull { i, img ->
                val url = if (img.attr("data-src").isNotEmpty()) img.attr("data-src") else img.attr("src")
                if (url.isNotEmpty() && url.startsWith("http")) Page(i, imageUrl = url) else null
            }
        }

        return urls.mapIndexed { i, url ->
            Page(i, imageUrl = url)
        }
    }

    override fun imageUrlParse(document: Document): String = ""
}
