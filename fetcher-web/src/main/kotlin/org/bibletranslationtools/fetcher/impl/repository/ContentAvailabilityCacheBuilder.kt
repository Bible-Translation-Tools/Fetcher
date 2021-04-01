package org.bibletranslationtools.fetcher.impl.repository

import java.io.File
import org.bibletranslationtools.fetcher.config.CDN_BASE_URL
import org.bibletranslationtools.fetcher.config.CONTENT_ROOT_DIR
import org.bibletranslationtools.fetcher.data.Book
import org.bibletranslationtools.fetcher.data.Language
import org.bibletranslationtools.fetcher.data.Product
import org.bibletranslationtools.fetcher.repository.BookRepository
import org.bibletranslationtools.fetcher.repository.ChapterCatalog
import org.bibletranslationtools.fetcher.repository.LanguageCatalog
import org.bibletranslationtools.fetcher.repository.ResourceContainerRepository
import org.bibletranslationtools.fetcher.repository.StorageAccess
import org.bibletranslationtools.fetcher.usecase.FetchBookViewData
import org.bibletranslationtools.fetcher.usecase.FetchChapterViewData
import org.bibletranslationtools.fetcher.usecase.ProductFileExtension
import org.bibletranslationtools.fetcher.usecase.RequestResourceContainer
import org.bibletranslationtools.fetcher.usecase.cache.AvailabilityCache
import org.bibletranslationtools.fetcher.usecase.cache.BookCache
import org.bibletranslationtools.fetcher.usecase.cache.ChapterCache
import org.bibletranslationtools.fetcher.usecase.cache.LanguageCache
import org.bibletranslationtools.fetcher.usecase.cache.ProductCache
import org.wycliffeassociates.resourcecontainer.ResourceContainer

class ContentAvailabilityCacheBuilder(
    private val languageCatalog: LanguageCatalog,
    private val chapterCatalog: ChapterCatalog,
    private val bookRepository: BookRepository,
    private val storageAccess: StorageAccess,
    private val rcRepo: ResourceContainerRepository
) {
    private val resourceId = "ulb"

    @Synchronized
    fun build(): AvailabilityCache {
        return AvailabilityCache(cacheLanguages())
    }

    private fun cacheLanguages(): List<LanguageCache> {
        val glList = languageCatalog.getAll()
        return glList.map { lang ->
            val products = cacheProducts(lang)
            val isAvailable = products.any { it.availability }
            LanguageCache(lang.code, isAvailable, products)
        }
    }

    private fun cacheProducts(language: Language): List<ProductCache> {
        val productList = ProductCatalogImpl().getAll()
        return productList.map { prod ->
            val books = cacheBooks(language, prod)
            val isAvailable = books.any { it.availability }
            ProductCache(prod.slug, isAvailable, books)
        }
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
                oratureChapters(language.code, book.slug)
            else ->
                audioChapters(language, product, book)
        }
    }

    private fun oratureChapters(
        languageCode: String,
        bookSlug: String
    ): List<ChapterCache> {
        val chapters = chapterCatalog.getAll(languageCode, bookSlug)
        val chapterList = chapters.map { ChapterCache(it.number) }
        val rcFile = rcRepo.getRC(languageCode, resourceId)
            ?: return chapterList
        val mediaTypes = RequestResourceContainer.mediaTypes.map { it.name.toLowerCase() }

        ResourceContainer.load(rcFile).use { rc ->
            val mediaList =
                rc.media?.projects?.find { it.identifier == bookSlug }
                    ?.media?.filter { it.identifier in mediaTypes && it.chapterUrl.isNotEmpty() }

            mediaList?.forEach { media ->
                fetchChaptersFromMediaUrl(media.chapterUrl, chapterList)
            }
        }

        return chapterList
    }

    private fun audioChapters(
        language: Language,
        product: Product,
        book: Book
    ): List<ChapterCache> {
        val chaptersFromDirectory = FetchChapterViewData(
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

    private fun fetchChaptersFromMediaUrl(url: String, chapterList: List<ChapterCache>) {
        for (chapter in chapterList) {
            val relativePath = File(url).relativeTo(File(CDN_BASE_URL))
                .path.replace("{chapter}", chapter.number.toString())

            val chapterFile = File(CONTENT_ROOT_DIR).resolve(relativePath)
            chapter.availability = chapterFile.exists()

            if (chapter.availability) chapter.url = "#"
        }
    }
}
