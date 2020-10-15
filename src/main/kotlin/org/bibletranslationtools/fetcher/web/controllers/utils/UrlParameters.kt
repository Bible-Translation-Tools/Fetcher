package org.bibletranslationtools.fetcher.web.controllers.utils

data class UrlParameters(
    private val lc: String? = null, // language code
    private val ps: String? = null, // product slug
    private val bs: String? = null // book slug
) {
    val languageCode = lc ?: ""
    val productSlug = ps ?: ""
    val bookSlug = bs ?: ""
}
