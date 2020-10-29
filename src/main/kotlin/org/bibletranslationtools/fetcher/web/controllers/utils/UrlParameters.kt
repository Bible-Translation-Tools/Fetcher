package org.bibletranslationtools.fetcher.web.controllers.utils

class UrlParameters(
    languageCode: String? = null,
    productSlug: String? = null,
    bookSlug: String? = null,
    chapter: String? = null
) {
    val languageCode: String = languageCode ?: ""
    val productSlug: String = productSlug ?: ""
    val bookSlug: String = bookSlug ?: ""
    val chapter: String = chapter ?: ""
}
