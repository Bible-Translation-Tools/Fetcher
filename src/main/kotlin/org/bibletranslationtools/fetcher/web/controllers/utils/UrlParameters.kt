package org.bibletranslationtools.fetcher.web.controllers.utils

data class UrlParameters(
    private val language: String? = null,
    private val product: String? = null,
    private val book: String? = null,
    private val chapter: String? = null
) {
    val languageCode: String
        get() = language ?: ""

    val productSlug: String
        get() = product ?: ""

    val bookSlug: String
        get() = book ?: ""

    val chapterParam: String
        get() = chapter ?: ""
}
