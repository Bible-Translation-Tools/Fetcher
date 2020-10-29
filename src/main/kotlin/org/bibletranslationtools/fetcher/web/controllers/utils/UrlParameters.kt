package org.bibletranslationtools.fetcher.web.controllers.utils

data class UrlParameters(
    private val _languageCode: String? = null,
    private val _productSlug: String? = null,
    private val _bookSlug: String? = null,
    private val _chapter: String? = null
) {
    val languageCode: String
        get() = _languageCode ?: ""

    val productSlug: String
        get() = _productSlug ?: ""

    val bookSlug: String
        get() = _bookSlug ?: ""

    val chapter: String
        get() = _chapter ?: ""
}
