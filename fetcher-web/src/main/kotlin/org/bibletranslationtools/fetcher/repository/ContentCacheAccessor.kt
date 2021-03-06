package org.bibletranslationtools.fetcher.repository

interface ContentCacheAccessor {
    fun update()
    fun isLanguageAvailable(code: String): Boolean
    fun isProductAvailable(productSlug: String, languageCode: String): Boolean

    fun isBookAvailable(
        bookSlug: String,
        languageCode: String,
        productSlug: String
    ): Boolean

    fun getChapterUrl(
        number: Int,
        bookSlug: String,
        languageCode: String,
        productSlug: String
    ): String?

    fun getBookUrl(
        bookSlug: String,
        languageCode: String,
        productSlug: String
    ): String?
}
