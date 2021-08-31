package org.bibletranslationtools.fetcher.impl.repository

import java.util.stream.Collectors
import org.bibletranslationtools.fetcher.config.EnvironmentConfig
import org.bibletranslationtools.fetcher.data.Book
import org.bibletranslationtools.fetcher.data.Language
import org.bibletranslationtools.fetcher.data.Product
import org.bibletranslationtools.fetcher.repository.BookRepository
import org.bibletranslationtools.fetcher.repository.ChapterCatalog
import org.bibletranslationtools.fetcher.repository.LanguageCatalog
import org.bibletranslationtools.fetcher.repository.ProductCatalog
import org.bibletranslationtools.fetcher.repository.StorageAccess
import org.bibletranslationtools.fetcher.usecase.FetchBookViewData
import org.bibletranslationtools.fetcher.usecase.FetchChapterViewData
import org.bibletranslationtools.fetcher.usecase.ProductFileExtension
import org.bibletranslationtools.fetcher.usecase.cache.AvailabilityCache
import org.bibletranslationtools.fetcher.usecase.cache.BookCache
import org.bibletranslationtools.fetcher.usecase.cache.ChapterCache
import org.bibletranslationtools.fetcher.usecase.cache.LanguageCache
import org.bibletranslationtools.fetcher.usecase.cache.ProductCache
import org.slf4j.LoggerFactory

class ContentAvailabilityCacheBuilder(
    private val envConfig: EnvironmentConfig,
    private val languageCatalog: LanguageCatalog,
    private val productCatalog: ProductCatalog,
    private val chapterCatalog: ChapterCatalog,
    private val bookRepository: BookRepository,
    private val storageAccess: StorageAccess
) {
    private val resourceId = "ulb"
    private val logger = LoggerFactory.getLogger(javaClass)

    @Synchronized
    fun build(): AvailabilityCache {
        return AvailabilityCache(cacheLanguages())
    }

    private fun cacheLanguages(): List<LanguageCache> {
        val glList = languageCatalog.getAll()
        return glList.parallelStream().map { lang ->
            val products = cacheProducts(lang)
            val isAvailable = products.any { it.availability }
            LanguageCache(lang.code, isAvailable, products)
        }.collect(Collectors.toList())
    }

    private fun cacheProducts(language: Language): List<ProductCache> {
        val productList = productCatalog.getAll()
        return productList.parallelStream().map { prod ->
            val books = cacheBooks(language, prod)
            val isAvailable = books.any { it.availability }
            ProductCache(prod.slug, isAvailable, books)
        }.collect(Collectors.toList())
    }

    private fun cacheBooks(language: Language, product: Product): List<BookCache> {
        val productExtension = ProductFileExtension.getType(product.slug)!!
        val bookList = bookRepository.getBooks(resourceId)

        return bookList.map { book ->
            val chapters = cacheChapters(
                language, product, book
            )
            val isAvailable: Boolean = chapters.any { it.availability }

            val bookUrl = when (productExtension) {
                ProductFileExtension.ORATURE -> if (isAvailable) "#" else null
                else ->
                    FetchBookViewData(
                        envConfig,
                        bookRepository,
                        storageAccess,
                        language,
                        product
                    ).getBookDownloadUrl(book.slug)
            }

            BookCache(book.slug, isAvailable || bookUrl != null, bookUrl, chapters)
        }
    }

    private fun cacheChapters(
        language: Language,
        product: Product,
        book: Book
    ): List<ChapterCache> {
        return when (ProductFileExtension.getType(product.slug)) {
            ProductFileExtension.ORATURE ->
                oratureChapters(language, book)
            else ->
                audioChapters(language, product, book)
        }
    }

    private fun oratureChapters(
        language: Language,
        book: Book
    ): List<ChapterCache> {
        // if there content in mp3, then Orature content should be available
        val product = productCatalog.getProduct(ProductFileExtension.MP3.fileType)!!
        val chapters = audioChapters(language, product, book)
        chapters.forEach {
            it.url = if (it.availability) "#" else null
        }
        return chapters
    }

    private fun audioChapters(
        language: Language,
        product: Product,
        book: Book
    ): List<ChapterCache> {
        val chaptersFromDirectory = FetchChapterViewData(
            envConfig,
            chapterCatalog,
            storageAccess,
            language,
            product,
            book
        ).chaptersFromDirectory()

        return chaptersFromDirectory.map {
            val isAvailable = it.url != null
            ChapterCache(it.chapterNumber, isAvailable, it.url)
        }
    }
}
