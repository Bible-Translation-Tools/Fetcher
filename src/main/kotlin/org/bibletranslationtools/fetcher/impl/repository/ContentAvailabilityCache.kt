package org.bibletranslationtools.fetcher.impl.repository

import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import org.bibletranslationtools.fetcher.repository.ContentCacheRepository
import org.wycliffeassociates.resourcecontainer.ResourceContainer

class ContentAvailabilityCache : ContentCacheRepository {
    private val repoDir = File("E:/miscs/rc/tmp")
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
        val chapters: List<ChapterCache> = listOf()
    )

    private data class ChapterCache(
        val number: Int,
        val url: String? = null,
        var availability: Boolean = false
    )

    init {
        tree = listOf()
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
        val glList = PortGatewayLanguageCatalog().getAll()
        return glList.map { lang ->
            val products = cacheProducts(lang.code)
            val available = products.any { it.availability }
            LanguageCache(lang.code, available, products)
        }
    }

    private fun cacheProducts(languageCode: String): List<ProductCache> {
        val productList = ProductCatalogImpl().getAll().filter { it.slug == "orature" }
        return productList.map { prod ->
            val books = cacheBooks(languageCode, prod.slug)
            val available = books.any { it.availability }
            ProductCache(prod.slug, available, books)
        }
    }

    private fun cacheBooks(languageCode: String, productSlug: String): List<BookCache> {
        val bookList = BookCatalogImpl().getAll()

        return bookList.map { book ->
            val chapters = cacheChapters(languageCode, book.slug, productSlug)
            val available = chapters.any { it.availability }
            BookCache(book.slug, available, chapters)
        }
    }

    private fun cacheChapters(
        languageCode: String,
        productSlug: String,
        bookSlug: String
    ): List<ChapterCache> {
        val chapterList = ChapterCatalogImpl().getAll(languageCode, bookSlug)
        val resultList = chapterList.map { ChapterCache(it.number) }
        val baseRcName = "%s_%s.zip"
        val rcName = repoDir.resolve(String.format(baseRcName, languageCode, "ulb"))
        if (!rcName.isFile) return resultList

        ResourceContainer.load(rcName).use { rc ->
            val mediaList =
                rc.media?.projects?.find { it.identifier == bookSlug }
                    ?.media?.filter { it.identifier in mediaTypes && it.chapterUrl.isNotEmpty() }
            mediaList?.forEach { media ->
                for (chapter in resultList) {
                    val url = URL(media.chapterUrl.replace("{chapter}", chapter.number.toString()))
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "HEAD"
                    if (conn.responseCode == 200) chapter.availability = true
                }
            }
        }

        return resultList
    }
}
