package org.bibletranslationtools.fetcher.web

import org.bibletranslationtools.fetcher.usecase.DependencyResolver

class RoutingValidator(val resolver: DependencyResolver) {

    fun isLanguageCodeValid(languageCode: String?): Boolean {
        var isValid = true

        if(languageCode.isNullOrEmpty()) isValid = false
        if(
            !resolver.languageRepository.getLanguages().map { it.code == languageCode }.contains(true)
        ) isValid = false

        return isValid
    }

    fun isProductSlugValid(productSlug: String?): Boolean {
        var isValid = true

        if(productSlug.isNullOrEmpty()) isValid = false
        if(
            !resolver.productCatalog.getAll().map { it.slug == productSlug }.contains(true)
        ) isValid = false

        return isValid
    }

    fun isBookSlugValid(languageCode: String?, bookSlug: String?): Boolean {
        var isValid = true

        when {
            bookSlug.isNullOrEmpty() -> isValid = false
            languageCode.isNullOrEmpty() -> isValid = false
            resolver.bookRepository.getBook(bookSlug, languageCode) == null -> isValid = false
        }

        return isValid
    }

}