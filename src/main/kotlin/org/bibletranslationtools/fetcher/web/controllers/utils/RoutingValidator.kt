package org.bibletranslationtools.fetcher.web.controllers.utils

import org.bibletranslationtools.fetcher.usecase.DependencyResolver
import java.lang.NumberFormatException

class RoutingValidator(private val resolver: DependencyResolver) {

    fun isLanguageCodeValid(languageCode: String?): Boolean {
        return when {
            languageCode.isNullOrEmpty() -> false
            resolver.languageCatalog.getLanguage(languageCode) == null -> false
            else -> true
        }
    }

    fun isProductSlugValid(productSlug: String?): Boolean {
        return when {
            productSlug.isNullOrEmpty() -> false
            resolver.productCatalog.getProduct(productSlug) == null -> false
            else -> true
        }
    }

    fun isBookSlugValid(bookSlug: String?): Boolean {
        return when {
            bookSlug.isNullOrEmpty() -> false
            resolver.bookRepository.getBook(bookSlug) == null -> false
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
