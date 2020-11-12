package org.bibletranslationtools.fetcher.impl.repository

import io.ktor.http.HttpStatusCode
import org.bibletranslationtools.fetcher.repository.BookRepository
import org.bibletranslationtools.fetcher.repository.ChapterCatalog
import org.bibletranslationtools.fetcher.repository.ContentCacheRepository
import org.bibletranslationtools.fetcher.repository.DirectoryProvider
import org.bibletranslationtools.fetcher.repository.LanguageCatalog
import org.bibletranslationtools.fetcher.repository.StorageAccess
import java.net.HttpURLConnection
import java.net.URL
import org.bibletranslationtools.fetcher.usecase.FetchBookViewData
import org.bibletranslationtools.fetcher.usecase.FetchChapterViewData
import org.bibletranslationtools.fetcher.usecase.ProductFileExtension
import org.bibletranslationtools.fetcher.usecase.RequestResourceContainer
import org.slf4j.LoggerFactory
import org.wycliffeassociates.resourcecontainer.ResourceContainer
import java.io.IOException

class ContentAvailabilityCache(
    private val languageCatalog: LanguageCatalog,
    private val chapterCatalog: ChapterCatalog,
    private val bookRepository: BookRepository,
    private val storageAccess: StorageAccess,
    directoryProvider: DirectoryProvider
) : ContentCacheRepository {
    private val repoDir = directoryProvider.getRCRepositoriesDir()
    private val templateRCName = "%s_%s.zip"
    private val resourceId = "ulb"
    private var root: List<LanguageCache>
    private val logger = LoggerFactory.getLogger(javaClass)

    private data class LanguageCache(
        val code: String,
        var availability: Boolean = false,
        val products: List<ProductCache> = listOf()
    )

    private data class ProductCache(
        val slug: String,
        var availability: Boolean = false,
        val books: List<BookCache> = listOf()
    )

    private data class BookCache(
        val slug: String,
        var availability: Boolean = false,
        val url: String? = null,
        val chapters: List<ChapterCache> = listOf()
    )

    private data class ChapterCache(
        val number: Int,
        var availability: Boolean = false,
        var url: String? = null
    )

    init {
        root = cacheLanguages()
    }

    @Synchronized
    override fun update() {
        root = cacheLanguages()
    }

    override fun isLanguageAvailable(code: String) = root.any { it.code == code && it.availability }

    override fun isProductAvailable(productSlug: String, languageCode: String): Boolean {
        return root.find {
            it.code == languageCode && it.availability
        }?.products?.any {
            it.slug == productSlug
        } ?: false
    }

    override fun isBookAvailable(
        bookSlug: String,
        languageCode: String,
        productSlug: String
    ): Boolean {
        val productCache = root.find {
            it.code == languageCode && it.availability
        }?.products?.find {
            it.slug == productSlug && it.availability
        } ?: return false

        return productCache.books.any {
            it.slug == bookSlug && it.availability
        }
    }

    override fun getChapterUrl(
        number: Int,
        bookSlug: String,
        languageCode: String,
        productSlug: String
    ): String? {
        val bookCache = root.find {
            it.code == languageCode && it.availability
        }?.products?.find {
            it.slug == productSlug && it.availability
        }?.books?.find {
            it.slug == bookSlug && it.availability
        } ?: return null

        return bookCache.chapters.find {
            it.number == number
        }?.url
    }

    override fun getBookUrl(
        bookSlug: String,
        languageCode: String,
        productSlug: String
    ): String? {
        return root.find {
            it.code == languageCode && it.availability
        }?.products?.find {
            it.slug == productSlug && it.availability
        }?.books?.find {
            it.slug == bookSlug
        }?.url
    }

    private fun cacheLanguages(): List<LanguageCache> {
        val glList = languageCatalog.getAll()//.filter {it.code == "en" ||it.code == "fr"}
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
        val chapterList = try {
            chapterCatalog.getAll(languageCode, bookSlug)
        } catch (ex: Exception) {
            logger.error("An error occurred while getting chapter catalog for $languageCode - $bookSlug")
            throw ex
        }

        val resultList = chapterList.map { ChapterCache(it.number) }
        val rcFile = repoDir.resolve(String.format(templateRCName, languageCode, resourceId))
        if (!rcFile.isFile) return resultList

        val mediaTypes = RequestResourceContainer.mediaTypes.map { it.name.toLowerCase() }

        ResourceContainer.load(rcFile).use { rc ->
            val mediaList =
                rc.media?.projects?.find { it.identifier == bookSlug }
                    ?.media?.filter { it.identifier in mediaTypes && it.chapterUrl.isNotEmpty() }

            mediaList?.forEach { media ->
                for (chapter in resultList) {
                    val url = URL(media.chapterUrl.replace("{chapter}", chapter.number.toString()))

                    // check if remote content is available
                    try {
                        val conn = url.openConnection() as HttpURLConnection
                        conn.requestMethod = "HEAD"
                        if (conn.responseCode == HttpStatusCode.OK.value) {
                            chapter.availability = true
                            chapter.url = "#"
                        }
                        conn.disconnect()
                    } catch (ex: IOException) {
                        chapter.availability = false
                    }
                }
            }
        }

        return resultList
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
}
