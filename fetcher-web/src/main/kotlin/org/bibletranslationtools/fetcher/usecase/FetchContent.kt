package org.bibletranslationtools.fetcher.usecase

import org.bibletranslationtools.fetcher.repository.BookRepository
import org.bibletranslationtools.fetcher.repository.ChapterCatalog
import org.bibletranslationtools.fetcher.repository.ContentCacheAccessor
import org.bibletranslationtools.fetcher.repository.LanguageRepository
import org.bibletranslationtools.fetcher.repository.StorageAccess
import org.bibletranslationtools.fetcher.usecase.viewdata.ChapterViewData

class FetchContent(
    private val cacheAccessor: ContentCacheAccessor,
    private val languageRepository: LanguageRepository,
    private val chapterCatalog: ChapterCatalog,
    private val bookRepository: BookRepository,
    private val storageAccess: StorageAccess
) {
    fun isLanguageAvailable(code: String): Boolean {
        return if (languageRepository.isGateway(code)) {
            cacheAccessor.isLanguageAvailable(code)
        } else {
            code in storageAccess.getLanguageCodes()
        }
    }

    fun isProductAvailable(productSlug: String, languageCode: String): Boolean {
        return when {
            languageRepository.isGateway(languageCode) -> {
                cacheAccessor.isProductAvailable(productSlug, languageCode)
            }
            // heart languages
            ProductFileExtension.getType(productSlug) == ProductFileExtension.MP3 -> {
                storageAccess.hasProductContent(productSlug, languageCode)
            }
            else -> false
        }
    }

    fun isBookAvailable(
        bookSlug: String,
        languageCode: String,
        productSlug: String
    ): Boolean {
        return when {
            languageRepository.isGateway(languageCode) -> {
                cacheAccessor.isBookAvailable(bookSlug, languageCode, productSlug)
            }
            ProductFileExtension.getType(productSlug) == ProductFileExtension.MP3 -> {
                FetchBookViewData(
                    bookRepository,
                    storageAccess,
                    languageCode,
                    productSlug
                ).hasBookContent(bookSlug)
            }
            else -> false
        }
    }

    fun getBookUrl(
        bookSlug: String,
        languageCode: String,
        productSlug: String
    ): String? {
        return when {
            languageRepository.isGateway(languageCode) -> {
                cacheAccessor.getBookUrl(bookSlug, languageCode, productSlug)
            }
            else -> {
                FetchBookViewData(
                    bookRepository,
                    storageAccess,
                    languageCode,
                    productSlug
                ).getBookDownloadUrl(bookSlug)
            }
        }
    }

    fun getChapterContentList(
        languageCode: String,
        productSlug: String,
        bookSlug: String
    ): List<ChapterViewData> {
        return if (languageRepository.isGateway(languageCode)) {
            // go to cache
            val chapters = chapterCatalog.getAll(languageCode, bookSlug)
            chapters.map {
                val requestUrl = cacheAccessor.getChapterUrl(
                    number = it.number,
                    bookSlug = bookSlug,
                    languageCode = languageCode,
                    productSlug = productSlug
                )
                ChapterViewData(it.number, url = requestUrl)
            }
        } else {
            FetchChapterViewData(
                chapterCatalog,
                storageAccess,
                languageCode = languageCode,
                productSlug = productSlug,
                bookSlug = bookSlug
            ).chaptersFromDirectory()
        }
    }
}