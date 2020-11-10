package org.bibletranslationtools.fetcher.impl.repository

import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import org.bibletranslationtools.fetcher.repository.ContentCacheRepository
import org.bibletranslationtools.fetcher.usecase.DependencyResolver
import org.bibletranslationtools.fetcher.usecase.FetchBookViewData
import org.bibletranslationtools.fetcher.usecase.FetchChapterViewData
import org.bibletranslationtools.fetcher.usecase.ProductFileExtension
import org.wycliffeassociates.resourcecontainer.ResourceContainer

class ContentAvailabilityCache : ContentCacheRepository {
    private val repoDir = File(System.getenv("ORATURE_REPO_DIR"))
    private val mediaTypes = listOf("mp3", "wav")
    private var tree: List<LanguageCache>

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
        val url: String? = null
    )

    init {
        tree = cacheLanguages()
    }

    @Synchronized
    override fun update() {
        tree = cacheLanguages()
    }

    override fun isLanguageAvailable(code: String) = tree.any { it.code == code && it.availability }

    override fun isProductAvailable(productSlug: String, languageCode: String): Boolean {
        return tree.find {
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
        val productCache = tree.find {
            it.code == languageCode && it.availability
        }?.products?.find {
            it.slug == productSlug && it.availability
        } ?: return false

        return productCache.books.any {
            it.slug == bookSlug && it.availability
        }
    }

    override fun isChapterAvailable(
        number: Int,
        bookSlug: String,
        languageCode: String,
        productSlug: String
    ): Boolean {
        val bookCache = tree.find {
            it.code == languageCode && it.availability
        }?.products?.find {
            it.slug == productSlug && it.availability
        }?.books?.find {
            it.slug == bookSlug && it.availability
        } ?: return false

        return bookCache.chapters.any {
            it.number == number && it.availability
        }
    }

    private fun cacheLanguages(): List<LanguageCache> {
        val glList = PortGatewayLanguageCatalog().getAll().filter { it.code == "en" }
        return glList.map { lang ->
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
        val bookList = BookCatalogImpl().getAll()
        val product = ProductFileExtension.getType(productSlug)!!
        return bookList.map { book ->
            val chapters = cacheChapters(
                languageCode = languageCode, productSlug = productSlug, bookSlug = book.slug
            )
            var isAvailable: Boolean = chapters.any { it.availability }

            val bookUrl = when (product) {
                ProductFileExtension.ORATURE -> null
                else ->
                    FetchBookViewData(
                        DependencyResolver.bookRepository,
                        DependencyResolver.storageAccess,
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
        val chapterList = DependencyResolver.chapterCatalog.getAll(languageCode, bookSlug)
        val resultList = chapterList.map { ChapterCache(it.number) }
        val baseRcName = "%s_%s.zip"
        val resourceId = "ulb"
        val rcName = repoDir.resolve(String.format(baseRcName, languageCode, resourceId))
        if (!rcName.isFile) return resultList

        ResourceContainer.load(rcName).use { rc ->
            val mediaList =
                rc.media?.projects?.find { it.identifier == bookSlug }
                    ?.media?.filter { it.identifier in mediaTypes && it.chapterUrl.isNotEmpty() }

            mediaList?.forEach { media ->
                for (chapter in resultList) {
                    val url = URL(media.chapterUrl.replace("{chapter}", chapter.number.toString()))

                    // check if remote content is available
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "HEAD"
                    if (conn.responseCode == 200) chapter.availability = true
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
            DependencyResolver.chapterCatalog,
            DependencyResolver.storageAccess,
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
