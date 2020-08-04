package org.bibletranslationtools.fetcher.web

import org.bibletranslationtools.fetcher.usecase.DependencyResolver

class RoutingValidator(val resolver: DependencyResolver) {

    fun isLanguageCodeValid(languageCode: String?): Boolean {
        var isValid = true

        if(languageCode.isNullOrEmpty()) isValid = false

        return isValid
    }

    fun isProductSlugValid(productSlug: String?): Boolean {
        var isValid = true

        if(productSlug.isNullOrEmpty()) isValid = false

        return isValid
    }

    fun isBookSlugValid(bookSlug: String?): Boolean {
        var isValid = true

        if(bookSlug.isNullOrEmpty()) isValid = false

        return isValid
    }

}