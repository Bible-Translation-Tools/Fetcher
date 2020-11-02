package org.bibletranslationtools.fetcher.web.controllers.utils

import org.bibletranslationtools.fetcher.repository.BookRepository
import org.bibletranslationtools.fetcher.repository.LanguageCatalog
import org.bibletranslationtools.fetcher.repository.ProductCatalog
import java.lang.NumberFormatException

class RoutingValidator(
    private val languageCatalog: LanguageCatalog,
    private val productCatalog: ProductCatalog,
    private val bookRepository: BookRepository
) {

    fun isLanguageCodeValid(languageCode: String?): Boolean {
        return when {
            languageCode.isNullOrEmpty() -> false
            languageCatalog.getLanguage(languageCode) == null -> false
            else -> true
        }
    }

    fun isProductSlugValid(productSlug: String?): Boolean {
        return when {
            productSlug.isNullOrEmpty() -> false
            productCatalog.getProduct(productSlug) == null -> false
            else -> true
        }
    }

    fun isBookSlugValid(bookSlug: String?): Boolean {
        return when {
            bookSlug.isNullOrEmpty() -> false
            bookRepository.getBook(bookSlug) == null -> false
            else -> true
        }
    }

    fun isChapterValid(chapter: String?): Boolean {
        return try {
            chapter?.toInt()
            true
        } catch (ex: NumberFormatException) {
            chapter == ALL_CHAPTERS_PARAM
        }
    }
}
