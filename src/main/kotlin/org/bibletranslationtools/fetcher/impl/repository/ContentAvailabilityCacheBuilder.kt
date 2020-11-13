package org.bibletranslationtools.fetcher.impl.repository

import io.ktor.http.HttpStatusCode
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.bibletranslationtools.fetcher.data.cache.AvailabilityCache
import org.bibletranslationtools.fetcher.data.cache.BookCache
import org.bibletranslationtools.fetcher.data.cache.ChapterCache
import org.bibletranslationtools.fetcher.data.cache.LanguageCache
import org.bibletranslationtools.fetcher.data.cache.ProductCache
import org.bibletranslationtools.fetcher.repository.BookRepository
import org.bibletranslationtools.fetcher.repository.ChapterCatalog
import org.bibletranslationtools.fetcher.repository.DirectoryProvider
import org.bibletranslationtools.fetcher.repository.LanguageCatalog
import org.bibletranslationtools.fetcher.repository.StorageAccess
import org.bibletranslationtools.fetcher.usecase.FetchBookViewData
import org.bibletranslationtools.fetcher.usecase.FetchChapterViewData
import org.bibletranslationtools.fetcher.usecase.ProductFileExtension
import org.bibletranslationtools.fetcher.usecase.RequestResourceContainer
import org.slf4j.LoggerFactory
import org.wycliffeassociates.resourcecontainer.ResourceContainer

class ContentAvailabilityCacheBuilder(
    private val languageCatalog: LanguageCatalog,
    private val chapterCatalog: ChapterCatalog,
    private val bookRepository: BookRepository,
    private val storageAccess: StorageAccess,
    directoryProvider: DirectoryProvider
) {
    private val repoDir = directoryProvider.getRCRepositoriesDir()
    private val templateRCName = "%s_%s.zip"
    private val resourceId = "ulb"
    private val logger = LoggerFactory.getLogger(javaClass)

    fun build(): AvailabilityCache {
        return AvailabilityCache(cacheLanguages())
    }

    private fun cacheLanguages(): List<LanguageCache> {
        val glList = languageCatalog.getAll()
        return glList.map { lang ->
            println(glList.indexOf(lang))
            val products = cacheProducts(lang.code)
            val isAvailable = products.any { it.availability }
            LanguageCache(lang.code, isAvailable, products)
        }
    }

    private fun cacheProducts(languageCode: String): List<ProductCache> {
        val productList = ProductCatalogImpl().getAll()
        return productList.map { prod ->
            val books = cacheBooks(languageCode, prod.slug)
            val isAvailable = books.any { it.availability }
            ProductCache(prod.slug, isAvailable, books)
        }
    }

    private fun cacheBooks(languageCode: String, productSlug: String): List<BookCache> {
        val product = ProductFileExtension.getType(productSlug)!!
        val bookList = bookRepository.getBooks(resourceId)

        return bookList.map { book ->
            val chapters = cacheChapters(
                languageCode = languageCode, productSlug = productSlug, bookSlug = book.slug
            )
            val isAvailable: Boolean = chapters.any { it.availability }

            val bookUrl = when (product) {
                ProductFileExtension.ORATURE -> if (isAvailable) "#" else null
                else ->
                    FetchBookViewData(
                        bookRepository,
                        storageAccess,
                        languageCode,
                        productSlug
                    ).getBookDownloadUrl(book.slug)
            }

            BookCache(book.slug, isAvailable || bookUrl != null, bookUrl, chapters)
        }
    }

    private fun cacheChapters(
        languageCode: String,
        productSlug: String,
        bookSlug: String
    ): List<ChapterCache> {
        return when (ProductFileExtension.getType(productSlug)) {
            ProductFileExtension.ORATURE ->
                oratureChapters(languageCode, bookSlug)
            else ->
                audioChapters(languageCode, productSlug, bookSlug)
        }
    }

    private fun oratureChapters(
        languageCode: String,
        bookSlug: String
    ): List<ChapterCache> {
        val chapters = chapterCatalog.getAll(languageCode, bookSlug)
        val chapterList = chapters.map { ChapterCache(it.number) }
        val rcFile = repoDir.resolve(String.format(templateRCName, languageCode, resourceId))
        if (!rcFile.isFile) return chapterList

        fetchFromRepo(rcFile, bookSlug, chapterList)

        return chapterList
    }

    private fun audioChapters(
        languageCode: String,
        productSlug: String,
        bookSlug: String
    ): List<ChapterCache> {
        val chaptersFromDirectory = FetchChapterViewData(
            chapterCatalog,
            storageAccess,
            languageCode = languageCode,
            productSlug = productSlug,
            bookSlug = bookSlug
        ).chaptersFromDirectory()

        return chaptersFromDirectory.map {
            val isAvailable = it.url != null
            ChapterCache(it.chapterNumber, isAvailable, it.url)
        }
    }

    private fun fetchFromRepo(
        rcFile: File,
        bookSlug: String,
        chapterList: List<ChapterCache>
    ) {
        val mediaTypes = RequestResourceContainer.mediaTypes.map { it.name.toLowerCase() }

        ResourceContainer.load(rcFile).use { rc ->
            val mediaList =
                rc.media?.projects?.find { it.identifier == bookSlug }
                    ?.media?.filter { it.identifier in mediaTypes && it.chapterUrl.isNotEmpty() }

            mediaList?.forEach { media ->
                for (chapter in chapterList) {
                    val url = URL(media.chapterUrl.replace("{chapter}", chapter.number.toString()))

                    // check if remote content is available
                    try {
                        val conn = url.openConnection() as HttpURLConnection
                        conn.requestMethod = "HEAD"
                        chapter.availability = conn.responseCode == HttpStatusCode.OK.value
                        conn.disconnect()
                    } catch (ex: IOException) {
                        chapter.availability = false
                    }
                    if (chapter.availability) chapter.url = "#"
                }
            }
        }
    }
}
